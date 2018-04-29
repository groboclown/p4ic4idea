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

import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.P4ServerName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static net.groboclown.idea.ExtMatchers.greaterThan;
import static net.groboclown.p4.server.api.config.ServerConfig.SEP;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createError;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createWarning;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerConfigTest {

    @Test
    void getServerIdForDataPart_full() {
        MockConfigPart data = new MockConfigPart()
                .withServerName("1234")
                .withUsername("luser")
                .withPassword("my password")
                .withAuthTicketFile(new File("auth-ticket-file"))
                .withTrustTicketFile(new File("trust-ticket-file"))
                .withServerFingerprint("1234asdf1234asdf");
        assertEquals("localhost:1234" + SEP +
                "luser" + SEP +
                "auth-ticket-file" + SEP +
                "trust-ticket-file" + SEP +
                "1234asdf1234asdf", ServerConfig.getServerIdForDataPart(data));
    }

    @Test
    void getServerIdForDataPart_empty() {
        MockConfigPart data = new MockConfigPart()
                .withNoPassword();
        assertEquals("null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null", ServerConfig.getServerIdForDataPart(data));
    }

    @Test
    void getServerIdForDataPart_nullPassword() {
        MockConfigPart data = new MockConfigPart()
                .withPassword(null);
        assertEquals("null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null", ServerConfig.getServerIdForDataPart(data));
    }

    @Test
    void isValid() {
        assertFalse(ServerConfig.isValidServerConfig(null));

        MockConfigPart part = new MockConfigPart();
        part.withConfigProblems(createError());
        assertFalse(ServerConfig.isValidServerConfig(part));

        part.withConfigProblems();
        assertFalse(ServerConfig.isValidServerConfig(part));

        P4ServerName mockName = mock(P4ServerName.class);
        when(mockName.getFullPort()).thenReturn("");
        part.withP4ServerName(mockName);
        assertFalse(ServerConfig.isValidServerConfig(part));

        part.withConfigProblems(createWarning())
            .withServerName("my-server");
        assertFalse(ServerConfig.isValidServerConfig(part));

        part.withUsername("");
        assertFalse(ServerConfig.isValidServerConfig(part));

        part.withUsername("abc");
        assertTrue(ServerConfig.isValidServerConfig(part));
    }

    @Test
    void createFrom_error() {
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.createFrom(new MockConfigPart()));
    }

    @Test
    void createFrom_empty() {
        ServerConfig sc = ServerConfig.createFrom(
                new MockConfigPart()
                        .withUsername("username")
                        .withServerName("servername")
        );

        assertEquals(P4ServerName.forPort("servername"), sc.getServerName());
        assertEquals("username", sc.getUsername());
        assertNull(sc.getServerFingerprint());
        assertNull(sc.getAuthTicket());
        assertNull(sc.getTrustTicket());
        assertNull(sc.getLoginSso());
        assertThat(sc.getConfigVersion(), greaterThan(0));
        assertFalse(sc.hasServerFingerprint());
        assertFalse(sc.hasAuthTicket());
        assertFalse(sc.hasTrustTicket());
        assertFalse(sc.hasLoginSso());
        assertFalse(sc.usesStoredPassword());
        assertEquals("localhost:servername" + SEP +
                "username" + SEP + "null" + SEP + "null" + SEP + "null",
                sc.getServerId());
    }

    @Test
    void isSameServer_empty() {
        MockConfigPart part1 = new MockConfigPart()
                .withUsername("u1")
                .withServerName("n1:1");
        ServerConfig sc = ServerConfig.createFrom(part1);
        assertTrue(sc.isSameServerConnection(part1));

        // Change it in the client ways, and it's still the same server.
        part1.withClientHostname("h1").withClientname("cc")
                .withDefaultCharset("a1").withIgnoreFileName("i")
                .withReloadValue(false);
        assertTrue(sc.isSameServerConnection(part1));

        // Change the user
        MockConfigPart part2 = part1.copy()
                .withUsername("u2");
        assertFalse(sc.isSameServerConnection(part2));

        // change the server name
        MockConfigPart part3 = part1.copy()
                .withServerName("n2:1");
        assertFalse(sc.isSameServerConnection(part3));

        // change the password
        MockConfigPart part4 = part1.copy()
                .withPassword("a");
        assertFalse(sc.isSameServerConnection(part4));
        part4.withNoPassword().withRequiresUserEnteredPassword(true);
        assertFalse(sc.isSameServerConnection(part4));
    }

    @Test
    void testToString() {
        //fail("write");
    }

    @Test
    void testEquals() {
        //fail("write");
    }

    @Test
    void testHashCode() {
        //fail("write");
    }
}
