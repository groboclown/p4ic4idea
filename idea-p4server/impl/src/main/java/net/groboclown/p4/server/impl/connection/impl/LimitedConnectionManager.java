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

package net.groboclown.p4.server.impl.connection.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4Func;
import net.groboclown.p4.server.impl.util.TraceableSemaphore;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LimitedConnectionManager implements ConnectionManager {
    private static final Logger LOG = Logger.getInstance(LimitedConnectionManager.class);

    private final TraceableSemaphore restriction;
    private final ConnectionManager proxy;


    public LimitedConnectionManager(@NotNull ConnectionManager proxy, int maximumConcurrentCount, long timeout,
            TimeUnit timeoutUnit) {
        this.restriction = new TraceableSemaphore("connections", maximumConcurrentCount, timeout, timeoutUnit);
        this.proxy = proxy;
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @NotNull P4Func<IClient, R> fun) {
        return get(() -> proxy.withConnection(config, fun));
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @Nullable File cwd, @NotNull P4Func<IClient, R> fun) {
        return get(() -> proxy.withConnection(config, cwd, fun));
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ServerConfig config, @NotNull P4Func<IOptionsServer, R> fun) {
        return get(() -> proxy.withConnection(config, fun));
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull P4ServerName config, P4Func<IOptionsServer, R> fun) {
        return get(() -> proxy.withConnection(config, fun));
    }

    @Override
    public void disconnect(@NotNull P4ServerName config) {
        proxy.disconnect(config);
    }

    private <R> Answer<R> get(@NotNull Supplier<Answer<R>> fun) {
        final TraceableSemaphore.Requester requester = TraceableSemaphore.createRequest();
        Answer<R> ret = Answer.background((sink) -> {
            try {
                restriction.acquire(requester);
                sink.resolve(null);
            } catch (InterruptedException e) {
                sink.reject(AnswerUtil.createFor(e));
            } catch (CancellationException e) {
                sink.reject(AnswerUtil.createFor(e));
            }
        })
        .mapAsync((x) -> {
            LOG.debug("Start execution for " + requester);
            Answer<R> r = fun.get();
            LOG.debug("End execution for " + requester);
            return r;
        });
        ret.after(() -> {
            // Will only release if the requester acquired the lock.
            LOG.debug("Finalizing execution for " + requester);
            restriction.release(requester);
        });
        return ret;
    }
}
