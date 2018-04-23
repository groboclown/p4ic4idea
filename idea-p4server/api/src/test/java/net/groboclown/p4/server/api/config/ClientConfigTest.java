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
package net.groboclown.p4.server.api.config;

import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.p4.server.api.MockDataPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ClientConfigTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void createFrom() {
        MockDataPart data = new MockDataPart()
                .withUsername("luser")
                .withServerName("mine:1234")
                .withClientname("lclient")
                ;
        ServerConfig sc = ServerConfig.createFrom(data);
        ClientConfig cc = ClientConfig.createFrom(idea.getMockProject(), sc, data, Collections.emptyList());
        fail("write");

        // all the getters are tested in here.
    }

    @Test
    void testHashCode() {
        fail("write");

    }

    @Test
    void testEquals() {
        fail("write");

    }
}