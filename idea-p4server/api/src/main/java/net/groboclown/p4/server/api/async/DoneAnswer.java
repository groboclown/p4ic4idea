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

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class DoneAnswer<S> implements Answer<S> {
    private final P4CommandRunner.ServerResultException error;
    private final S result;

    public static <S> Answer<S> resolve(@Nullable S s) {
        return new DoneAnswer<>(s, null);
    }

    public static <S> Answer<S> reject(@NotNull P4CommandRunner.ServerResultException error) {
        return new DoneAnswer<>(null, error);
    }

    private DoneAnswer(S result, P4CommandRunner.ServerResultException error) {
        this.result = result;
        this.error = error;
    }

    @NotNull
    @Override
    public Answer<S> whenCompleted(@NotNull Consumer<S> c) {
        if (error == null) {
            c.accept(result);
        }
        return this;
    }

    @NotNull
    @Override
    public Answer<S> whenFailed(@NotNull Consumer<P4CommandRunner.ServerResultException> c) {
        if (error != null) {
            c.accept(error);
        }
        return this;
    }

    @NotNull
    @Override
    public <T> Answer<T> map(@NotNull Function<S, T> fun) {
        if (error != null) {
            return new DoneAnswer<>(null, error);
        }
        return new DoneAnswer<>(fun.apply(result), null);
    }

    @NotNull
    @Override
    public <T> Answer<T> mapAsync(@NotNull Function<S, Answer<T>> fun) {
        if (error != null) {
            return new DoneAnswer<>(null, error);
        }
        return fun.apply(result);
    }

    @NotNull
    @Override
    public <T> Answer<T> futureMap(@NotNull BiConsumer<S, AnswerSink<T>> func) {
        AsyncAnswer<T> ret = new AsyncAnswer<>();
        func.accept(result, ret);
        return ret;
    }

    @Override
    public void after(@NotNull Runnable r) {
        r.run();
    }

    @Override
    public boolean blockingWait(int timeout, TimeUnit unit) {
        return true;
    }
}
