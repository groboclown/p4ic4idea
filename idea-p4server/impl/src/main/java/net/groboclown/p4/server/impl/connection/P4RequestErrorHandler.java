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

package net.groboclown.p4.server.impl.connection;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Handles errors generated during the processing of a request against a
 * Perforce server.  Errors should be pushed to the message bus where
 * appropriate.
 */
public abstract class P4RequestErrorHandler {
    /**
     * Translates any Perforce server interaction error into the correct
     * {@link net.groboclown.p4.server.api.P4CommandRunner.ServerResultException},
     * or just return the caller's output if there was no error.
     *
     * @param c callable that's processing the Perforce server.
     * @param <R> return type
     * @return the processed value
     * @throws P4CommandRunner.ServerResultException translated exception
     */
    @Nullable
    public final <R> R handle(@NotNull Callable<R> c)
            throws P4CommandRunner.ServerResultException {
        try {
            return c.call();
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    /**
     * Similar to {@link #handle(Callable)}, but encases any returned value
     * inside a {@link Promise}.  Errors are wrapped in a rejected promise,
     * and normal return values are in a resolved promise.
     *
     * @param c callable that's processing the Perforce server.
     * @param <R> return type
     * @return the processed value or error, wrapped as a promise.
     */
    @NotNull
    public final <R> Promise<R> handleAsync(@NotNull Callable<R> c) {
        try {
            R ret = handle(c);
            return Promise.resolve(ret);
        } catch (P4CommandRunner.ServerResultException e) {
            return Promises.rejectedPromise(e);
        }
    }


    /**
     * Called when the {@link IOptionsServer#disconnect()} throws
     * an exception.  It doesn't affect the overall error state,
     * but it should be at least noted as happening.
     *
     * @param e error
     */
    public abstract void handleOnDisconnectError(@NotNull ConnectionException e);


    /**
     * Called when the {@link IOptionsServer#disconnect()} throws
     * an exception.  It doesn't affect the overall error state,
     * but it should be at least noted as happening.
     *
     * @param e error
     */
    public abstract void handleOnDisconnectError(@NotNull AccessException e);


    protected abstract P4CommandRunner.ServerResultException handleException(@NotNull Exception e);

    protected final P4CommandRunner.ServerResultException createServerResultException(@Nullable Exception e,
            @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String message,
            @NotNull P4CommandRunner.ErrorCategory category) {
        return new P4CommandRunner.ServerResultException(
                new ResultErrorImpl(message, category), e);
    }


    private static class ResultErrorImpl implements P4CommandRunner.ResultError {
        private final String msg;
        private final P4CommandRunner.ErrorCategory category;

        private ResultErrorImpl(@Nullable String msg, @NotNull P4CommandRunner.ErrorCategory category) {
            // FIXME make a better message
            this.msg = msg;
            this.category = category;
        }

        @NotNull
        @Override
        public P4CommandRunner.ErrorCategory getCategory() {
            return category;
        }

        @Nls
        @NotNull
        @Override
        public Optional<String> getMessage() {
            return Optional.ofNullable(msg);
        }
    }
}
