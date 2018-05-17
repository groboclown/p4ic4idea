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
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class P4VcsRootSettingsImpl implements P4VcsRootSettings {
    private List<ConfigPart> parts = new ArrayList<>();


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
        // FIXME implement.  Use XmlSerializer
    }

    @Override
    public void writeExternal(Element element)
            throws WriteExternalException {
        // FIXME implement.  Use XmlSerializer
    }

}
