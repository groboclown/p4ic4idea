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
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class DoneActionAnswer<S> implements P4CommandRunner.ActionAnswer<S> {
    private final S result;

    public DoneActionAnswer(S result) {
        this.result = result;
   }

    @NotNull
    @Override
    public P4CommandRunner.ActionAnswer<S> whenOffline(Runnable r) {
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
        c.accept(result);
        return this;
    }

    @Override
    public void after(Runnable r) {
        r.run();
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.ActionAnswer<T> mapAction(Function<S, T> fun) {
        return new DoneActionAnswer<>(fun.apply(result));
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.QueryAnswer<T> mapQuery(Function<S, T> fun) {
        return new DoneQueryAnswer<>(fun.apply(result));
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.ActionAnswer<T> mapActionAsync(Function<S, P4CommandRunner.ActionAnswer<T>> fun) {
        return fun.apply(result);
    }

    @NotNull
    @Override
    public <T> P4CommandRunner.QueryAnswer<T> mapQueryAsync(Function<S, P4CommandRunner.QueryAnswer<T>> fun) {
        return fun.apply(result);
    }

    @Override
    public boolean waitForCompletion(int timeout, TimeUnit unit) {
        return true;
    }

    @Nullable
    @Override
    public S blockingGet(int timeout, TimeUnit unit)
            throws InterruptedException, CancellationException, P4CommandRunner.ServerResultException {
        return result;
    }
}
