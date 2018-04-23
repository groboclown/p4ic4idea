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

import net.groboclown.p4.server.api.MockDataPart;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static net.groboclown.p4.server.api.config.ServerConfig.SEP;

class ServerConfigTest {

    @Test
    void getServerIdForDataPart_full() {
        MockDataPart data = new MockDataPart()
                .withServerName("1234")
                .withUsername("luser")
                .withPassword("my password")
                .withAuthTicketFile(new File("auth-ticket-file"))
                .withTrustTicketFile(new File("trust-ticket-file"))
                .withServerFingerprint("1234asdf1234asdf");
        assertEquals("localhost:1234" + SEP +
                "luser" + SEP +
                "<password>" + SEP +
                "auth-ticket-file" + SEP +
                "trust-ticket-file" + SEP +
                "1234asdf1234asdf", ServerConfig.getServerIdForDataPart(data));
    }

    @Test
    void getServerIdForDataPart_empty() {
        MockDataPart data = new MockDataPart()
                .withNoPassword();
        assertEquals("null" + SEP +
                "null" + SEP +
                "<>" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null", ServerConfig.getServerIdForDataPart(data));
    }

    @Test
    void getServerIdForDataPart_nullPassword() {
        MockDataPart data = new MockDataPart()
                .withPassword(null);
        assertEquals("null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null" + SEP +
                "null", ServerConfig.getServerIdForDataPart(data));
    }

    @Test
    void createFrom() {
        fail("write");
    }

    @Test
    void isValid() {
        fail("write");
    }

    @Test
    void getConfigVersion() {
        fail("write");
    }

    @Test
    void getServerName() {
        fail("write");
    }

    @Test
    void getUsername() {
        fail("write");
    }

    @Test
    void getPlaintextPassword() {
        fail("write");
    }

    @Test
    void getAuthTicket() {
        fail("write");
    }

    @Test
    void getTrustTicket() {
        fail("write");
    }

    @Test
    void getServerFingerprint() {
        fail("write");
    }

    @Test
    void getLoginSso() {
        fail("write");
    }

    @Test
    void hasServerFingerprint() {
        fail("write");
    }

    @Test
    void hasAuthTicket() {
        fail("write");
    }

    @Test
    void hasTrustTicket() {
        fail("write");
    }

    @Test
    void hasLoginSso() {
        fail("write");
    }

    @Test
    void getServerId() {
        fail("write");
    }

    @Test
    void isSameServer() {
        fail("write");
    }

    @Test
    void testToString() {
        fail("write");
    }

    @Test
    void testEquals() {
        fail("write");
    }

    @Test
    void testHashCode() {
        fail("write");
    }
}
