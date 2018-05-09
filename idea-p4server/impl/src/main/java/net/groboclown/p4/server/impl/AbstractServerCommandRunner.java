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

package net.groboclown.p4.server.impl;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import java.util.HashMap;
import java.util.Map;

// FIXME make this NOT implement P4CommandRunner, and instead be its
// own thing that doesn't need an underlying cache.  That also means
// that implementations don't need to worry about the messages.
public abstract class AbstractServerCommandRunner implements P4CommandRunner {
    public interface ServerActionRunner<R extends ServerResult> {
        Promise<R> perform(@NotNull ServerConfig config, @NotNull ServerAction<R> action);
    }

    public interface ClientActionRunner<R extends ClientResult> {
        Promise<R> perform(@NotNull ClientConfig config, @NotNull ClientAction<R> action);
    }

    public interface ServerQueryRunner<R extends ServerResult> {
        Promise<R> query(@NotNull ServerConfig config, @NotNull ServerQuery<R> query);
    }

    public interface ClientQueryRunner<R extends ClientResult> {
        Promise<R> query(@NotNull ClientConfig config, @NotNull ClientQuery<R> query);
    }

    public interface ServerNameQueryRunner<R extends ServerNameResult> {
        Promise<R> query(@NotNull P4ServerName name, @NotNull ServerNameQuery<R> query);
    }

    public interface SyncCacheServerQueryRunner<R extends ServerResult> {
        R syncCachedQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query)
                throws ServerResultException;
    }

    public interface SyncCacheClientQueryRunner<R extends ClientResult> {
        R syncCachedQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query)
                throws ServerResultException;
    }

    public interface SyncServerQueryRunner<R extends ServerResult> {
        FutureResult<R> syncQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query)
            throws ServerResultException;
    }

    public interface SyncClientQueryRunner<R extends ClientResult> {
        FutureResult<R> syncQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query)
            throws ServerResultException;
    }


    private final Map<ServerActionCmd, ServerActionRunner<?>> serverActionRunners = new HashMap<>();
    private final Map<ClientActionCmd, ClientActionRunner<?>> clientActionRunners = new HashMap<>();
    private final Map<ServerQueryCmd, ServerQueryRunner<?>> serverQueryRunners = new HashMap<>();
    private final Map<ClientQueryCmd, ClientQueryRunner<?>> clientQueryRunners = new HashMap<>();
    private final Map<ServerNameQueryCmd, ServerNameQueryRunner<?>> serverNameQueryRunners = new HashMap<>();
    private final Map<SyncServerQueryCmd, SyncCacheServerQueryRunner<?>> syncCacheServerQueryRunners = new HashMap<>();
    private final Map<SyncClientQueryCmd, SyncCacheClientQueryRunner<?>> syncCacheClientQueryRunners = new HashMap<>();
    private final Map<SyncServerQueryCmd, SyncServerQueryRunner<?>> syncServerQueryRunners = new HashMap<>();
    private final Map<SyncClientQueryCmd, SyncClientQueryRunner<?>> syncClientQueryRunners = new HashMap<>();


    protected void register(@NotNull ServerActionCmd cmd, @NotNull ServerActionRunner<?> runner) {
        serverActionRunners.put(cmd, runner);
    }

    protected void register(@NotNull ClientActionCmd cmd, @NotNull ClientActionRunner<?> runner) {
        clientActionRunners.put(cmd, runner);
    }

    protected void register(@NotNull ServerQueryCmd cmd, @NotNull ServerQueryRunner<?> runner) {
        serverQueryRunners.put(cmd, runner);
    }

    protected void register(@NotNull ClientQueryCmd cmd, @NotNull ClientQueryRunner<?> runner) {
        clientQueryRunners.put(cmd, runner);
    }

    protected void register(@NotNull ServerNameQueryCmd cmd, @NotNull ServerNameQueryRunner<?> runner) {
        serverNameQueryRunners.put(cmd, runner);
    }

    protected void register(@NotNull SyncServerQueryCmd cmd, @NotNull SyncCacheServerQueryRunner<?> runner) {
        syncCacheServerQueryRunners.put(cmd, runner);
    }

    protected void register(@NotNull SyncClientQueryCmd cmd, @NotNull SyncCacheClientQueryRunner<?> runner) {
        syncCacheClientQueryRunners.put(cmd, runner);
    }

    protected void register(@NotNull SyncServerQueryCmd cmd, @NotNull SyncServerQueryRunner<?> runner) {
        syncServerQueryRunners.put(cmd, runner);
    }

    protected void register(@NotNull SyncClientQueryCmd cmd, @NotNull SyncClientQueryRunner<?> runner) {
        syncClientQueryRunners.put(cmd, runner);
    }


    /**
     * Force all connections to close, if any are open in a pool.
     *
     * @param config server configuration's connections to close.
     */
    public abstract void disconnect(@NotNull ServerConfig config);


    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> Promise<R> perform(@NotNull ServerConfig config, @NotNull ServerAction<R> action) {
        ServerActionRunner<?> runner = serverActionRunners.get(action.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + action.getCmd());
        }
        return ((ServerActionRunner<R>) runner).perform(config, action);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> Promise<R> perform(@NotNull ClientConfig config, @NotNull ClientAction<R> action) {
        ClientActionRunner<?> runner = clientActionRunners.get(action.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + action.getCmd());
        }
        return ((ClientActionRunner<R>) runner).perform(config, action);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> Promise<R> query(@NotNull ServerConfig config, @NotNull ServerQuery<R> query) {
        ServerQueryRunner<?> runner = serverQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((ServerQueryRunner<R>) runner).query(config, query);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> Promise<R> query(@NotNull ClientConfig config, @NotNull ClientQuery<R> query) {
        ClientQueryRunner<?> runner = clientQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((ClientQueryRunner<R>) runner).query(config, query);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerNameResult> Promise<R> query(@NotNull P4ServerName name,
            @NotNull ServerNameQuery<R> query) {
        ServerNameQueryRunner<?> runner = serverNameQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((ServerNameQueryRunner<R>) runner).query(name, query);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query)
            throws ServerResultException {
        SyncCacheServerQueryRunner<?> runner = syncCacheServerQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((SyncCacheServerQueryRunner<R>) runner).syncCachedQuery(config, query);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query)
            throws ServerResultException {
        SyncCacheClientQueryRunner<?> runner = syncCacheClientQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((SyncCacheClientQueryRunner<R>) runner).syncCachedQuery(config, query);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query)
            throws ServerResultException {
        SyncServerQueryRunner<?> runner = syncServerQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((SyncServerQueryRunner<R>) runner).syncQuery(config, query);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query)
            throws ServerResultException {
        SyncClientQueryRunner<?> runner = syncClientQueryRunners.get(query.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + query.getCmd());
        }
        return ((SyncClientQueryRunner<R>) runner).syncQuery(config, query);
    }
}
