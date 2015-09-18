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
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Decides the connection approach based on the settings.
 *
 * Can also be used with the P4CONFIG connection.
 */
public class EnvConnectionHandler extends ConnectionHandler {
    public static EnvConnectionHandler INSTANCE = new EnvConnectionHandler();

    private EnvConnectionHandler() {
        // stateless utility class
    }

    @Override
    public Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName) {
        return discoverConnectionMethod(config).getConnectionProperties(config, clientName);
    }

    @Override
    public void defaultAuthentication(@Nullable Project project, @NotNull IOptionsServer server, @NotNull ServerConfig config)
            throws P4JavaException {
        discoverConnectionMethod(config).defaultAuthentication(project, server, config);
    }

    @Override
    public boolean forcedAuthentication(@Nullable Project project, @NotNull IOptionsServer server, @NotNull ServerConfig config)
            throws P4JavaException {
        return discoverConnectionMethod(config).forcedAuthentication(project, server, config);
    }

    @NotNull
    @Override
    public List<ConfigurationProblem> getConfigProblems(@NotNull ServerConfig config) {
        return discoverConnectionMethod(config).getConfigProblems(config);
    }


    @NotNull
    private ConnectionHandler discoverConnectionMethod(@NotNull ServerConfig config) {
        // the config's given authentication method will be not helpful.
        // Instead, we must do what the native Perforce tools do.
        if (config.hasAuthTicket()) {
            File f = config.getAuthTicket();
            if (f != null && f.exists() && f.canRead() && ! f.isDirectory()) {
                return AuthTicketConnectionHandler.INSTANCE;
            }
        }

        // Default to the old way
        return ClientPasswordConnectionHandler.INSTANCE;

        // NOTE: SSO is not currently supported
    }
}
