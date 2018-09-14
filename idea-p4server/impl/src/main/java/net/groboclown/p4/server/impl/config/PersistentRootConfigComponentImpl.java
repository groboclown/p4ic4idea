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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.p4.server.api.config.PersistentRootConfigComponent;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.cache.store.VcsRootCacheStore;
import org.jdom.Element;
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
public class PersistentRootConfigComponentImpl extends PersistentRootConfigComponent {
    private final Object sync = new Object();
    private final Map<VirtualFile, List<ConfigPart>> rootPartMap = new HashMap<>();

    @Override
    public List<ConfigPart> getConfigPartsForRoot(@NotNull VirtualFile root) {
        List<ConfigPart> res;
        synchronized (sync) {
            res = rootPartMap.get(root);
        }
        return res;
    }

    @Override
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
        return ret;
    }

    @Override
    public void loadState(Element element) {
        if (element == null || element.getChildren().isEmpty()) {
            return;
        }
        synchronized (sync) {
            rootPartMap.clear();
            for (Element root : element.getChildren("root")) {
                VcsRootCacheStore.State state = XmlSerializer.deserialize(root, VcsRootCacheStore.State.class);
                VcsRootCacheStore store = new VcsRootCacheStore(state, null);
                rootPartMap.put(store.getRootDirectory(), store.getConfigParts());
            }
        }
    }

    @Override
    public void noStateLoaded() {
        // keep part map as-is
    }
}
