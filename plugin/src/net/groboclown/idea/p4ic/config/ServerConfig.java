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

import com.intellij.openapi.util.io.FileUtil;
import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.DataPart;
import net.groboclown.idea.p4ic.config.part.PartValidation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;


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
    // Just some character that won't appear in any real text, but
    // is still viewable in a debugger.
    private static final char SEP = (char) 0x2202;

    private final P4ServerName serverName;
    private final String username;
    private final File authTicket;
    private final File trustTicket;
    private final String serverFingerprint;

    // This really shouldn't just be here in plaintext, however
    // the source of the value is stored in plaintext on the user's
    // computer anyway.
    private final String password;

    private final String serverId;



    @NotNull
    public static ServerConfig createFrom(@NotNull DataPart part) {
        return new ServerConfig(part);
    }

    public static boolean isValid(@Nullable DataPart part) {
        // Could be optimized in the future.
        return getProblems(part).isEmpty();
    }

    public static Collection<ConfigProblem> getProblems(@Nullable DataPart part) {
        if (part == null) {
            return Collections.singletonList(new ConfigProblem(part, "config.display.key.no-value"));
        }
        return PartValidation.findAllProblems(part);
    }

    private ServerConfig(@NotNull DataPart part) {
        if (! isValid(part)) {
            throw new IllegalStateException("Did not check validity before creating");
        }

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
        this.password =
                part.hasPasswordSet()
                        ? (part.getPlaintextPassword() == null
                        ? ""
                        : part.getPlaintextPassword())
                        : null;

        serverId = this.serverName.getFullPort() + SEP +
                this.username + SEP +
                (this.password != null ? "(PWD)" : "(NPWD") + SEP +
                this.authTicket + SEP +
                this.trustTicket + SEP +
                this.serverFingerprint;
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

    public boolean hasServerFingerprint() {
        return getServerFingerprint() != null && getServerFingerprint().length() > 0;
    }

    public boolean hasAuthTicket() {
        return getAuthTicket() != null;
    }

    public boolean hasTrustTicket() {
        return getTrustTicket() != null;
    }

    /**
     *
     * @return unique identifier for the server connection settings.
     */
    @NotNull
    public String getServerId() {
        return serverId;
    }

    public boolean isSameServer(@Nullable DataPart part) {
        if (part == null) {
            return false;
        }
        if (! getServerName().equals(part.getServerName())) {
            return false;
        }

        if (hasAuthTicket() && ! FileUtil.filesEqual(getAuthTicket(), part.getAuthTicketFile())) {
            return false;
        }
        if (hasAuthTicket() != part.hasAuthTicketFileSet()) {
            return false;
        }

        if (hasTrustTicket() && ! FileUtil.filesEqual(getTrustTicket(), part.getTrustTicketFile())) {
            return false;
        }
        if (hasTrustTicket() != part.hasTrustTicketFileSet()) {
            return false;
        }

        if (hasServerFingerprint() && ! getServerFingerprint().equals(part.getServerFingerprint())) {
            return false;
        }
        if (hasServerFingerprint() != part.hasServerFingerprintSet()) {
            return false;
        }

        return true;
    }

    @NotNull
    public Map<String, String> toProperties() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4PORT, getServerName().getDisplayName());
        ret.put(PerforceEnvironment.P4TRUST,
                getTrustTicket() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : getTrustTicket().toString());
        ret.put(PerforceEnvironment.P4USER, getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                getAuthTicket() == null
                        ? P4Bundle.getString("configuration.resolve.value.unset")
                        : getAuthTicket().toString());
        ret.put("Server Fingerprint", getServerFingerprint());
        ret.put(PerforceEnvironment.P4PASSWD,
                getPlaintextPassword() == null
                        ? P4Bundle.getString("configuration.resolve.password.unset")
                        : P4Bundle.getString("configuration.resolve.password.set"));
        return ret;
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
}
