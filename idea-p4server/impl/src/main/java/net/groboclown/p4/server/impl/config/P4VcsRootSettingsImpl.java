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

import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.cache.store.VcsRootCacheStore;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class P4VcsRootSettingsImpl implements P4VcsRootSettings {
    private final VirtualFile rootDir;
    private List<ConfigPart> parts = new ArrayList<>();

    public P4VcsRootSettingsImpl(VirtualFile rootDir) {
        this.rootDir = rootDir;
    }


    @Override
    public List<ConfigPart> getConfigParts() {
        return new ArrayList<>(parts);
    }

    @Override
    public void setConfigParts(List<ConfigPart> parts) {
        this.parts = new ArrayList<>(parts);
    }

    @Override
    public void readExternal(Element element)
            throws InvalidDataException {
        Element configElement = element.getChild("p4-config");
        if (configElement == null || configElement.getChildren().isEmpty()) {
            // not set.
            setConfigParts(Collections.emptyList());
            return;
        }
        Element rootElement = configElement.getChildren().get(0);
        VcsRootCacheStore.State state = XmlSerializer.deserialize(rootElement, VcsRootCacheStore.State.class);
        VcsRootCacheStore store = new VcsRootCacheStore(state, null);
        setConfigParts(store.getConfigParts());
    }

    @Override
    public void writeExternal(Element element)
            throws WriteExternalException {
        VcsRootCacheStore store = new VcsRootCacheStore(rootDir);
        store.setConfigParts(parts);
        Element rootElement = XmlSerializer.serialize(store.getState());
        Element configElement = new Element("p4-config");
        configElement.addContent(rootElement);
        element.addContent(configElement);
    }

}
