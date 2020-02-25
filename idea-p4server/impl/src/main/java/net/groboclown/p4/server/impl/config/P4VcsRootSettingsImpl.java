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

package net.groboclown.p4.server.impl.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.part.EnvCompositePart;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * The internal state of this object is stored in the vcs.xml file.  Because of that, we cannot store
 * per-user data.  Instead, we must delegate that storage to a second class which is stored in
 * workspace.xml.
 */
public class P4VcsRootSettingsImpl implements P4VcsRootSettings {
    private static final Logger LOG = Logger.getInstance(P4VcsRootSettingsImpl.class);

    private final Project project;
    private final VirtualFile rootDir;

    public P4VcsRootSettingsImpl(@NotNull Project project, @NotNull VirtualFile rootDir) {
        this.project = project;
        this.rootDir = rootDir;
    }

    @NotNull
    @Override
    public VirtualFile getRootDir() {
        return rootDir;
    }

    @Override
    public boolean usesDefaultConfigParts() {
        final PersistentRootConfigComponent config = PersistentRootConfigComponent.getInstance(project);
        if (config == null) {
            return true;
        }
        List<ConfigPart> ret = config.getConfigPartsForRoot(rootDir);
        return ret == null || ret.isEmpty();
    }

    @Override
    @NotNull
    public List<ConfigPart> getConfigParts() {
        final PersistentRootConfigComponent config = PersistentRootConfigComponent.getInstance(project);
        if (config == null) {
            LOG.debug("No PersistentRootConfigComponent loaded; using default configuration parts.");
            return getDefaultConfigParts();
        }
        List<ConfigPart> ret = config.getConfigPartsForRoot(rootDir);
        if (ret == null || ret.isEmpty()) {
            LOG.debug("No persisted configuration parts for root directory " + rootDir);
            return getDefaultConfigParts();
        }
        return ret;
    }

    @Override
    public void setConfigParts(@NotNull List<ConfigPart> parts) {
        for (ConfigPart part : parts) {
            if (part == null) {
                LOG.warn("Added null part" + parts);
            }
        }
        final PersistentRootConfigComponent config = PersistentRootConfigComponent.getInstance(project);
        if (config == null) {
            LOG.warn("Could not save configuration parts: PersistentRootConfigComponent not initialized");
            return;
        }
        config.setConfigPartsForRoot(rootDir, parts);
    }

    @Override
    public void readExternal(Element element)
            throws InvalidDataException {
        Element configElement = element.getChild("p4-config");
        if (configElement == null || configElement.getChildren().isEmpty()) {
            // not set.
            LOG.debug("No configuration settings in the external store.");
            // do not change the existing parts list.
            //setConfigParts(Collections.emptyList());
            return;
        }
        final PersistentRootConfigComponent config = PersistentRootConfigComponent.getInstance(project);
        if (config == null) {
            LOG.warn("Loaded persistent root directory " + rootDir +
                    ", but PersistentRootConfigComponent not initialized");
        } else if (!config.hasConfigPartsForRoot(rootDir)) {
            LOG.warn("Loaded persistent root directory " + rootDir + ", but has no persistent config parts loaded");
        }
    }

    @Override
    public void writeExternal(Element element)
            throws WriteExternalException {
        element.addContent(new Element("p4-config"));
    }

    private List<ConfigPart> getDefaultConfigParts() {
        return Collections.singletonList(new EnvCompositePart(rootDir));
    }
}
