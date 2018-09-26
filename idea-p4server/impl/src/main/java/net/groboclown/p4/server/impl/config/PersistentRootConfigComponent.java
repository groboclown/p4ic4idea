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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.cache.store.VcsRootCacheStore;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage for configuration per VCS root.
 */
@State(
        name = "p4-PersistentRootConfigComponent",
        storages = {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )
        }
)
public class PersistentRootConfigComponent
        implements ProjectComponent, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(PersistentRootConfigComponent.class);
    public static final String COMPONENT_NAME = PersistentRootConfigComponent.class.getName();

    private final Project project;
    private final Object sync = new Object();
    private final Map<VirtualFile, List<ConfigPart>> rootPartMap = new HashMap<>();

    public PersistentRootConfigComponent(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    public List<ConfigPart> getConfigPartsForRoot(@NotNull VirtualFile root) {
        List<ConfigPart> res;
        synchronized (sync) {
            res = rootPartMap.get(root);
        }
        return res;
    }


    @Nullable
    public static PersistentRootConfigComponent getInstance(@NotNull Project project) {
        return (PersistentRootConfigComponent) project.getComponent(COMPONENT_NAME);
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        // do nothing explicit
    }

    @Override
    public void disposeComponent() {
    }

    public boolean hasConfigPartsForRoot(@NotNull VirtualFile root) {
        return getConfigPartsForRoot(root) != null;
    }

    public void setConfigPartsForRoot(@NotNull VirtualFile root, @NotNull List<ConfigPart> parts) {
        List<ConfigPart> copy = Collections.unmodifiableList(new ArrayList<>(parts));
        synchronized (sync) {
            rootPartMap.put(root, copy);
        }
    }

    @Nullable
    @Override
    public Element getState() {
        Element ret = new Element("all-root-configs");
        for (Map.Entry<VirtualFile, List<ConfigPart>> entry : rootPartMap.entrySet()) {
            VcsRootCacheStore store = new VcsRootCacheStore(entry.getKey());
            store.setConfigParts(entry.getValue());
            Element rootElement = XmlSerializer.serialize(store.getState());
            Element configElement = new Element("root");
            configElement.addContent(rootElement);
            ret.addContent(configElement);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Returning serialized state: " + new XMLOutputter().outputString(ret));
        }
        return ret;
    }

    @Override
    public void loadState(Element element) {
        if (element == null || element.getChildren().isEmpty()) {
            LOG.warn("Loaded null or empty state");
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading serialized state: " + new XMLOutputter().outputString(element));
        }
        synchronized (sync) {
            rootPartMap.clear();
            for (Element root : element.getChildren("root")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Parsing " + new XMLOutputter().outputString(root));
                }
                if (root.getChildren().isEmpty()) {
                    LOG.warn("Invalid parsing of serialized node " + new XMLOutputter().outputString(root));
                    continue;
                }
                final VcsRootCacheStore.State state = XmlSerializer.deserialize(
                        root.getChildren().get(0), VcsRootCacheStore.State.class);
                if (state.rootDirectory == null) {
                    LOG.warn("Loaded a null root directory configuration; assuming root directory " + project.getBaseDir());
                    state.rootDirectory = project.getBasePath();
                }
                final VcsRootCacheStore store = new VcsRootCacheStore(state, null);
                VirtualFile rootDir = store.getRootDirectory();
                rootPartMap.put(rootDir, store.getConfigParts());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded state from XML: " + rootPartMap);
            }
        }
    }

    @Override
    public void noStateLoaded() {
        // Really means "state loading completed."
        LOG.debug("No state loaded");
    }
}
