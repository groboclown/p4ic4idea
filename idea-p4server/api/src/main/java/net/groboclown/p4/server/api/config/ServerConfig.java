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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.groboclown.p4.server.api.util.EqualUtil.isEqual;
import static org.apache.commons.lang3.StringUtils.isBlank;


/**
 * Stores the connection information related to a specific Perforce
 * server.  It only keeps track of information required to identify the
 * server, which means it needs the username, password, auth ticket file,
 * trust ticket file, and server fingerprint.
 * <p>
 * Implementations must specify {@link #equals(Object)} and
 * {@link #hashCode()}, to indicate whether the server connection
 * properties are the same; it should not match on online mode.
 * <p>
 * If the configuration specified a password, then it is stored externally
 * in the {@link ApplicationPasswordRegistry}.  This also allows for keeping
 * in one place the password in case the user enters it manually, which would
 * break immutability.
 * <p>
 * This is used for running server commands that do not require a client,
 * but do require a username.  There are limited commands that no not require a
 * username, and for those, the {@link P4ServerName} is sufficient for connectivity.
 * <p>
 * This class MUST be immutable.
 */
@Immutable
public final class ServerConfig {
    private static final Logger LOG = Logger.getInstance(ServerConfig.class);

    // Just some character that won't appear in any real text, but
    // is still viewable in a debugger.
    static final char SEP = (char) 0x263b;

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private final int configVersion;
    private final P4ServerName serverName;
    private final String username;
    private final File authTicket;
    private final File trustTicket;
    private final String serverFingerprint;
    private final String loginSso;

    private final String serverId;

    private final boolean usesPassword;

    @NotNull
    static String getServerIdForDataPart(@NotNull ConfigPart part) {
        StringBuilder sb = new StringBuilder();
        if (part.hasServerNameSet() && part.getServerName() != null) {
            sb.append(part.getServerName().getFullPort());
        } else {
            sb.append((String) null);
        }
        // Note: does not include password information.
        sb.append(SEP)
            .append(part.hasUsernameSet() ? part.getUsername() : null)
            .append(SEP)
            .append(part.hasAuthTicketFileSet() ? part.getAuthTicketFile() : null)
            .append(SEP)
            .append(part.hasTrustTicketFileSet() ? part.getTrustTicketFile() : null)
            .append(SEP)
            .append(part.hasServerFingerprintSet() ? part.getServerFingerprint() : null);
        // These may be common enough that we want to save memory by interning the strings.
        return sb.toString().intern();
    }



    @NotNull
    public static ServerConfig createFrom(@NotNull ConfigPart part) {
        return new ServerConfig(part);
    }

    public static boolean isValidServerConfig(@Nullable ConfigPart part) {
        if (part == null || part.hasError()) {
            return false;
        }

        // This should be included with the above config problems.
        if (part.getServerName() == null || isBlank(part.getServerName().getFullPort())) {
            return false;
        }

        if (isBlank(part.getUsername())) {
            return false;
        }

        return true;
    }

    private ServerConfig(@NotNull ConfigPart part) {
        if (! isValidServerConfig(part)) {
            throw new IllegalArgumentException("Did not check validity before creating");
        }
        this.configVersion = COUNT.incrementAndGet();

        assert part.hasServerNameSet();
        this.serverName = part.getServerName();
        assert part.hasUsernameSet();
        this.username = part.getUsername();
        this.authTicket =
                part.hasAuthTicketFileSet()
                        ? part.getAuthTicketFile()
                        : null;
        this.trustTicket =
                part.hasTrustTicketFileSet()
                        ? part.getTrustTicketFile()
                        : null;
        this.serverFingerprint =
                part.hasServerFingerprintSet()
                        ? part.getServerFingerprint()
                        : null;
        this.loginSso =
                part.hasLoginSsoSet()
                        ? part.getLoginSso()
                        : null;
        this.usesPassword = part.requiresUserEnteredPassword() || part.hasPasswordSet();

        this.serverId = getServerIdForDataPart(part);

        // Must be done at the very end.
        // This logic is carefully constructed to only store the password if it's supplied by the
        // user, and the user does not require that it is manually entered into the UI.
        // This class never stores the password in itself.
        if (!part.requiresUserEnteredPassword() && part.hasPasswordSet() && part.getPlaintextPassword() != null) {
            LOG.info("Storing user password in password store");
            // DEBUG
            // ApplicationPasswordRegistry.getInstance().store(this, part.getPlaintextPassword().toCharArray(), false);
            ApplicationPasswordRegistry instance = ApplicationPasswordRegistry.getInstance();
            String passwd = part.getPlaintextPassword();
            char[] passwdChars = passwd.toCharArray();
            instance.store(this, passwdChars, false);
        }
    }


    public int getConfigVersion() {
        return this.configVersion;
    }

    /**
     *
     * @return true if the user supplies a password, either through plaintext, or through the UI asking for it.
     */
    public boolean usesStoredPassword() {
        return usesPassword;
    }

    @NotNull
    public P4ServerName getServerName() {
        return serverName;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @Nullable
    public File getAuthTicket() {
        return authTicket;
    }

    @Nullable
    public File getTrustTicket() {
        return trustTicket;
    }

    @Nullable
    public String getServerFingerprint() {
        return serverFingerprint;
    }

    @Nullable
    public String getLoginSso() {
        return loginSso;
    }

    public boolean hasServerFingerprint() {
        return getServerFingerprint() != null && getServerFingerprint().length() > 0;
    }

    public boolean hasAuthTicket() {
        return getAuthTicket() != null;
    }

    public boolean hasTrustTicket() {
        return getTrustTicket() != null;
    }

    public boolean hasLoginSso() {
        return getLoginSso() != null;
    }

    /**
     *
     * @return unique identifier for the server connection settings.
     */
    @NotNull
    public String getServerId() {
        return serverId;
    }

    /**
     * Checks if the {@literal part} connects to the same server
     * in the same way as this server configuration.  For identifying
     * if the server itself is the same, then compare the
     * {@link P4ServerName} values.
     *
     * @param part configuration to compare
     * @return true if the {@literal part} and this config connect to
     *      the same server the same way.
     */
    boolean isSameServerConnection(@Nullable ConfigPart part) {
        if (part == null) {
            LOG.debug("isSameServerConnection: input is null");
            return false;
        }

        if (! isEqual(getServerName(), part.getServerName())) {
            if (LOG.isDebugEnabled()) {
                if (part.getServerName() == null) {
                    LOG.debug("isSameServerConnection: input server name is null");
                } else {
                    LOG.debug("isSameServerConnection: server doesn't match: "
                            + getServerName().getServerPort() + "::" + getServerName().getServerProtocol() + " <> "
                            + part.getServerName().getServerPort() + "::" + part.getServerName().getServerProtocol());
                }
            }
            return false;
        }

        if (! filesEqual(hasAuthTicket(), getAuthTicket(), part.hasAuthTicketFileSet(), part.getAuthTicketFile())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServerConnection: auth ticket doesn't match: "
                        + getAuthTicket() + " <> " + part.getAuthTicketFile());
            }
            return false;
        }

        if (! filesEqual(hasTrustTicket(), getTrustTicket(), part.hasTrustTicketFileSet(), part.getTrustTicketFile())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServerConnection: trust ticket doesn't match: "
                        + getTrustTicket() + " <> " + part.getTrustTicketFile());
            }
            return false;
        }

        if (hasServerFingerprint() != part.hasServerFingerprintSet()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServerConnection: has server fingerprint mismatch: "
                        + hasServerFingerprint() + " <> " + part.hasServerFingerprintSet());
            }
            return false;
        }
        if (hasServerFingerprint() && ! isEqual(getServerFingerprint(), part.getServerFingerprint())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServerConnection: server fingerprint mismatch: "
                        + getServerFingerprint() + " <> " + part.getServerFingerprint());
            }
            return false;
        }

        if (! isEqual(getUsername(), part.getUsername())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServerConnection: username mismatch: "
                        + getUsername() + " <> " + part.getUsername());
            }
            return false;
        }

        // password usage is a bit more complex.
        if (
                (usesStoredPassword() && (!part.requiresUserEnteredPassword() && !part.hasPasswordSet()))
                || (!usesStoredPassword() && (part.requiresUserEnteredPassword() || part.hasPasswordSet()))
            ) {
            return false;
        }

        return true;
    }

    @NotNull
    private Map<String, String> toProperties() {
        return ConfigPropertiesUtil.toProperties(this, "(unset)", "(empty)", "(set)");
    }

    @TestOnly
    @Override
    public String toString() {
        return toProperties().toString();
    }


    // equals only cares about the information that connects
    // to the server, not the individual server setup.  Note that
    // this might have the potential to lose information.
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServerConfig)) {
            return false;
        }
        ServerConfig sc = (ServerConfig) other;
        return sc.getServerId().equals(getServerId());
    }

    @Override
    public int hashCode() {
        return getServerId().hashCode();
    }


    private static boolean filesEqual(boolean aSet, @Nullable File aFile, boolean bSet, @Nullable File bFile) {
        return FileUtil.filesEqual(scrubFile(aSet, aFile), scrubFile(bSet, bFile));
    }

    @Nullable
    private static File scrubFile(boolean isSet, @Nullable File file) {
        if (! isSet || file == null) {
            return null;
        }
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }
}
