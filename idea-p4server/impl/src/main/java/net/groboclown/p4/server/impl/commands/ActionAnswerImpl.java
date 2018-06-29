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
import net.groboclown.p4.server.api.async.Answer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Immutable
public class ActionAnswerImpl<S> implements P4CommandRunner.ActionAnswer<S> {
    private final Answer<S> answer;

    public ActionAnswerImpl(@NotNull Answer<S> answer) {
        this.answer = answer;
    }

    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenOffline(Runnable r) {
        return this;
    }

    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenServerError(Consumer<P4CommandRunner.ServerResultException> c) {
        answer.whenFailed(c);
        return this;
    }

    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenCompleted(Consumer<S> c) {
        answer.whenCompleted(c);
        return this;
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.ActionAnswer<T> mapAction(Function<S, T> fun) {
        return new ActionAnswerImpl<>(answer.map(fun));
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.QueryAnswer<T> mapQuery(Function<S, T> fun) {
        return new QueryAnswerImpl<>(answer.map(fun));
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.ActionAnswer<T> mapActionAsync(Function<S, P4CommandRunner.ActionAnswer<T>> fun) {
        return new ActionAnswerImpl<>(answer.futureMap((s, sink) ->
                fun.apply(s)
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)
        ));
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.QueryAnswer<T> mapQueryAsync(Function<S, P4CommandRunner.QueryAnswer<T>> fun) {
        return new QueryAnswerImpl<>(answer.futureMap((s, sink) ->
                fun.apply(s)
                        .whenCompleted(sink::resolve)
                        .whenServerError(sink::reject)));
    }

    @Override
    public void after(Runnable fun) {
        answer.after(fun);
    }

    @Override
    public boolean waitForCompletion(int timeout, TimeUnit unit)
            throws InterruptedException {
        return answer.blockingWait(timeout, unit);
    }

    @Nullable
    @Override
    public S blockingGet(int timeout, TimeUnit unit)
            throws InterruptedException, CancellationException, P4CommandRunner.ServerResultException {
        return Answer.blockingGet(answer, timeout, unit);
    }
}
