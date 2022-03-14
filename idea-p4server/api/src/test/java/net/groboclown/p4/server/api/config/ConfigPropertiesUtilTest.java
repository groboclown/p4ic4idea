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

import com.intellij.credentialStore.OneTimeString;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.config.part.MockConfigPart;
import net.groboclown.p4.server.api.P4ServerName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import static net.groboclown.idea.ExtMatchers.mapContainsAll;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createError;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createWarning;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unchecked")
class ConfigPropertiesUtilTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @BeforeEach
    void before() {
        // idea.registerApplicationService(PasswordSafe.class, new MockPasswordSafe());
        ApplicationPasswordRegistry passwdRegistry = new ApplicationPasswordRegistry() {
            @NotNull
            @Override
            public Promise<OneTimeString> getOrAskFor(@Nullable Project project, @NotNull ServerConfig config) {
                return get(config);
            }

            @Override
            public void askForNewPassword(@Nullable Project project, @NotNull ServerConfig config) {
                // do nothing
            }
        };
        //idea.registerApplicationComponent(ApplicationPasswordRegistry.COMPONENT_NAME, passwdRegistry);
        idea.registerApplicationComponent(ApplicationPasswordRegistry.COMPONENT_CLASS, passwdRegistry);
    }


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
                new Pair<>("P4PORT", portName("servername")),
                new Pair<>("P4TRUST", "trust.txt"),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4TICKETS", "auth.txt"),
                new Pair<>("P4FINGERPRINT", "fingerprint"),
                // with Requires User Entered Password, it doesn't matter if the password is explicitly set.
                new Pair<>("P4PASSWD", "<unset>"),
                new Pair<>("P4CHARSET", "charset"),
                new Pair<>("P4IGNORE", "ignore"),
                new Pair<>("P4CLIENT", "clientname"),
                new Pair<>("P4HOST", "hostname"),
                new Pair<>("P4LOGINSSO", "sso")
        ));
    }

    @Test
    void toProperties_ServerConfig_filled() {
        File authFile = new File("auth.txt");
        File trustFile = new File("trust.txt");
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                ServerConfig.createFrom(
                    new MockConfigPart()
                            .withUsername("username")
                            .withServerName("servername")
                            .withClientname("clientname")
                            .withConfigProblems(createWarning())
                            .withPassword("password")
                            .withServerFingerprint("fingerprint")
                            .withAuthTicketFile(authFile)
                            .withTrustTicketFile(trustFile)
                            .withClientHostname("hostname")
                            .withDefaultCharset("charset")
                            .withIgnoreFileName("ignore")
                            .withLoginSso("sso")
                            .withRequiresUserEnteredPassword(true)
                            .withSourceName("s")
                ),
                "<unset>", "<req>", "<nr>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", portName("servername")),
                new Pair<>("P4TRUST", "trust.txt"),
                new Pair<>("P4TICKETS", "auth.txt"),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4PASSWD", "<req>"),
                new Pair<>("P4FINGERPRINT", "fingerprint"),
                new Pair<>("P4LOGINSSO", "sso")
        ));
    }

    @Test
    void toProperties_ServerConfig_empty() {
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                ServerConfig.createFrom(
                        new MockConfigPart()
                                .withUsername("username")
                                .withServerName("servername")
                ),
                "<unset>", "<stored>", "<ns>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", portName("servername")),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4PASSWD", "<ns>"),
                new Pair<>("P4TRUST", "<unset>"),
                new Pair<>("P4TICKETS", "<unset>"),
                new Pair<>("P4FINGERPRINT", "<unset>"),
                new Pair<>("P4LOGINSSO", "<unset>")
        ));
    }

    @Test
    void toProperties_ServerConfig_emptyPassword() {
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                ServerConfig.createFrom(
                    new MockConfigPart()
                            .withUsername("username")
                            .withServerName("servername")
                            .withPassword("")
                ),
                "<unset>", "<stored>", "<ns>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", portName("servername")),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4TRUST", "<unset>"),
                new Pair<>("P4TICKETS", "<unset>"),
                new Pair<>("P4FINGERPRINT", "<unset>"),
                new Pair<>("P4LOGINSSO", "<unset>"),

                // The user supplied an empty password.  This means that a password is stored.
                new Pair<>("P4PASSWD", "<stored>")
        ));
    }

    @Test
    void toProperties_ClientConfig_empty() {
        MockConfigPart part = new MockConfigPart()
                .withUsername("username")
                .withServerName("servername")
                .withClientname("client");
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                ClientConfig.createFrom(ServerConfig.createFrom(part), part),
                "<unset>", "<stored>", "<ns>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", portName("servername")),
                new Pair<>("P4CLIENT", "client"),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4PASSWD", "<ns>"),
                new Pair<>("P4TRUST", "<unset>"),
                new Pair<>("P4TICKETS", "<unset>"),
                new Pair<>("P4FINGERPRINT", "<unset>"),
                new Pair<>("P4CHARSET", "<unset>"),
                new Pair<>("P4IGNORE", "<unset>"),
                new Pair<>("P4HOST", "<unset>"),
                new Pair<>("P4LOGINSSO", "<unset>")
        ));
    }

    @Test
    void toProperties_ClientConfig_filled() {
        File authFile = new File("auth.txt");
        File trustFile = new File("trust.txt");
        MockConfigPart part = new MockConfigPart()
                .withUsername("username")
                .withPassword("pass")
                .withServerName("servername")
                .withClientname("client")
                .withTrustTicketFile(trustFile)
                .withAuthTicketFile(authFile)
                .withServerFingerprint("abcd")
                .withDefaultCharset("char")
                .withIgnoreFileName("ignore-these")
                .withClientHostname("c-host")
                .withLoginSso("log-sso.cmd -t my_auth");
        Map<String, String> props = ConfigPropertiesUtil.toProperties(
                ClientConfig.createFrom(ServerConfig.createFrom(part), part),
                "<unset>", "<stored>", "<ns>"
        );
        assertThat(props, mapContainsAll(
                new Pair<>("P4PORT", portName("servername")),
                new Pair<>("P4CLIENT", "client"),
                new Pair<>("P4USER", "username"),
                new Pair<>("P4PASSWD", "<stored>"),
                new Pair<>("P4TRUST", "trust.txt"),
                new Pair<>("P4TICKETS", "auth.txt"),
                new Pair<>("P4FINGERPRINT", "abcd"),
                new Pair<>("P4CHARSET", "char"),
                new Pair<>("P4IGNORE", "ignore-these"),
                new Pair<>("P4HOST", "c-host"),
                new Pair<>("P4LOGINSSO", "log-sso.cmd -t my_auth")
        ));
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private static String portName(@NotNull String serverName) {
        return Objects.requireNonNull(P4ServerName.forPort(serverName)).getDisplayName();
    }
}
