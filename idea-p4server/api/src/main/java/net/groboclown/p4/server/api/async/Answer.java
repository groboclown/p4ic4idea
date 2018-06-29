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

package net.groboclown.p4.server.api.async;

import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Answer<S> {
    static <S> Answer<S> background(@NotNull Consumer<AnswerSink<S>> c) {
        return AsyncAnswer.background(c);
    }

    static <S> Answer<S> forPromise(@NotNull Promise<S> p) {
        AsyncAnswer<S> ret = new AsyncAnswer<>();
        p
                .then((s) -> {
                    ret.resolve(s);
                    return s;
                })
                .rejected((t) -> {
                    if (t instanceof P4CommandRunner.ServerResultException) {
                        ret.reject((P4CommandRunner.ServerResultException) t);
                    }
                });
        return ret;
    }

    static <S> Answer<S> resolve(@Nullable S s) {
        return DoneAnswer.resolve(s);
    }

    static <S> Answer<S> reject(@NotNull P4CommandRunner.ServerResultException e) {
        return DoneAnswer.reject(e);
    }

    static <S> S blockingGet(@NotNull Answer<S> answer, int timeout, TimeUnit unit)
            throws InterruptedException, CancellationException, P4CommandRunner.ServerResultException {
        return BlockingAnswer.blockingGet(answer, timeout, unit);
    }


    @NotNull
    Answer<S> whenCompleted(@NotNull Consumer<S> c);
    @NotNull
    Answer<S> whenFailed(@NotNull Consumer<P4CommandRunner.ServerResultException> c);

    @NotNull
    <T> Answer<T> map(@NotNull Function<S, T> fun);

    @NotNull
    <T> Answer<T> mapAsync(@NotNull Function<S, Answer<T>> fun);

    @NotNull
    <T> Answer<T> futureMap(@NotNull BiConsumer<S, AnswerSink<T>> func);

    void after(@NotNull Runnable r);

    /**
     *
     * @param timeout time to wait
     * @param unit unit of the time to wait
     * @return true if the wait completed, false if it timed out.
     */
    boolean blockingWait(int timeout, TimeUnit unit) throws InterruptedException;
}
