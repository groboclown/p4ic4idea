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

import com.intellij.openapi.application.ApplicationManager;
import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple underlying implementation of the {@link net.groboclown.p4.server.api.P4CommandRunner.QueryAnswer}
 * and {@link net.groboclown.p4.server.api.P4CommandRunner.ActionAnswer} interfaces
 * that handles the blocking and thread off-loading.
 *
 *
 * @param <S>
 */
class AsyncAnswer<S> implements Answer<S>, AnswerSink<S> {
    private final Object sync = new Object();
    private final List<Consumer<S>> doneListeners = new ArrayList<>();
    private final List<Consumer<P4CommandRunner.ServerResultException>> errListeners = new ArrayList<>();
    private final CountDownLatch completedSignal = new CountDownLatch(1);
    private S result;
    private P4CommandRunner.ServerResultException error;
    private boolean completed = false;

    public static <S> Answer<S> background(Consumer<AnswerSink<S>> c) {
        AsyncAnswer<S> ret = new AsyncAnswer<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> c.accept(ret));
        return ret;
    }

    public void resolve(@Nullable S result) {
        synchronized (sync) {
            if (completed) {
                throw new IllegalStateException("already completed");
            }
            this.result = result;
            completed = true;
        }
        completedSignal.countDown();

        // already completed, so the list of listeners will never change from
        // another call.
        for (Consumer<S> listener : doneListeners) {
            listener.accept(result);
        }
        doneListeners.clear();
        errListeners.clear();
    }

    public void reject(@NotNull P4CommandRunner.ServerResultException error) {
        synchronized (sync) {
            if (completed) {
                throw new IllegalStateException("already completed");
            }
            this.error = error;
            completed = true;
        }
        completedSignal.countDown();

        // already completed, so the list of listeners will never change from
        // another call.
        for (Consumer<P4CommandRunner.ServerResultException> listener : errListeners) {
            listener.accept(error);
        }
        doneListeners.clear();
        errListeners.clear();
    }

    @NotNull
    @Override
    public Answer<S> whenCompleted(@NotNull Consumer<S> c) {
        synchronized (sync) {
            if (!completed) {
                doneListeners.add(c);
                return this;
            }
        }
        if (error == null) {
            c.accept(result);
        }
        return this;
    }

    @NotNull
    @Override
    public Answer<S> whenFailed(@NotNull Consumer<P4CommandRunner.ServerResultException> c) {
        synchronized (sync) {
            if (!completed) {
                errListeners.add(c);
                return this;
            }
        }
        if (error != null) {
            c.accept(error);
        }
        return this;
    }

    @NotNull
    @Override
    public <T> Answer<T> map(@NotNull Function<S, T> fun) {
        synchronized (sync) {
            if (!completed) {
                Answer<T> ret = new AsyncAnswer<>();
                doneListeners.add((s) -> ((AsyncAnswer<T>) ret).resolve(fun.apply(s)));
                return ret;
            }
        }
        if (error != null) {
            return DoneAnswer.reject(error);
        }
        return DoneAnswer.resolve(fun.apply(result));
    }

    @NotNull
    @Override
    public <T> Answer<T> mapAsync(@NotNull Function<S, Answer<T>> fun) {
        synchronized (sync) {
            if (!completed) {
                AsyncAnswer<T> ret = new AsyncAnswer<>();
                doneListeners.add((s) ->
                    fun.apply(s)
                        .whenCompleted(ret::resolve)
                        .whenFailed(ret::reject));
                return ret;
            }
        }
        if (error != null) {
            return DoneAnswer.reject(error);
        }
        return fun.apply(result);
    }

    @NotNull
    @Override
    public <T> Answer<T> futureMap(@NotNull BiConsumer<S, AnswerSink<T>> func) {
        AsyncAnswer<T> ret = new AsyncAnswer<>();
        whenCompleted((s) -> func.accept(s, ret));
        return ret;
    }

    @Override
    public boolean blockingWait(int timeout, TimeUnit unit)
            throws InterruptedException {
        synchronized (sync) {
            if (completed) {
                return true;
            }
        }
        return completedSignal.await(timeout, unit);
    }
}
