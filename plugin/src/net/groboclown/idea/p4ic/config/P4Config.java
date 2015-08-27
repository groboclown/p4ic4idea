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
package net.groboclown.idea.p4ic.config;

import com.perforce.p4java.server.IServerAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 *
 * @author Matt Albrecht
 */
public interface P4Config {
    enum ConnectionMethod {
        CLIENT(false),
        AUTH_TICKET(false),
        P4CONFIG(false),
        REL_P4CONFIG(true),
        SSO(false),
        UNIT_TEST_SINGLE(false),
        UNIT_TEST_MULTIPLE(true),
        DEFAULT(false);

        private final boolean isRelative;

        ConnectionMethod(final boolean isRelative) {
            this.isRelative = isRelative;
        }

        public boolean isRelativeToPath() {
            return isRelative;
        }
    }

    boolean hasIsAutoOfflineSet();

    boolean isAutoOffline();

    void reload();

    boolean hasPortSet();

    @Nullable
    String getPort();

    boolean hasProtocolSet();

    @Nullable
    IServerAddress.Protocol getProtocol();

    boolean hasClientnameSet();

    @Nullable
    String getClientname();

    boolean hasUsernameSet();

    @Nullable
    String getUsername();

    @NotNull
    ConnectionMethod getConnectionMethod();

    @Nullable
    String getPassword();

    @Nullable
    String getAuthTicketPath();

    boolean hasTrustTicketPathSet();

    @Nullable
    String getTrustTicketPath();

    boolean hasServerFingerprintSet();

    @Nullable
    String getServerFingerprint();


    @Nullable
    String getConfigFile();

    boolean isPasswordStoredLocally();

    /**
     * Allow for custom setting the client hostname.
     *
     * @return hostname of the client.
     */
    @Nullable
    String getClientHostname();


    String getIgnoreFileName();


    /*
    Look at supporting these options

    public static final String P4JAVA_TMP_DIR_KEY = "com.perforce.p4java.tmpDir";
    public static final String DEFAULT_CHARSET_KEY = "com.perforce.p4java.defaultCharset";
    public static final String IGNORE_FILE_NAME_KEY = "com.perforce.p4java.ignoreFileName";
    public static final String UNICODE_MAPPING = "com.perforce.p4java.unicodeMapping";
     */
}
