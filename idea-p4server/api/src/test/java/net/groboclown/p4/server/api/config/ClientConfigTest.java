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

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.part.MockConfigPart;
import org.junit.jupiter.api.Test;

import static net.groboclown.idea.ExtMatchers.greaterThan;
import static net.groboclown.p4.server.api.config.ClientConfig.SEP;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createError;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientConfigTest {
    @Test
    void createFrom_invalid_invalidServerConfigSettings() {
        MockConfigPart data = new MockConfigPart()
                .withUsername("u")
                .withServerName("blah")
                .withClientname("c");
        ServerConfig sc = ServerConfig.createFrom(data);

        data.withUsername(null).withServerName(null);
        // Should not throw an error - it only needs client stuff.
        ClientConfig.createFrom(sc, data);
    }

    @Test
    void createFrom_invalid_noClientname() {
        MockConfigPart data = new MockConfigPart()
                .withUsername("u")
                .withServerName("blah");
        assertThrows(IllegalArgumentException.class, () -> ClientConfig.createFrom(
                ServerConfig.createFrom(data), data
        ));
    }

    @Test
    void createFrom_invalid_errors() {
        MockConfigPart data = new MockConfigPart()
                .withServerName("blah")
                .withUsername("u")
                .withClientname("c");
        ServerConfig sc = ServerConfig.createFrom(data);
        data.withConfigProblems(createError());
        assertThrows(IllegalArgumentException.class, () -> ClientConfig.createFrom(
                sc, data
        ));
    }

    @Test
    void createFrom_valid_partial() {
        MockConfigPart data = new MockConfigPart()
                .withUsername("luser")
                .withServerName("mine:1234")
                .withClientname("lclient")
                ;
        ServerConfig sc = ServerConfig.createFrom(data);
        ClientConfig cc = ClientConfig.createFrom(sc, data);

        assertSame(sc, cc.getServerConfig());
        assertEquals(new ClientServerRef(sc.getServerName(), "lclient"),
                cc.getClientServerRef());
        assertThat(cc.getConfigVersion(), greaterThan(0));
        assertEquals(
                sc.getServerId() + SEP +
                "lclient" + SEP +
                null + SEP +
                null + SEP +
                null + SEP,
                cc.getClientServerUniqueId());
        assertEquals("lclient", cc.getClientname());
        assertNull(cc.getClientHostName());
        assertNull(cc.getIgnoreFileName());
        assertNull(cc.getDefaultCharSet());

        // check that changing the original data does not affect the config
        data.withClientname("not valid");
        assertEquals("lclient", cc.getClientname());
    }
}