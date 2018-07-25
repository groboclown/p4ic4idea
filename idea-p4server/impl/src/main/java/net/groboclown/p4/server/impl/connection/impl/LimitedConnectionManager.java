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
import net.groboclown.p4.server.impl.connection.ConnectionManager;
import net.groboclown.p4.server.impl.connection.P4Func;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class LimitedConnectionManager implements ConnectionManager {
    private static final Logger LOG = Logger.getInstance(LimitedConnectionManager.class);
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger(0);

    private final Semaphore restriction;
    private final ConnectionManager proxy;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    // TODO THIS IS DEBUGGING STUFF
    private final Set<Integer> waitingIds = Collections.synchronizedSet(new HashSet<>());

    public LimitedConnectionManager(@NotNull ConnectionManager proxy, int maximumConcurrentCount, long timeout,
            TimeUnit timeoutUnit) {
        this.restriction = new Semaphore(maximumConcurrentCount);
        this.proxy = proxy;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @NotNull
    @Override
    public <R> Answer<R> withConnection(@NotNull ClientConfig config, @NotNull P4Func<IClient, R> fun) {
        return get(() -> proxy.withConnection(config, fun));
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
        final int getIndex = REQUEST_COUNT.incrementAndGet();
        final boolean[] captureSuccess = { true };
        Answer<R> ret = Answer.background((sink) -> {
            try {
                // TODO rethink adding in the maximum wait time.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Waiting on index " + getIndex + "; current waiters: " + waitingIds);
                    waitingIds.add(getIndex);
                }
                captureSuccess[0] = restriction.tryAcquire(timeout, timeoutUnit);
                if (!captureSuccess[0]) {
                    throw new CancellationException("Request timed out after " + timeout + " " + timeoutUnit.toString().toLowerCase());
                }
                sink.resolve(null);
            } catch (InterruptedException | CancellationException e) {
                LOG.debug("Failed to acquire lock on " + getIndex);
                captureSuccess[0] = false;
                sink.reject(createServerError(e));
            }
        })
        .mapAsync((x) -> fun.get());
        ret.after(() -> {
            if (LOG.isDebugEnabled()) {
                waitingIds.remove(getIndex);
            }
            if (captureSuccess[0]) {
                restriction.release();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Released wait index for " + getIndex + "; remaining waiters: " + waitingIds);
                }
            }
        });
        return ret;
    }

    private P4CommandRunner.ServerResultException createServerError(Exception e) {
        // TODO use a different way to get the interrupted exception.
        return new P4CommandRunner.ServerResultException(
                new P4CommandRunner.ResultError() {
                    @NotNull
                    @Override
                    public P4CommandRunner.ErrorCategory getCategory() {
                        return P4CommandRunner.ErrorCategory.TIMEOUT;
                    }

                    @Nls
                    @NotNull
                    @Override
                    public Optional<String> getMessage() {
                        return Optional.ofNullable(e.getLocalizedMessage());
                    }
                },
                e);
    }
}
