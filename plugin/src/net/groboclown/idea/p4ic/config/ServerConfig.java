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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import net.groboclown.idea.p4ic.config.part.DataPart;
import net.groboclown.idea.p4ic.config.part.PartValidation;
import net.groboclown.idea.p4ic.util.EqualUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Stores the connection information related to a specific Perforce
 * server.  It only keeps track of information required to identify the
 * server, which means it needs the username, password, auth ticket file,
 * trust ticket file, and server fingerprint.
 * <p>
 * Implementations must specify {@link #equals(Object)} and
 * {@link #hashCode()}, to indicate whether the server connection
 * properties are the same; it should not match on online mode.
 */
public final class ServerConfig {
    private static final Logger LOG = Logger.getInstance(ServerConfig.class);

    // Just some character that won't appear in any real text, but
    // is still viewable in a debugger.
    private static final char SEP = (char) 0x2202;

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private final int configVersion;
    private final P4ServerName serverName;
    private final String username;
    private final File authTicket;
    private final File trustTicket;
    private final String serverFingerprint;
    private final String loginSso;

    // This really shouldn't just be here in plaintext, however
    // the source of the value is stored in plaintext on the user's
    // computer anyway.
    private final String password;

    private final String serverId;

    @NotNull
    static String getServerIdForDataPart(@NotNull DataPart part) {
        StringBuilder sb = new StringBuilder();
        if (part.hasServerNameSet() && part.getServerName() != null) {
            sb.append(part.getServerName().getFullPort());
        } else {
            sb.append((String) null);
        }
        sb.append(SEP)
            .append(part.hasUsernameSet() ? part.getUsername() : null)
            .append(SEP)
            .append(part.hasPasswordSet()
                ? (part.getPlaintextPassword() == null
                    ? ""
                    : "<password>")
                : null)
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
    public static ServerConfig createFrom(@NotNull DataPart part) {
        return new ServerConfig(part);
    }

    public static boolean isValid(@Nullable DataPart part) {
        // Could be optimized in the future.
        return getErrors(part).isEmpty();
    }

    static Collection<ConfigProblem> getErrors(@Nullable DataPart part) {
        if (part == null) {
            return Collections.singletonList(new ConfigProblem(part, false, "config.display.key.no-value"));
        }
        List<ConfigProblem> ret = new ArrayList<ConfigProblem>();
        for (ConfigProblem configProblem : PartValidation.findAllProblems(part)) {
            if (configProblem.isError()) {
                ret.add(configProblem);
            }
        }
        return ret;
    }

    private ServerConfig(@NotNull DataPart part) {
        if (! isValid(part)) {
            throw new IllegalStateException("Did not check validity before creating");
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
        this.password =
                part.hasPasswordSet()
                        ? (part.getPlaintextPassword() == null
                        ? ""
                        : part.getPlaintextPassword())
                        : null;


        serverId = getServerIdForDataPart(part);
    }


    public int getConfigVersion() {
        return this.configVersion;
    }


    @NotNull
    public P4ServerName getServerName() {
        return serverName;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     *
     * @return the password the user stored in plaintext, if they stored it themselves.
     *      Should only return non-null when the user has a P4CONFIG file with this value
     *      set, or a P4PASSWD env set.
     */
    @Nullable
    public String getPlaintextPassword() {
        return password;
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

    boolean isSameServer(@Nullable DataPart part) {
        if (part == null) {
            LOG.debug("isSameServer: input is null");
            return false;
        }

        if (! EqualUtil.isEqual(getServerName(), part.getServerName())) {
            if (LOG.isDebugEnabled()) {
                if (part.getServerName() == null) {
                    LOG.debug("isSameServer: input server name is null");
                } else {
                    LOG.debug("isSameServer: server doesn't match: "
                            + getServerName().getServerPort() + "::" + getServerName().getServerProtocol() + " <> "
                            + part.getServerName().getServerPort() + "::" + part.getServerName().getServerProtocol());
                }
            }
            return false;
        }

        if (! filesEqual(hasAuthTicket(), getAuthTicket(), part.hasAuthTicketFileSet(), part.getAuthTicketFile())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServer: auth ticket doesn't match: "
                        + getAuthTicket() + " <> " + part.getAuthTicketFile());
            }
            return false;
        }

        if (! filesEqual(hasTrustTicket(), getTrustTicket(), part.hasTrustTicketFileSet(), part.getTrustTicketFile())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServer: trust ticket doesn't match: "
                        + getTrustTicket() + " <> " + part.getTrustTicketFile());
            }
            return false;
        }

        if (hasServerFingerprint() != part.hasServerFingerprintSet()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServer: has server fingerprint mismatch: "
                        + hasServerFingerprint() + " <> " + part.hasServerFingerprintSet());
            }
            return false;
        }
        if (hasServerFingerprint() && ! EqualUtil.isEqual(getServerFingerprint(), part.getServerFingerprint())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServer: server fingerprint mismatch: "
                        + getServerFingerprint() + " <> " + part.getServerFingerprint());
            }
            return false;
        }

        if (! EqualUtil.isEqual(getUsername(), part.getUsername())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isSameServer: username mismatch: "
                       + getUsername() + " <> " + part.getUsername());
            }
        }

        return true;
    }

    @NotNull
    private Map<String, String> toProperties() {
        return ConfigPropertiesUtil.toProperties(this);
    }

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
