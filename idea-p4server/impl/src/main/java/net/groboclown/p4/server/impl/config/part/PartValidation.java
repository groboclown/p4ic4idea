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

package net.groboclown.p4.server.impl.config.part;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// FIXME this whole thing needs messaging.
public class PartValidation {
    private static final Logger LOG = Logger.getInstance(PartValidation.class);

    private final Set<ConfigProblem> problems = new HashSet<ConfigProblem>();

    public static Collection<ConfigProblem> findAllProblems(@NotNull ConfigPart part) {
        final PartValidation validation = new PartValidation();
        validation.problems.addAll(part.getConfigProblems());
        if (! part.hasServerNameSet() || part.getServerName() == null) {
            validation.problems.add(new ConfigProblem(part, "configuration.port.invalid", true));
        }
        validation.checkUsername(part);
        validation.checkAuthTicketFile(part);
        validation.checkTrustTicketFile(part);
        validation.checkLoginSsoFile(part);
        validation.checkClientName(part, true);

        return validation.getProblems();
    }

    Collection<ConfigProblem> getProblems() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(problems.toString());
        }
        return problems;
    }

    boolean checkPort(@NotNull ConfigPart part, @Nullable String rawPort, @Nullable P4ServerName serverName) {
        if (rawPort != null && serverName == null) {
            // FIXME add rawPort in message
            problems.add(new ConfigProblem(part, "configuration.port.invalid", true));
            return false;
        }
        return true;
    }

    boolean checkPort(@NotNull ConfigPart part, @Nullable String rawPort) {
        return checkPort(part, rawPort, part.getServerName());
    }

    boolean checkAuthTicketFile(@NotNull ConfigPart part, @Nullable File file) {
        // If it points to a directory, then we ignore this.
        if (file != null && ! file.exists()) {
            // FIXME include "file" in message
            problems.add(new ConfigProblem(part, "configuration.problem.authticket.exist", false));
            return false;
        }
        return true;
    }

    boolean checkAuthTicketFile(@NotNull ConfigPart part) {
        return checkAuthTicketFile(part, part.getAuthTicketFile());
    }

    boolean checkTrustTicketFile(@NotNull ConfigPart part, @Nullable File file) {
        // If it points to a directory, then we ignore this.
        if (file != null && ! file.exists()) {
            // FIXME add "File" in message
            problems.add(new ConfigProblem(part, "configuration.problem.trustticket.exist", false));
            return false;
        }
        return true;
    }

    boolean checkTrustTicketFile(@NotNull ConfigPart part) {
        return checkTrustTicketFile(part, part.getTrustTicketFile());
    }

    boolean checkLoginSsoFile(@NotNull ConfigPart part, @Nullable String file) {
        if (file != null && file.isEmpty()) {
            // FIXME add "File" in message
            problems.add(new ConfigProblem(part, "configuration.problem.loginsso.exist", false));
            return false;
        }
        return true;
    }

    boolean checkLoginSsoFile(@NotNull ConfigPart part) {
        return checkLoginSsoFile(part, part.getLoginSso());
    }

    boolean checkUsername(@NotNull ConfigPart part, @Nullable String username) {
        if (username == null || username.isEmpty()) {
            problems.add(new ConfigProblem(part, "configuration.problem.username", true));
            return false;
        }
        return true;
    }

    boolean checkUsername(@NotNull ConfigPart part) {
        return checkUsername(part, part.getUsername());
    }

    boolean checkClientName(@NotNull ConfigPart part, boolean ensureNotNull) {
        return checkClientName(part, part.getClientname(), ensureNotNull);
    }

    private boolean checkClientName(@NotNull ConfigPart part, @Nullable String clientName, boolean ensureNotNull) {
        if (clientName != null) {
            try {
                Integer.parseInt(clientName);
                problems.add(new ConfigProblem(part, "error.config.client.numeric", true));
            } catch (NumberFormatException e) {
                // This is fine.  Ignore.
            }
        } else if (ensureNotNull) {
            problems.add(new ConfigProblem(part, "error.config.no-client", false));
        }
        return false;
    }
}
