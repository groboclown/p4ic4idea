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

import com.intellij.openapi.util.Pair;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.P4ServerName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static net.groboclown.idea.ExtMatchers.mapContainsAll;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createError;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ConfigPropertiesUtilTest {

    @SuppressWarnings("unchecked")
    @Test
    void toProperties_configPartEmpty() {
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                new MockConfigPart(),
                "<unset>", "<ep>", "<set>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", "<unset>"),
                new Pair<>("P4TRUST", "<unset>"),
                new Pair<>("P4USER", "<unset>"),
                new Pair<>("P4TICKETS", "<unset>"),
                new Pair<>("P4FINGERPRINT", "<unset>"),
                new Pair<>("P4PASSWD", "<unset>"),
                new Pair<>("P4CHARSET", "<unset>"),
                new Pair<>("P4IGNORE", "<unset>"),
                new Pair<>("P4CLIENT", "<unset>"),
                new Pair<>("P4HOST", "<unset>"),
                new Pair<>("P4LOGINSSO", "<unset>")
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    void toProperties_configPartEmptyPassword() {
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                new MockConfigPart()
                    .withPassword(""),
                "<unset>", "<ep>", "<set>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", "<unset>"),
                new Pair<>("P4TRUST", "<unset>"),
                new Pair<>("P4USER", "<unset>"),
                new Pair<>("P4TICKETS", "<unset>"),
                new Pair<>("P4FINGERPRINT", "<unset>"),
                new Pair<>("P4PASSWD", "<ep>"),
                new Pair<>("P4CHARSET", "<unset>"),
                new Pair<>("P4IGNORE", "<unset>"),
                new Pair<>("P4CLIENT", "<unset>"),
                new Pair<>("P4HOST", "<unset>"),
                new Pair<>("P4LOGINSSO", "<unset>")
        ));
    }

    @Test
    void toProperties_configPartSet() {
        File authFile = new File("auth.txt");
        File trustFile = new File("trust.txt");
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                new MockConfigPart()
                    .withUsername("username")
                    .withServerName("servername")
                    .withClientname("clientname")
                    .withConfigProblems(createError())
                    .withPassword("password")
                    .withServerFingerprint("fingerprint")
                    .withAuthTicketFile(authFile)
                    .withTrustTicketFile(trustFile)
                    .withClientHostname("hostname")
                    .withDefaultCharset("charset")
                    .withIgnoreFileName("ignore")
                    .withLoginSso("sso")
                    .withRequiresUserEnteredPassword(true)
                    .withSourceName("s"),
                "<unset>", "<ep>", "<set>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", P4ServerName.forPort("servername").getDisplayName()),
                new Pair<>("P4TRUST", "trust.txt"),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4TICKETS", "auth.txt"),
                new Pair<>("P4FINGERPRINT", "fingerprint"),
                new Pair<>("P4PASSWD", "<set>"),
                new Pair<>("P4CHARSET", "charset"),
                new Pair<>("P4IGNORE", "ignore"),
                new Pair<>("P4CLIENT", "clientname"),
                new Pair<>("P4HOST", "hostname"),
                new Pair<>("P4LOGINSSO", "sso")
        ));
    }

    @Test
    void toProperties_ServerConfig() {
        fail("write");
    }

    @Test
    void toProperties_ClientConfig() {
        fail("write");
    }
}