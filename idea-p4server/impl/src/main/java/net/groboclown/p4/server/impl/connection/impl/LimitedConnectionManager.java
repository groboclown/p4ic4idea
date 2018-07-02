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

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class LimitedConnectionManager implements ConnectionManager {
    private final Semaphore restriction;
    private final ConnectionManager proxy;

    public LimitedConnectionManager(@NotNull ConnectionManager proxy, int maximumConcurrentCount) {
        this.restriction = new Semaphore(maximumConcurrentCount);
        this.proxy = proxy;
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
        Answer<R> ret = Answer.background((sink) -> {
            try {
                // TODO rethink adding in the maximum wait time.
                restriction.acquire();
                sink.resolve(null);
            } catch (InterruptedException e) {
                sink.reject(createServerError(e));
            }
        })
        .mapAsync((x) -> fun.get());
        ret.after(restriction::release);
        return ret;
    }

    private P4CommandRunner.ServerResultException createServerError(InterruptedException e) {
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
