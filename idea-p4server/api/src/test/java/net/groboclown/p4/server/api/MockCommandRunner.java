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

package net.groboclown.p4.server.api;

import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MockCommandRunner
        implements P4CommandRunner {

    public interface ServerQueryAnswer<R extends ServerResult, Q extends ServerQuery<R>> {
        R answer(ServerConfig config, Q query);
    }
    public interface ClientQueryAnswer<R extends ClientResult, Q extends ClientQuery<R>> {
        R answer(ClientConfig config, Q query);
    }



    private final Map<ServerQueryCmd, ServerQueryAnswer<?, ?>> serverQueryResultMap = new HashMap<>();
    private final Map<ClientQueryCmd, ClientQueryAnswer<?, ?>> clientQueryResultMap = new HashMap<>();

    public <R extends ServerResult, Q extends ServerQuery<R>>
    void setResult(@Nonnull ServerQueryCmd cmd, @Nonnull ServerQueryAnswer<R, Q> answer) {
        serverQueryResultMap.put(cmd, answer);
    }

    public <R extends ClientResult, Q extends ClientQuery<R>>
    void setResult(@Nonnull ClientQueryCmd cmd, @Nonnull ClientQueryAnswer<R, Q> answer) {
        clientQueryResultMap.put(cmd, answer);
    }


    @NotNull
    @Override
    public <R extends ServerResult> Promise<R> query(@NotNull ServerConfig config, @NotNull ServerQuery<R> query) {
        assertTrue(
                serverQueryResultMap.containsKey(query.getCmd()),
                "Unexpected query cmd " + query.getCmd()
        );
        return Promise.resolve(answerQuery(config, query, serverQueryResultMap.get(query.getCmd())));
    }

    @SuppressWarnings("unchecked")
    private <R extends ServerResult>
    R answerQuery(ServerConfig config, ServerQuery query, ServerQueryAnswer answer) {
        return (R) answer.answer(config, query);
    }

    @NotNull
    @Override
    public <R extends ClientResult> Promise<R> query(@NotNull ClientConfig config, @NotNull ClientQuery<R> query) {
        assertTrue(
                clientQueryResultMap.containsKey(query.getCmd()),
                "Unexpected query cmd " + query.getCmd()
        );
        return Promise.resolve(answerQuery(config, query, clientQueryResultMap.get(query.getCmd())));
    }

    @SuppressWarnings("unchecked")
    private <R extends ClientResult>
    R answerQuery(ClientConfig config, ClientQuery query, ClientQueryAnswer answer) {
        return (R) answer.answer(config, query);
    }

    @NotNull
    @Override
    public <R extends ServerResult> Promise<R> perform(@NotNull ServerConfig config,
            @NotNull ServerAction<R> action) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ClientResult> Promise<R> perform(@NotNull ClientConfig config,
            @NotNull ClientAction<R> action) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ServerNameResult> Promise<R> query(@NotNull P4ServerName name,
            @NotNull ServerNameQuery<R> query) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query)
            throws ServerResultException {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query)
            throws ServerResultException {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query)
            throws ServerResultException {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query)
            throws ServerResultException {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }
}
