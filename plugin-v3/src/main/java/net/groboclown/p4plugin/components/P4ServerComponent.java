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

package net.groboclown.p4plugin.components;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.TopCommandRunner;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4PluginVersion;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserResult;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.connection.ConnectCommandRunner;
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.impl.LimitedConnectionManager;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4plugin.messages.MessageErrorHandler;
import net.groboclown.p4plugin.util.TempDirUtil;
import org.jetbrains.annotations.NotNull;

public class P4ServerComponent implements ProjectComponent {
    private static final String COMPONENT_NAME = "Perforce Server Primary Connection";
    private final Project project;
    private P4CommandRunner commandRunner;
    private AbstractServerCommandRunner connectRunner;

    public static P4ServerComponent getInstance(Project project) {
        // a non-registered component can happen when the config is loaded outside a project.
        P4ServerComponent ret = null;
        if (project != null) {
            ret = project.getComponent(P4ServerComponent.class);
        }
        if (ret == null) {
            ret = new P4ServerComponent(project);
        }
        return ret;
    }


    @SuppressWarnings("WeakerAccess")
    public P4ServerComponent(Project project) {
        this.project = project;
    }

    public P4CommandRunner getCommandRunner() {
        return commandRunner;
    }

    // For Configuration UI.  Avoids cache hits.
    public P4CommandRunner.QueryAnswer<ListClientsForUserResult> getClientsForUser(ServerConfig config) {
        return connectRunner.getClientsForUser(config, new ListClientsForUserQuery(config.getUsername(),
                UserProjectPreferences.getMaxClientRetrieveCount(project)));
    }

    // For Configuration UI.  Avoids cache hits.
    public P4CommandRunner.QueryAnswer<ListClientsForUserResult> checkServerConnection(ServerConfig config) {
        return connectRunner.getClientsForUser(config, new ListClientsForUserQuery(config.getUsername(), 1));
    }


    // For Configuration UI.  Avoids cache hits.
    public P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> checkClientConnection(ClientConfig clientConfig) {
        return connectRunner.listOpenedFilesChanges(
                clientConfig, new ListOpenedFilesChangesQuery(1, 1));
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        if (connectRunner == null) {
            connectRunner = new ConnectCommandRunner(createConnectionManager());
        }
        if (commandRunner == null) {
            commandRunner = new TopCommandRunner(project,
                    CacheComponent.getInstance(project).getCacheQuery(),
                    CacheComponent.getInstance(project).getCachePending(),
                    connectRunner);
        }
    }

    @Override
    public void disposeComponent() {
        if (commandRunner != null) {
            commandRunner = null;
        }
    }


    @NotNull
    protected ConnectionManager createConnectionManager() {
        ConnectionManager ret = new SimpleConnectionManager(
                TempDirUtil.getTempDir(project),
                UserProjectPreferences.getSocketSoTimeoutMillis(project),
                P4PluginVersion.getPluginVersion(),
                createErrorHandler()
        );
        int connectionRestriction = UserProjectPreferences.getMaxServerConnections(project);
        if (connectionRestriction > 0) {
            ret = new LimitedConnectionManager(ret, connectionRestriction);
        }
        return ret;
    }


    protected P4RequestErrorHandler createErrorHandler() {
        return new MessageErrorHandler(project);
    }
}
