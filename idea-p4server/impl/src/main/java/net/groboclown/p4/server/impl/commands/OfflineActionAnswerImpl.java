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

package net.groboclown.p4.server.impl.commands;

import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Immutable
public class OfflineActionAnswerImpl<S> implements P4CommandRunner.ActionAnswer<S> {
    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenOffline(Runnable r) {
        r.run();
        return this;
    }

    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenServerError(Consumer<P4CommandRunner.ServerResultException> c) {
        return this;
    }

    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenCompleted(Consumer<S> c) {
        return this;
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.ActionAnswer<T> mapAction(Function<S, T> fun) {
        return new OfflineActionAnswerImpl<>();
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.QueryAnswer<T> mapQuery(Function<S, T> fun) {
        return new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError());
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.ActionAnswer<T> mapActionAsync(Function<S, P4CommandRunner.ActionAnswer<T>> fun) {
        return new OfflineActionAnswerImpl<>();
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.QueryAnswer<T> mapQueryAsync(Function<S, P4CommandRunner.QueryAnswer<T>> fun) {
        return new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError());
    }

    @Override
    public boolean waitForCompletion(int timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public S blockingGet(int timeout, TimeUnit unit)
            throws InterruptedException, CancellationException, P4CommandRunner.ServerResultException {
        // offline, so return null.
        return null;
    }
}
