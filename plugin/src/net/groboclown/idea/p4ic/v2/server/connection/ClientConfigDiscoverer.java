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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.P4ProjectConfigComponent;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Different methods to find a {@link ClientConfig} based on other
 * objects.
 */
public class ClientConfigDiscoverer {
    @Nullable
    public static ClientConfig find(@NotNull P4ProjectConfig projectConfig, @NotNull ServerConfig server,
            @NotNull String clientName)
            throws P4InvalidConfigException {
        for (ClientConfig clientConfig : projectConfig.getClientConfigs()) {
            if (clientConfig.getServerConfig().equals(server) && clientName.equals(clientConfig.getClientName())) {
                return clientConfig;
            }
        }
        return null;
    }


    @Nullable
    public static ClientConfig find(@NotNull Project project, @NotNull P4ServerName serverName,
            @NotNull String clientName) {
        for (ClientConfig clientConfig : getAllClientConfigs(project)) {
            if (serverName.equals(clientConfig.getServerConfig().getServerName()) &&
                    clientName.equals(clientConfig.getClientName())) {
                return clientConfig;
            }
        }
        return null;
    }


    @NotNull
    private static Collection<ClientConfig> getAllClientConfigs(@NotNull Project project) {
        P4ProjectConfigComponent projectConfigComponent = P4ProjectConfigComponent.getInstance(project);
        if (projectConfigComponent == null) {
            return Collections.emptyList();
        }
        return projectConfigComponent.getP4ProjectConfig().getClientConfigs();
    }
}
