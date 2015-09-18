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
package net.groboclown.idea.p4ic.server.connection;

import com.intellij.openapi.project.Project;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AuthTicketConnectionHandler extends ClientPasswordConnectionHandler {
    public static AuthTicketConnectionHandler INSTANCE = new AuthTicketConnectionHandler();

    private AuthTicketConnectionHandler() {
        // Utility class
    }

    @Override
    public Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName) {
        Properties ret = super.getConnectionProperties(config, clientName);
        if (config.getAuthTicket() != null) {
            ret.setProperty(PropertyDefs.TICKET_PATH_KEY, config.getAuthTicket().getAbsolutePath());
        }

        return ret;
    }

    @Override
    public void defaultAuthentication(@Nullable Project project, @NotNull IOptionsServer server, @NotNull ServerConfig config) throws P4JavaException {
        // no need to login - assume the ticket is valid.
    }

    @NotNull
    @Override
    public List<ConfigurationProblem> getConfigProblems(@NotNull final ServerConfig config) {
        List<ConfigurationProblem> problems = new ArrayList<ConfigurationProblem>(super.getConfigProblems(config));
        if (config.getAuthTicket() == null) {
            problems.add(new ConfigurationProblem(P4Bundle.message("configuration.problem.authticket")));
        }
        return problems;
    }
}
