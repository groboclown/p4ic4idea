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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import net.groboclown.p4.server.TopCommandRunner;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4PluginVersion;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserResult;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.UserProjectPreferencesUpdatedMessage;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4.server.impl.connection.ConnectCommandRunner;
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4RequestErrorHandler;
import net.groboclown.p4.server.impl.connection.impl.LimitedConnectionManager;
import net.groboclown.p4.server.impl.connection.impl.SimpleConnectionManager;
import net.groboclown.p4plugin.messages.MessageErrorHandler;
import net.groboclown.p4plugin.util.TempDirUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class P4ServerComponent implements ProjectComponent, Disposable {
    public static final String COMPONENT_NAME = "Perforce Server Primary Connection";
    private final Project project;
    private P4CommandRunner commandRunner;
    private AbstractServerCommandRunner connectRunner;
    private boolean disposed = false;

    // An attempt to prevent some potential memory leaks.

    @NotNull
    public static <R extends P4CommandRunner.ServerResult> P4CommandRunner.ActionAnswer<R> perform(
            @NotNull Project project,
            @NotNull OptionalClientServerConfig config,
            @NotNull P4CommandRunner.ServerAction<R> action) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.ActionAnswer<R> ret = instance.first.getCommandRunner().perform(config, action);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }


    @NotNull
    public static <R extends P4CommandRunner.ClientResult> P4CommandRunner.ActionAnswer<R> perform(@NotNull Project project,
            @NotNull ClientConfig config, @NotNull P4CommandRunner.ClientAction<R> action) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.ActionAnswer<R> ret = instance.first.getCommandRunner().perform(config, action);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ServerResult> P4CommandRunner.QueryAnswer<R> query(@NotNull Project project,
            @NotNull OptionalClientServerConfig config,
            @NotNull P4CommandRunner.ServerQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.QueryAnswer<R> ret = instance.first.getCommandRunner().query(config, query);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ClientResult> P4CommandRunner.QueryAnswer<R> query(@NotNull Project project,
            @NotNull ClientConfig config, @NotNull P4CommandRunner.ClientQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.QueryAnswer<R> ret = instance.first.getCommandRunner().query(config, query);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ServerNameResult> P4CommandRunner.QueryAnswer<R> query(@NotNull Project project,
            @NotNull P4ServerName name, @NotNull P4CommandRunner.ServerNameQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.QueryAnswer<R> ret = instance.first.getCommandRunner().query(name, query);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ServerResult> R syncCachedQuery(@NotNull Project project,
            @NotNull OptionalClientServerConfig config,
            @NotNull P4CommandRunner.SyncServerQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        R ret = instance.first.getCommandRunner().syncCachedQuery(config, query);
        if (instance.second) {
            instance.first.dispose();
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ClientResult> R syncCachedQuery(@NotNull Project project,
            @NotNull ClientConfig config, @NotNull P4CommandRunner.SyncClientQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        R ret = instance.first.getCommandRunner().syncCachedQuery(config, query);
        if (instance.second) {
            instance.first.dispose();
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ServerResult> P4CommandRunner.FutureResult<R> syncQuery(
            @NotNull Project project,
            @NotNull OptionalClientServerConfig config,
            @NotNull P4CommandRunner.SyncServerQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.FutureResult<R> ret = instance.first.getCommandRunner().syncQuery(config, query);
        if (instance.second) {
            ret.getPromise().whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    @NotNull
    public static <R extends P4CommandRunner.ClientResult> P4CommandRunner.FutureResult<R> syncQuery(@NotNull Project project, @NotNull ClientConfig config, @NotNull P4CommandRunner.SyncClientQuery<R> query) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.FutureResult<R> ret = instance.first.getCommandRunner().syncQuery(config, query);
        if (instance.second) {
            ret.getPromise().whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    public static P4CommandRunner.QueryAnswer<ListClientsForUserResult> getClientsForUser(@NotNull Project project,
            @NotNull OptionalClientServerConfig config) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.QueryAnswer<ListClientsForUserResult> ret = instance.first.getClientsForUser(config);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    public static P4CommandRunner.QueryAnswer<ListClientsForUserResult> checkServerConnection(@NotNull Project project,
            @NotNull OptionalClientServerConfig config) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.QueryAnswer<ListClientsForUserResult> ret = instance.first.checkServerConnection(config);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    public static P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> checkClientConnection(@NotNull Project project, ClientConfig clientConfig) {
        final Pair<P4ServerComponent, Boolean> instance = findInstance(project);
        P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> ret = instance.first.checkClientConnection(clientConfig);
        if (instance.second) {
            ret.whenAnyState(instance.first::dispose);
        }
        return ret;
    }

    public static P4CommandRunner.ActionAnswer<Void> sendCachedPendingRequests(
            @NotNull Project project, @NotNull ClientConfig clientConfig) {
        P4ServerComponent ret = project.getComponent(P4ServerComponent.class);
        if (ret == null || ret.getCommandRunner() == null) {
            return new DoneActionAnswer<>(null);
        }
        return ret.getCommandRunner().sendCachedPendingRequests(clientConfig);
    }

    private static Pair<P4ServerComponent, Boolean> findInstance(@NotNull Project project) {
        // a non-registered component can happen when the config is loaded outside a project.
        P4ServerComponent ret = project.getComponent(P4ServerComponent.class);
        boolean mustBeDisposed = false;
        if (ret == null) {
            ret = new P4ServerComponent(project);
            ret.initComponent();
            mustBeDisposed = true;
        }
        return Pair.create(ret, mustBeDisposed);
    }




    public P4ServerComponent(@NotNull Project project) {
        this.project = project;
    }

    public P4CommandRunner getCommandRunner() {
        // This is necessary for loading a project from version control when the project isn't setup yet.
        // Init is happening earlier now
        // initComponent();
        return commandRunner;
    }

    // For Configuration UI.  Avoids cache hits.
    public P4CommandRunner.QueryAnswer<ListClientsForUserResult> getClientsForUser(
            @NotNull OptionalClientServerConfig config) {
        // This is necessary for loading a project from version control when the project isn't setup yet.
        // Init is happening earlier now
        // initComponent();
        return connectRunner.getClientsForUser(config,
                new ListClientsForUserQuery(config.getUsername(),
                        UserProjectPreferences.getMaxClientRetrieveCount(project)));
    }

    // For Configuration UI.  Avoids cache hits.
    public P4CommandRunner.QueryAnswer<ListClientsForUserResult> checkServerConnection(
            @NotNull OptionalClientServerConfig config) {
        // This is necessary for loading a project from version control when the project isn't setup yet.
        // Init is happening earlier now
        // initComponent();
        return connectRunner.getClientsForUser(config,
                new ListClientsForUserQuery(config.getUsername(), 1));
    }


    // For Configuration UI.  Avoids cache hits.
    private P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> checkClientConnection(ClientConfig clientConfig) {
        // This is necessary for loading a project from version control when the project isn't setup yet.
        // Init is happening earlier now
        // initComponent();
        return connectRunner.listOpenedFilesChanges(
                // Checking the connection doesn't require a root; we don't care about the files returned.
                clientConfig, new ListOpenedFilesChangesQuery(null, 1, 1));
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
        // do nothing
    }

    @Override
    public void initComponent() {
        if (disposed) {
            throw new IllegalStateException("disposed");
        }
        if (connectRunner == null) {
            connectRunner = new ConnectCommandRunner(project, createConnectionManager());
        }
        if (commandRunner == null) {
            commandRunner = new TopCommandRunner(project,
                    CacheComponent.getInstance(project).getCacheQuery(),
                    CacheComponent.getInstance(project).getCachePending(),
                    connectRunner, this);
        }
    }

    @Override
    public void disposeComponent() {
        dispose();
    }

    @Override
    public void dispose() {
        if (commandRunner != null) {
            commandRunner = null;
        }
        if (connectRunner != null) {
            connectRunner = null;
        }
        if (!disposed) {
            disposed = true;
            Disposer.dispose(this);
        }
    }

    public boolean isDisposed() {
        return disposed;
    }


    @NotNull
    protected ConnectionManager createConnectionManager() {
        final SimpleConnectionManager scm = new SimpleConnectionManager(
                TempDirUtil.getTempDir(project),
                UserProjectPreferences.getSocketSoTimeoutMillis(project),
                P4PluginVersion.getPluginVersion(),
                createErrorHandler()
        );
        ConnectionManager ret = scm;
        final LimitedConnectionManager lcm;
        final int connectionRestriction = UserProjectPreferences.getMaxServerConnections(project);
        if (connectionRestriction > 0) {
            lcm = new LimitedConnectionManager(scm, connectionRestriction,
                    UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            ret = lcm;
        } else {
            lcm = null;
        }
        // The message bus client handles the listener disposing implicitly.
        MessageBusClient.ProjectClient mbus = MessageBusClient.forProject(project, this);

        // Due to the protections added to the message bus, we can only add the listener once per message bus.
        // Thus, all the extra null checks.  This is all in service of bug #193.
        UserProjectPreferencesUpdatedMessage.addListener(mbus, this,
                e -> {
            scm.setSocketSoTimeoutMillis(UserProjectPreferences.getSocketSoTimeoutMillis(project));
            if (lcm != null) {
                lcm.setLockTimeout(
                        UserProjectPreferences.getLockWaitTimeoutMillis(project),
                        TimeUnit.MILLISECONDS);
            }
        });
        return ret;
    }


    protected P4RequestErrorHandler createErrorHandler() {
        return new MessageErrorHandler(project);
    }
}
