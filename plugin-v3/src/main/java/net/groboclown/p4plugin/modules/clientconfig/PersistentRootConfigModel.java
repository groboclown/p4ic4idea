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

package net.groboclown.p4plugin.modules.clientconfig;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.util.ProjectUtil;
import net.groboclown.p4.server.impl.cache.store.VcsRootCacheStore;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage for configuration per VCS root. WARNING this should only be accessed by the controller
 * ({@link VcsRootConfigController}).
 */
@State(
        name = "p4-PersistentRootConfigComponent",
        storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class PersistentRootConfigModel
        implements PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(PersistentRootConfigModel.class);
    public static final Class<PersistentRootConfigModel> COMPONENT_CLASS =
            PersistentRootConfigModel.class;

    private final Project project;
    private final Object sync = new Object();
    private final Map<VirtualFile, List<ConfigPart>> rootPartMap = new HashMap<>();

    public PersistentRootConfigModel(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    static PersistentRootConfigModel getInstance(@NotNull Project project) {
        return project.getComponent(COMPONENT_CLASS);
    }

    void setConfigPartsForRoot(@NotNull VirtualFile root, @NotNull List<ConfigPart> parts) {
        List<ConfigPart> copy = List.copyOf(parts);
        synchronized (sync) {
            rootPartMap.put(root, copy);
            if (LOG.isDebugEnabled()) {
                // NOTE: not synchronized access for debug read.
                LOG.debug("Stored root " + root + "; current roots stored = " + rootPartMap.keySet());
            }
        }
    }

    @Nullable
    List<ConfigPart> getConfigPartsForRoot(@NotNull VirtualFile root) {
        List<ConfigPart> res;
        synchronized (sync) {
            res = rootPartMap.get(root);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieved config parts for root " + root + " = " + res +
                        "; current roots stored = " + rootPartMap.keySet());
            }
        }
        return res;
    }

    @NotNull
    List<VirtualFile> getRegisteredRoots() {
        List<VirtualFile> res;
        synchronized (sync) {
            res = List.copyOf(rootPartMap.keySet());
        }
        return res;
    }

    @Nullable
    @Override
    public Element getState() {
        Element ret = new Element("all-root-configs");
        for (Map.Entry<VirtualFile, List<ConfigPart>> entry : rootPartMap.entrySet()) {
            if (!isValidRoot(entry.getKey())) {
                LOG.info("Skipped persisting VCS root " + entry.getKey() +
                        " because it does not appear to be a valid Perforce root");
                continue;
            }
            VcsRootCacheStore store = new VcsRootCacheStore(entry.getKey());
            store.setConfigParts(entry.getValue());
            Element rootElement = XmlSerializer.serialize(store.getState());
            Element configElement = new Element("root");
            configElement.addContent(rootElement);
            ret.addContent(configElement);
        }
        return ret;
    }

    @Override
    public void loadState(@NotNull Element element) {
        if (element == null || element.getChildren().isEmpty()) {
            LOG.warn("Loaded null or empty state");
            return;
        }
        synchronized (sync) {
            rootPartMap.clear();
            for (Element root : element.getChildren("root")) {
                if (root.getChildren().isEmpty()) {
                    LOG.warn("Invalid parsing of serialized node " + new XMLOutputter().outputString(root));
                    continue;
                }
                final VcsRootCacheStore.State state = XmlSerializer.deserialize(
                        root.getChildren().get(0), VcsRootCacheStore.State.class);
                if (state.rootDirectory == null) {
                    LOG.warn("Loaded a null root directory configuration; assuming root directory " +
                            ProjectUtil.findProjectBaseDir(project));
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

    private boolean isValidRoot(final VirtualFile file) {
        if (project.isDisposed()) {
            // Err on the side of caution
            return true;
        }
        ProjectLevelVcsManager mgr = ProjectLevelVcsManager.getInstance(project);
        if (mgr == null) {
            // Err on the side of caution
            return true;
        }
        for (VcsRoot root : mgr.getAllVcsRoots()) {
            if (root == null || root.getVcs() == null || root.getPath() == null) {
                continue;
            }
            if (!P4VcsKey.VCS_NAME.equals(root.getVcs().getKeyInstanceMethod().getName())) {
                continue;
            }
            if (file.equals(root.getPath())) {
                return true;
            }
        }
        // This can happen if the passed-in file is not yet registered in
        // the VCS root path.
        return false;
    }
}
