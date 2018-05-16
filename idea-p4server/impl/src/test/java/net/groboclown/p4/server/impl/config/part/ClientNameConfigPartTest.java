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
package net.groboclown.p4.server.impl.config.part;

import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ClientNameConfigPartTest {
    @Test
    void serialize() {
        ClientNameConfigPart part = new ClientNameConfigPart("source");
        part.setClientname("client-name-1");

        Element serial = XmlSerializer.serialize(part);
        assertNotNull(serial);

        ConfigPart res = XmlSerializer.deserialize(serial, ConfigPart.class);
        assertNotNull(res);
        assertThat(res, instanceOf(ClientNameConfigPart.class));
    }
}
