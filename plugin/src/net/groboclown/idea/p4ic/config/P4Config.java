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
import org.jetbrains.annotations.NonNls;
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
    // TODO this should be a client-side user setting
    @NonNls
    public static final String P4_IGNORE_FILE = ".p4ignore";

    enum ConnectionMethod {
        CLIENT,
        AUTH_TICKET,
        P4CONFIG,
        REL_P4CONFIG,
        SSO,
        DEFAULT
    }

    public boolean hasIsAutoOfflineSet();

    public boolean isAutoOffline();

    public void reload();

    public boolean hasPortSet();

    @Nullable
    public String getPort();

    public boolean hasProtocolSet();

    @Nullable
    public IServerAddress.Protocol getProtocol();

    public boolean hasClientnameSet();

    @Nullable
    public String getClientname();

    public boolean hasUsernameSet();

    @Nullable
    public String getUsername();

    @NotNull
    public ConnectionMethod getConnectionMethod();

    @Nullable
    public String getPassword();

    @Nullable
    public String getAuthTicketPath();

    public boolean hasTrustTicketPathSet();

    @Nullable
    public String getTrustTicketPath();

    @Nullable
    public String getConfigFile();

    public boolean isPasswordStoredLocally();

    /*
    Look at supporting these options

    public static final String P4JAVA_TMP_DIR_KEY = "com.perforce.p4java.tmpDir";
    public static final String DEFAULT_CHARSET_KEY = "com.perforce.p4java.defaultCharset";
    public static final String IGNORE_FILE_NAME_KEY = "com.perforce.p4java.ignoreFileName";
    public static final String UNICODE_MAPPING = "com.perforce.p4java.unicodeMapping";
     */
}
