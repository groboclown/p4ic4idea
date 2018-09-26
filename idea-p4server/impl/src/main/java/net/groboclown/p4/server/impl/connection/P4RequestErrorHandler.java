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

package net.groboclown.p4.server.impl.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Handles errors generated during the processing of a request against a
 * Perforce server.  Errors should be pushed to the message bus where
 * appropriate.
 */
public abstract class P4RequestErrorHandler {
    protected static final Logger LOG = Logger.getInstance(P4RequestErrorHandler.class);


    /**
     * Translates any Perforce server interaction error into the correct
     * {@link net.groboclown.p4.server.api.P4CommandRunner.ServerResultException},
     * or just return the caller's output if there was no error.
     *
     * @param c callable that's processing the Perforce server.
     * @param <R> return type
     * @return the processed value
     * @throws P4CommandRunner.ServerResultException translated exception
     */
    @Nullable
    public final <R> R handle(@NotNull ClientConfig config, @NotNull Callable<R> c)
            throws P4CommandRunner.ServerResultException {
        return handleConnection(new ConnectionInfo(config), c);
    }

    /**
     * Translates any Perforce server interaction error into the correct
     * {@link net.groboclown.p4.server.api.P4CommandRunner.ServerResultException},
     * or just return the caller's output if there was no error.
     *
     * @param c callable that's processing the Perforce server.
     * @param <R> return type
     * @return the processed value
     * @throws P4CommandRunner.ServerResultException translated exception
     */
    @Nullable
    public final <R> R handle(@NotNull ServerConfig config, @NotNull Callable<R> c)
            throws P4CommandRunner.ServerResultException {
        return handleConnection(new ConnectionInfo(config), c);
    }

    /**
     * Translates any Perforce server interaction error into the correct
     * {@link net.groboclown.p4.server.api.P4CommandRunner.ServerResultException},
     * or just return the caller's output if there was no error.
     *
     * @param c callable that's processing the Perforce server.
     * @param <R> return type
     * @return the processed value
     * @throws P4CommandRunner.ServerResultException translated exception
     */
    @Nullable
    public final <R> R handle(@NotNull P4ServerName config, @NotNull Callable<R> c)
            throws P4CommandRunner.ServerResultException {
        return handleConnection(new ConnectionInfo(config), c);
    }


    /**
     * Called when the {@link IOptionsServer#disconnect()} throws
     * an exception.  It doesn't affect the overall error state,
     * but it should be at least noted as happening.
     *
     * @param e error
     */
    public abstract void handleOnDisconnectError(@NotNull ConnectionException e);


    /**
     * Called when the {@link IOptionsServer#disconnect()} throws
     * an exception.  It doesn't affect the overall error state,
     * but it should be at least noted as happening.
     *
     * @param e error
     */
    public abstract void handleOnDisconnectError(@NotNull AccessException e);

    @NotNull
    protected abstract P4CommandRunner.ServerResultException handleException(
            @NotNull ConnectionInfo info, @NotNull Exception e);
    @NotNull
    protected abstract P4CommandRunner.ServerResultException handleError(
            @NotNull ConnectionInfo info, @NotNull Error e);


    @NotNull
    protected final P4CommandRunner.ServerResultException createServerResultException(@Nullable Exception e,
            @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String message,
            @NotNull P4CommandRunner.ErrorCategory category) {
        return new P4CommandRunner.ServerResultException(
                new ResultErrorImpl(message, category), e);
    }

    @NotNull
    protected final P4CommandRunner.ServerResultException createServerResultException(@Nullable Error e,
            @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String message,
            @NotNull P4CommandRunner.ErrorCategory category) {
        return new P4CommandRunner.ServerResultException(
                new ResultErrorImpl(message, category), e);
    }



    @Nullable
    private <R> R handleConnection(@NotNull ConnectionInfo info, @NotNull Callable<R> c)
            throws P4CommandRunner.ServerResultException {
        try {
            return c.call();
        } catch (Exception e) {
            throw handleException(info, e);
        } catch (VirtualMachineError | ThreadDeath e) {
            throw e;
        } catch (Error e) {
            throw handleError(info, e);
        }
    }


    protected static class ConnectionInfo {
        private final ClientConfig clientConfig;
        private final ServerConfig serverConfig;
        private final P4ServerName serverName;

        ConnectionInfo(@NotNull ClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            serverConfig = clientConfig.getServerConfig();
            serverName = clientConfig.getServerConfig().getServerName();
        }

        ConnectionInfo(@NotNull ServerConfig config) {
            this.clientConfig = null;
            this.serverConfig = config;
            this.serverName = config.getServerName();
        }

        ConnectionInfo(@NotNull P4ServerName config) {
            this.clientConfig = null;
            this.serverConfig = null;
            this.serverName = config;
        }

        public boolean isClientConfig() {
            return clientConfig != null;
        }

        public ClientConfig getClientConfig() {
            return clientConfig;
        }

        public boolean isStrictlyServerConfig() {
            return serverConfig != null && clientConfig == null;
        }

        public boolean hasServerConfig() {
            return serverConfig != null;
        }

        public ServerConfig getServerConfig() {
            return serverConfig;
        }

        public boolean isByName() {
            return serverConfig == null && clientConfig == null;
        }

        public P4ServerName getServerName() {
            return serverName;
        }

        @Override
        public String toString() {
            if (clientConfig != null) {
                return "client:" + clientConfig.toString();
            }
            if (serverConfig != null) {
                return "server:" + serverConfig.toString();
            }
            return "name:" + serverName.toString();
        }
    }


    private static class ResultErrorImpl implements P4CommandRunner.ResultError {
        private final String msg;
        private final P4CommandRunner.ErrorCategory category;

        private ResultErrorImpl(@Nullable String msg, @NotNull P4CommandRunner.ErrorCategory category) {
            this.msg = msg;
            this.category = category;
        }

        @NotNull
        @Override
        public P4CommandRunner.ErrorCategory getCategory() {
            return category;
        }

        @Nls
        @NotNull
        @Override
        public Optional<String> getMessage() {
            return Optional.ofNullable(msg);
        }
    }
}
