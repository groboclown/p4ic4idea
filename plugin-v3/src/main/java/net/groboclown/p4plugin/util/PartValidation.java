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

package net.groboclown.p4plugin.util;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PartValidation {
    private static final Logger LOG = Logger.getInstance(PartValidation.class);

    private final Set<ConfigProblem> problems = new HashSet<>();

    public static Collection<ConfigProblem> findAllProblems(@NotNull ConfigPart part) {
        final PartValidation validation = new PartValidation();
        validation.problems.addAll(part.getConfigProblems());
        validation.checkPort(part);
        validation.checkUsername(part);
        validation.checkAuthTicketFile(part);
        validation.checkTrustTicketFile(part);
        validation.checkLoginSsoFile(part);
        validation.checkClientName(part, true);

        return validation.getProblems();
    }

    private Collection<ConfigProblem> getProblems() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(problems.toString());
        }
        return problems;
    }

    private void checkPort(@NotNull ConfigPart part, @Nullable String rawPort, @Nullable P4ServerName serverName) {
        if (rawPort != null && serverName == null) {
            problems.add(new ConfigProblem(part, P4Bundle.message("configuration.port.invalid", rawPort), true));
        }
    }

    private void checkPort(@NotNull ConfigPart part) {
        checkPort(part, part.getRawPort(), part.getServerName());
    }

    private void checkAuthTicketFile(@NotNull ConfigPart part, @Nullable File file) {
        // If it points to a directory, then we ignore this.
        if (file != null && ! file.exists()) {
            problems.add(new ConfigProblem(part, P4Bundle.message("configuration.problem.authticket.exist", file), false));
        }
    }

    private void checkAuthTicketFile(@NotNull ConfigPart part) {
        checkAuthTicketFile(part, part.getAuthTicketFile());
    }

    private void checkTrustTicketFile(@NotNull ConfigPart part, @Nullable File file) {
        // If it points to a directory, then we ignore this.
        if (file != null && ! file.exists()) {
            problems.add(new ConfigProblem(part, P4Bundle.message("configuration.problem.trustticket.exist", file), false));
        }
    }

    private void checkTrustTicketFile(@NotNull ConfigPart part) {
        checkTrustTicketFile(part, part.getTrustTicketFile());
    }

    private void checkLoginSsoFile(@NotNull ConfigPart part, @Nullable String file) {
        if (file != null && file.isEmpty()) {
            problems.add(new ConfigProblem(part, P4Bundle.message("configuration.problem.loginsso.exist", file), false));
        }
    }

    private void checkLoginSsoFile(@NotNull ConfigPart part) {
        checkLoginSsoFile(part, part.getLoginSso());
    }

    private void checkUsername(@NotNull ConfigPart part, @Nullable String username) {
        if (username == null || username.isEmpty()) {
            problems.add(new ConfigProblem(part, P4Bundle.message("configuration.problem.username"), true));
        }
    }

    private void checkUsername(@NotNull ConfigPart part) {
        checkUsername(part, part.getUsername());
    }

    private void checkClientName(@NotNull ConfigPart part, boolean ensureNotNull) {
        checkClientName(part, part.getClientname(), ensureNotNull);
    }

    private void checkClientName(@NotNull ConfigPart part, @Nullable String clientName, boolean ensureNotNull) {
        if (clientName != null) {
            try {
                Integer.parseInt(clientName);
                problems.add(new ConfigProblem(part, P4Bundle.message("error.config.client.numeric", clientName),
                        true));
            } catch (NumberFormatException e) {
                // This is fine.  Ignore.
            }
        } else if (ensureNotNull) {
            problems.add(new ConfigProblem(part, P4Bundle.message("error.config.no-client"), true));
        }
    }
}
