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

import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ServerName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class PartValidation {
    private final Set<ConfigProblem> problems = new HashSet<ConfigProblem>();

    public static Collection<ConfigProblem> findAllProblems(@NotNull DataPart part) {
        final PartValidation validation = new PartValidation();
        validation.problems.addAll(part.getConfigProblems());
        if (! part.hasServerNameSet() || part.getServerName() == null) {
            validation.problems.add(new ConfigProblem("configuration.port.invalid", part.getServerName()));
        }
        validation.checkUsername(part.getUsername());

        return validation.getProblems();
    }

    Collection<ConfigProblem> getProblems() {
        return problems;
    }

    boolean checkPort(@Nullable String rawPort, @Nullable P4ServerName serverName) {
        if (rawPort != null && serverName == null) {
            problems.add(new ConfigProblem("configuration.port.invalid", rawPort));
            return false;
        }
        return true;
    }

    boolean checkAuthTicketFile(@Nullable File file) {
        if (file != null && !file.exists()) {
            problems.add(new ConfigProblem("configuration.problem.authticket.exist", file));
            return false;
        }
        return true;
    }

    boolean checkTrustTicketFile(@Nullable File file) {
        if (file != null && ! file.exists()) {
            problems.add(new ConfigProblem("configuration.problem.trustticket.exist", file));
            return false;
        }
        return true;
    }

    boolean checkUsername(@Nullable String username) {
        if (username == null || username.isEmpty()) {
            problems.add(new ConfigProblem("configuration.problem.username"));
            return false;
        }
        return true;
    }
}
