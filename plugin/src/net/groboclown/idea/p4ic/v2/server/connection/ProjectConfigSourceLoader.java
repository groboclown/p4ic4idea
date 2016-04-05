/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigUtil;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource.Builder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class ProjectConfigSourceLoader {
    private static final Logger LOG = Logger.getInstance(ProjectConfigSourceLoader.class);


    /**
     * Load the builders for the sources based on this config.
     *
     * @param config base config to load
     * @return the builders related to the config, or null if no
     */
    @NotNull
    public static Collection<Builder> loadSources(@NotNull Project project, @NotNull P4Config config) {
        // If the manual config defines a relative p4config file,
        // then find those under the root for the project.
        // Otherwise, just return the config.

        List<Builder> sourceBuilders;
        if (config.getConnectionMethod().isRelativeToPath()) {
            String configFile = config.getConfigFile();
            if (configFile == null) {
                LOG.info("Config invalid because p4config file was not specified");
                final Builder builder = new Builder(project, config);
                builder.setError(new P4InvalidConfigException(P4Bundle.message("error.config.no-file")));
                return Collections.singleton(builder);
            }
            if (LOG.isDebugEnabled()) {
                LOG.info(project.getName() + ": Config file: " + configFile + "; base config: " + config);

                LOG.debug("Loading p4config files named " + configFile + " under " +
                        P4Vcs.getInstance(project).getVcsRoots());
            }

            // Relative config file.  Make sure that we use the version
            // that maps the correct root directory to the config file.
            Map<VirtualFile, P4Config> map = P4ConfigUtil.loadCorrectDirectoryP4Configs(project, configFile);
            if (map.isEmpty()) {
                LOG.info("Config invalid because no p4config files were found.  Searching for config files named " +
                    configFile + ", under vcs roots " + P4Vcs.getInstance(project).getVcsRoots());
                final Builder builder = new Builder(project, config);
                builder.setError(new P4InvalidConfigException(P4Bundle.message("error.config.no-file")));
                return Collections.singleton(builder);
            }
            // The map returns one virtual file for each config directory found (and/or VCS root directories).
            // These may be duplicate configs, so need to add them to the matching entry, if any.
            sourceBuilders = new ArrayList<Builder>(map.size());
            LOG.info("config file mapping: " + map);

            for (Map.Entry<VirtualFile, P4Config> en : map.entrySet()) {
                boolean found = false;
                for (Builder sourceBuilder : sourceBuilders) {
                    if (sourceBuilder.isSame(en.getValue())) {
                        if (LOG.isDebugEnabled()) {
                            LOG.info("found existing builder path " + en.getKey());
                        }
                        sourceBuilder.add(en.getKey());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOG.info("found new builder path " + en.getKey() + " - " + en.getValue());
                    final Builder builder = new Builder(project, en.getValue());
                    builder.add(en.getKey());
                    sourceBuilders.add(builder);
                }
            }
        } else {
            P4Config fullConfig;
            try {
                fullConfig = P4ConfigUtil.loadCmdP4Config(config);
            } catch (IOException e) {
                LOG.info("Config invalid because of an IO exception", e);
                final Builder builder = new Builder(project, config);
                builder.setError(new P4InvalidConfigException(e));
                return Collections.singleton(builder);
            }

            // Note that the roots may change, which is why we register a
            // VCS root directory change listener.
            List<VirtualFile> roots = P4Vcs.getInstance(project).getVcsRoots();
            Builder builder = new Builder(project, fullConfig);
            sourceBuilders = Collections.singletonList(builder);
            for (VirtualFile root : roots) {
                builder.add(root);
            }
            if (LOG.isDebugEnabled()){
                LOG.info("Added source builder from config " + fullConfig + " with roots " + roots);
            }
        }
        return sourceBuilders;
    }
}
