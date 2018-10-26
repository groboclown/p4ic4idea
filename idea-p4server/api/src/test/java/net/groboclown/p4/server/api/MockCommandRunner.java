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
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

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
    public <R extends ServerResult> QueryAnswer<R> query(@NotNull ServerConfig config, @NotNull ServerQuery<R> query) {
        assertTrue(
                serverQueryResultMap.containsKey(query.getCmd()),
                "Unexpected query cmd " + query.getCmd()
        );
        return new MockQueryAnswer<>(answerQuery(config, query, serverQueryResultMap.get(query.getCmd())), null);
    }

    @SuppressWarnings("unchecked")
    private <R extends ServerResult> R answerQuery(ServerConfig config, ServerQuery query, ServerQueryAnswer answer) {
        return (R) answer.answer(config, query);
    }

    @NotNull
    @Override
    public <R extends ClientResult> QueryAnswer<R> query(@NotNull ClientConfig config, @NotNull ClientQuery<R> query) {
        assertTrue(
                clientQueryResultMap.containsKey(query.getCmd()),
                "Unexpected query cmd " + query.getCmd()
        );
        return new MockQueryAnswer<>(answerQuery(config, query, clientQueryResultMap.get(query.getCmd())), null);
    }

    @SuppressWarnings("unchecked")
    private <R extends ClientResult> R answerQuery(ClientConfig config, ClientQuery query, ClientQueryAnswer answer) {
        return (R) answer.answer(config, query);
    }

    @NotNull
    @Override
    public <R extends ServerResult> ActionAnswer<R> perform(@NotNull ServerConfig config,
            @NotNull ServerAction<R> action) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ClientResult> ActionAnswer<R> perform(@NotNull ClientConfig config,
            @NotNull ClientAction<R> action) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ServerNameResult> QueryAnswer<R> query(@NotNull P4ServerName name,
            @NotNull ServerNameQuery<R> query) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }

    @NotNull
    @Override
    public ActionAnswer<Void> sendCachedPendingRequests(@NotNull ClientConfig clientConfig) {
        fail("Should not be called");
        throw new IllegalArgumentException("");
    }


    static class MockQueryAnswer<R> implements QueryAnswer<R> {
        private final R result;
        private final ServerResultException error;

        MockQueryAnswer(R result, ServerResultException error) {
            this.result = result;
            this.error = error;
        }

        @NotNull
        @Override
        public QueryAnswer<R> whenCompleted(Consumer<R> c) {
            if (error == null) {
                c.accept(result);
            }
            return this;
        }

        @NotNull
        @Override
        public QueryAnswer<R> whenServerError(Consumer<ServerResultException> c) {
            if (error != null) {
                c.accept(error);
            }
            return this;
        }

        @NotNull
        @Override
        public QueryAnswer<R> whenAnyState(Runnable r) {
            r.run();
            return this;
        }

        @NotNull
        @Override
        public <T> ActionAnswer<T> mapAction(Function<R, T> fun) {
            if (error == null) {
                return new MockActionAnswer<T>(false, fun.apply(result), null);
            }
            return new MockActionAnswer<T>(false, null, error);
        }

        @NotNull
        @Override
        public <T> QueryAnswer<T> mapQuery(Function<R, T> fun) {
            if (error == null) {
                return new MockQueryAnswer<>(fun.apply(result), null);
            }
            return new MockQueryAnswer<>(null, error);
        }

        @NotNull
        @Override
        public <T> ActionAnswer<T> mapActionAsync(Function<R, ActionAnswer<T>> fun) {
            if (error == null) {
                return fun.apply(result);
            }
            return new MockActionAnswer<>(false, null, error);
        }

        @NotNull
        @Override
        public <T> QueryAnswer<T> mapQueryAsync(Function<R, QueryAnswer<T>> fun) {
            if (error == null) {
                return fun.apply(result);
            }
            return new MockQueryAnswer<>(null, error);
        }

        @Override
        public boolean waitForCompletion(int timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public R blockingGet(int timeout, TimeUnit unit)
                throws InterruptedException, CancellationException, ServerResultException {
            if (error == null) {
                return result;
            }
            throw error;
        }
    }

    private static class MockActionAnswer<S> implements ActionAnswer<S> {
        private final boolean offline;
        private final S result;
        private final ServerResultException error;

        private MockActionAnswer(boolean offline, S result, ServerResultException error) {
            this.offline = offline;
            this.result = result;
            this.error = error;
        }

        @NotNull
        @Override
        public ActionAnswer<S> whenOffline(Runnable r) {
            if (offline) {
                r.run();
            }
            return this;
        }

        @NotNull
        @Override
        public ActionAnswer<S> whenServerError(Consumer<ServerResultException> c) {
            if (error != null) {
                c.accept(error);
            }
            return this;
        }

        @NotNull
        @Override
        public ActionAnswer<S> whenCompleted(Consumer<S> c) {
            if (!offline && error == null) {
                c.accept(result);
            }
            return this;
        }

        @NotNull
        @Override
        public ActionAnswer<S> whenAnyState(Runnable r) {
            r.run();
            return this;
        }

        @NotNull
        @Override
        public <T> ActionAnswer<T> mapAction(Function<S, T> fun) {
            if (!offline && error == null) {
                return new MockActionAnswer<>(false, fun.apply(result), null);
            }
            return new MockActionAnswer<>(false, null, error);
        }

        @NotNull
        @Override
        public <T> QueryAnswer<T> mapQuery(Function<S, T> fun) {
            if (!offline && error == null) {
                return new MockQueryAnswer<>(fun.apply(result), null);
            }
            return new MockQueryAnswer<>(null, error);
        }

        @NotNull
        @Override
        public <T> ActionAnswer<T> mapActionAsync(Function<S, ActionAnswer<T>> fun) {
            if (!offline && error == null) {
                return fun.apply(result);
            }
            return new MockActionAnswer<>(false, null, error);
        }

        @NotNull
        @Override
        public <T> QueryAnswer<T> mapQueryAsync(Function<S, QueryAnswer<T>> fun) {
            if (!offline && error == null) {
                return fun.apply(result);
            }
            return new MockQueryAnswer<>(null, error);
        }

        @Override
        public boolean waitForCompletion(int timeout, TimeUnit unit) {
            return true;
        }

        @Nullable
        @Override
        public S blockingGet(int timeout, TimeUnit unit)
                throws InterruptedException, CancellationException, ServerResultException {
            if (error == null) {
                return result;
            }
            throw error;
        }
    }
}
