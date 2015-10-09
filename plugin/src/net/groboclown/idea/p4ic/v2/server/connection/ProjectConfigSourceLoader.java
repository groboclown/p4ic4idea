/* *************************************************************************
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 * *************************************************************************
 *                                                                         *
 * THIS MATERIAL IS PROVIDED "AS IS." ZILLIANT INC. DISCLAIMS ALL          *
 * WARRANTIES OF ANY KIND WITH REGARD TO THIS MATERIAL, INCLUDING,         *
 * BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF NONINFRINGEMENT,           *
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.                   *
 *                                                                         *
 * Zilliant Inc. shall not be liable for errors contained herein           *
 * or for incidental or consequential damages in connection with the       *
 * furnishing, performance, or use of this material.                       *
 *                                                                         *
 * Zilliant Inc. assumes no responsibility for the use or reliability      *
 * of interconnected equipment that is not furnished by Zilliant Inc,      *
 * or the use of Zilliant software with such equipment.                    *
 *                                                                         *
 * This document or software contains trade secrets of Zilliant Inc. as    *
 * well as proprietary information which is protected by copyright.        *
 * All rights are reserved.  No part of this document or software may be   *
 * photocopied, reproduced, modified or translated to another language     *
 * prior written consent of Zilliant Inc.                                  *
 *                                                                         *
 * ANY USE OF THIS SOFTWARE IS SUBJECT TO THE TERMS AND CONDITIONS         *
 * OF A SEPARATE LICENSE AGREEMENT.                                        *
 *                                                                         *
 * The information contained herein has been prepared by Zilliant Inc.     *
 * solely for use by Zilliant Inc., its employees, agents and customers.   *
 * Dissemination of the information and/or concepts contained herein to    *
 * other parties is prohibited without the prior written consent of        *
 * Zilliant Inc..                                                          *
 *                                                                         *
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 *                                                                         *
 * *************************************************************************/

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
     * @throws P4InvalidConfigException this does NOT notify the events when thrown; the
     *      caller must make that invocation.
     */
    @NotNull
    public static Collection<Builder> loadSources(@NotNull Project project, @NotNull P4Config config)
            throws P4InvalidConfigException {
        // If the manual config defines a relative p4config file,
        // then find those under the root for the project.
        // Otherwise, just return the config.

        List<Builder> sourceBuilders;
        if (config.getConnectionMethod().isRelativeToPath()) {
            String configFile = config.getConfigFile();
            if (configFile == null) {
                LOG.info("Config invalid because p4config file was not specified");
                throw new P4InvalidConfigException(P4Bundle.message("error.config.no-file"));
            }
            LOG.info(project.getName() + ": Config file: " + configFile + "; base config: " + config);

            // Relative config file.  Make sure that we use the version
            // that maps the correct root directory to the config file.
            Map<VirtualFile, P4Config> map = P4ConfigUtil.loadCorrectDirectoryP4Configs(project, configFile);
            if (map.isEmpty()) {
                LOG.info("Config invalid because no p4config files were found");
                throw new P4InvalidConfigException(P4Bundle.message("error.config.no-file"));
            }
            // The map returns one virtual file for each config directory found (and/or VCS root directories).
            // These may be duplicate configs, so need to add them to the matching entry, if any.
            sourceBuilders = new ArrayList<Builder>(map.size());
            LOG.info("config file mapping: " + map);

            for (Map.Entry<VirtualFile, P4Config> en : map.entrySet()) {
                boolean found = false;
                for (Builder sourceBuilder : sourceBuilders) {
                    if (sourceBuilder.isSame(en.getValue())) {
                        LOG.info("found existing builder path " + en.getKey());
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
                throw new P4InvalidConfigException(e);
            }

            // Note that the roots may change, which is why we register a
            // VCS root directory change listener.
            List<VirtualFile> roots = P4Vcs.getInstance(project).getVcsRoots();
            Builder builder = new Builder(project, fullConfig);
            sourceBuilders = Collections.singletonList(builder);
            for (VirtualFile root : roots) {
                builder.add(root);
            }
            LOG.info("Added source builder from config " + fullConfig + " with roots " + roots);
        }
        return sourceBuilders;
    }
}
