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

package net.groboclown.p4.server;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionCacheUpdateMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionCompletedCacheUpdateMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheUpdateMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCompletedCacheUpdateMessage;
import net.groboclown.p4.server.api.commands.server.LoginResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.cache.CacheQueryHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layers a cache and server.
 */
public class TopCommandRunner
        implements P4CommandRunner, Disposable {
    private final CacheQueryHandler cache;
    private final AbstractServerCommandRunner server;
    private final Map<String, ServerConnectionState> stateCache = new HashMap<>();
    private boolean disposed;


    public TopCommandRunner(@NotNull Project project,
            @NotNull CacheQueryHandler cache, @NotNull AbstractServerCommandRunner server) {
        this.cache = cache;
        this.server = server;

        MessageBusClient.ApplicationClient appClient = MessageBusClient.forApplication(this);
        MessageBusClient.ProjectClient projClient = MessageBusClient.forProject(project, this);
        ServerConnectedMessage.addListener(appClient, serverConfig -> {
            // User connected to the server.  No change to login state.
            ServerConnectionState state = getStateFor(serverConfig);
            state.badConnection = false;
            state.userOffline = false;

            sendPendingCacheRequests(serverConfig);
        });
        UserSelectedOfflineMessage.addListener(projClient, name -> {
            // User wants to work offline, regardless of connection status.
            for (ServerConnectionState state : getStatesFor(name)) {
                state.userOffline = true;
                server.disconnect(state.config);
            }
        });
        ReconnectRequestMessage.addListener(projClient, new ReconnectRequestMessage.Listener() {
            // The user requested to go online, so clear out the states
            // that might cause a request to not be fulfilled.
            @Override
            public void reconnectToAllClients(boolean mayDisplayDialogs) {
                for (ServerConnectionState state : getAllStates()) {
                    state.userOffline = false;
                    state.badConnection = false;
                    state.badLogin = false;
                    state.needsLogin = false;

                    sendPendingCacheRequests(state.config);
                }
            }

            @Override
            public void reconnectToClient(@NotNull ClientServerRef ref, boolean mayDisplayDialogs) {
                for (ServerConnectionState state : getStatesFor(ref.getServerName())) {
                    state.userOffline = false;
                    state.badConnection = false;
                    state.badLogin = false;
                    state.needsLogin = false;

                    sendPendingCacheRequests(state.config);
                }
            }
        });
        LoginFailureMessage.addListener(appClient, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = true;
            }

            @Override
            public void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = false;
                getStateFor(config).needsLogin = true;
            }

            @Override
            public void passwordInvalid(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = true;
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = false;
                getStateFor(config).passwordUnnecessary = true;
            }
        });
        ConnectionErrorMessage.addListener(appClient, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public void onHostConnectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @Nullable Exception e) {
                if (serverConfig != null) {
                    getStateFor(serverConfig).badConnection = true;
                } else {
                    for (ServerConnectionState state : getStatesFor(serverName)) {
                        state.badConnection = true;
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> Promise<R> perform(@NotNull ServerConfig config, @NotNull ServerAction<R> action) {
        ServerConnectionState state = getStateFor(config);
        // Some action requests need to be handled and never cached.
        switch (action.getCmd()) {
            case LOGIN:
                if (state.badLogin && !state.userOffline && !state.badConnection) {
                    return server.perform(config, action)
                            .then((resp) -> {
                                // Login was good.
                                state.needsLogin = false;
                                state.badConnection = false;
                                return resp;
                            });
                }
                return (Promise<R>) Promise.resolve(new LoginResult(config));
            default:
                ServerActionCacheUpdateMessage.sendEvent(new ServerActionCacheUpdateMessage.Event(
                        config.getServerName(), action
                ));
                // TODO if the state is something that requires a login, should that be done here?
                if (state.shouldRunCommand()) {
                    return server.perform(config, action)
                            .then((result) -> {
                                ServerActionCompletedCacheUpdateMessage.sendEvent(
                                        new ServerActionCompletedCacheUpdateMessage.Event(config.getServerName(),
                                                action));
                                return result;
                            });
                }
                // FIXME is this a valid response?
                // Alternatively, we create a static mapping of result values.
                return Promise.resolve(null);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> Promise<R> perform(@NotNull ClientConfig config, @NotNull ClientAction<R> action) {
        // All these commands need to be cached.
        ClientActionCacheUpdateMessage.sendEvent(new ClientActionCacheUpdateMessage.Event(
                config.getClientServerRef(), action));

        // TODO if the state is something that requires a login, should that be done here?
        ServerConnectionState state = getStateFor(config.getServerConfig());
        if (state.shouldRunCommand()) {
            return server.perform(config, action)
                    .then((result) -> {
                        ClientActionCompletedCacheUpdateMessage.sendEvent(
                                new ClientActionCompletedCacheUpdateMessage.Event(config.getClientServerRef(),
                                        action)
                        );
                        return result;
                    });
        }
        // FIXME is this a valid response?
        // Alternatively, we create a static mapping of result values.
        return Promise.resolve(null);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> Promise<R> query(@NotNull ServerConfig config, @NotNull ServerQuery<R> query) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> Promise<R> query(@NotNull ClientConfig config, @NotNull ClientQuery<R> query) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerNameResult> Promise<R> query(@NotNull P4ServerName name,
            @NotNull ServerNameQuery<R> query) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query)
            throws ServerResultException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query)
            throws ServerResultException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query)
            throws ServerResultException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query)
            throws ServerResultException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    private ServerConnectionState getStateFor(@NotNull ServerConfig config) {
        ServerConnectionState state;
        synchronized (stateCache) {
            state = stateCache.get(config.getServerId());
            if (state == null) {
                state = new ServerConnectionState(config);
                stateCache.put(config.getServerId(), state);
            }
        }
        return state;
    }

    private Collection<ServerConnectionState> getStatesFor(P4ServerName name) {
        List<ServerConnectionState> ret = new ArrayList<>();
        synchronized (stateCache) {
            for (ServerConnectionState state : stateCache.values()) {
                if (state.config.getServerName().equals(name)) {
                    ret.add(state);
                }
            }
        }
        return ret;
    }

    private Collection<ServerConnectionState> getAllStates() {
        synchronized (stateCache) {
            return new ArrayList<>(stateCache.values());
        }
    }

    private void sendPendingCacheRequests(ServerConfig serverConfig) {
        // FIXME pull the pending actions and perform them, and perform
        // and requested cache updates.
    }

    private static class ServerConnectionState {
        final ServerConfig config;
        boolean badConnection;
        boolean badLogin;
        boolean needsLogin;
        boolean passwordUnnecessary;
        boolean userOffline;

        private ServerConnectionState(ServerConfig config) {
            this.config = config;
        }

        /**
         *
         * @return standard check if the command should run.  Requires login.
         */
        boolean shouldRunCommand() {
            return !(badConnection || badLogin || userOffline || needsLogin);
        }
    }

}
