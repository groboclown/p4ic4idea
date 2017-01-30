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

package net.groboclown.idea.p4ic.config.part;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class PartValidation {
    private static final Logger LOG = Logger.getInstance(PartValidation.class);

    private final Set<ConfigProblem> problems = new HashSet<ConfigProblem>();

    public static Collection<ConfigProblem> findAllProblems(@NotNull DataPart part) {
        final PartValidation validation = new PartValidation();
        validation.problems.addAll(part.getConfigProblems());
        if (! part.hasServerNameSet() || part.getServerName() == null) {
            validation.problems.add(new ConfigProblem(part, "configuration.port.invalid", part.getServerName()));
        }
        validation.checkUsername(part);
        validation.checkAuthTicketFile(part);
        validation.checkTrustTicketFile(part);
        validation.checkLoginSsoFile(part);

        return validation.getProblems();
    }

    Collection<ConfigProblem> getProblems() {
        LOG.info(problems.toString());
        return problems;
    }

    boolean checkPort(@NotNull ConfigPart part, @Nullable String rawPort, @Nullable P4ServerName serverName) {
        if (rawPort != null && serverName == null) {
            problems.add(new ConfigProblem(part, "configuration.port.invalid", rawPort));
            return false;
        }
        return true;
    }

    boolean checkPort(@NotNull DataPart part, @Nullable String rawPort) {
        return checkPort(part, rawPort, part.getServerName());
    }

    boolean checkAuthTicketFile(@NotNull ConfigPart part, @Nullable File file) {
        if (file != null && (! file.exists() || ! file.isFile())) {
            problems.add(new ConfigProblem(part, "configuration.problem.authticket.exist", file));
            return false;
        }
        return true;
    }

    boolean checkAuthTicketFile(@NotNull DataPart part) {
        return checkAuthTicketFile(part, part.getAuthTicketFile());
    }

    boolean checkTrustTicketFile(@NotNull ConfigPart part, @Nullable File file) {
        if (file != null && (! file.exists() || ! file.isFile())) {
            problems.add(new ConfigProblem(part, "configuration.problem.trustticket.exist", file));
            return false;
        }
        return true;
    }

    boolean checkTrustTicketFile(@NotNull DataPart part) {
        return checkTrustTicketFile(part, part.getTrustTicketFile());
    }

    boolean checkLoginSsoFile(@NotNull ConfigPart part, @Nullable File file) {
        if (file != null && (! file.exists() || ! file.isFile())) {
            problems.add(new ConfigProblem(part, "configuration.problem.loginsso.exist", file));
            return false;
        }
        return true;
    }

    boolean checkLoginSsoFile(@NotNull DataPart part) {
        return checkLoginSsoFile(part, part.getLoginSso());
    }

    boolean checkUsername(@NotNull ConfigPart part, @Nullable String username) {
        if (username == null || username.isEmpty()) {
            problems.add(new ConfigProblem(part, "configuration.problem.username"));
            return false;
        }
        return true;
    }

    boolean checkUsername(@NotNull DataPart part) {
        return checkUsername(part, part.getUsername());
    }
}
