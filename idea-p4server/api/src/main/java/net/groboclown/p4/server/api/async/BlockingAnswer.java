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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BlockingAnswer<S> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private S result;
    private P4CommandRunner.ServerResultException error;

    public static <S> BlockingAnswer<S> createBlockFor(@NotNull Answer<S> answer) {
        return new BlockingAnswer<>(answer);
    }

    public static <S> S blockingGet(@NotNull Answer<S> answer, int timeout, @NotNull TimeUnit unit)
            throws InterruptedException, CancellationException, P4CommandRunner.ServerResultException {
        return new BlockingAnswer<>(answer).blockingGet(timeout, unit);
    }

    public static <S> S defaultBlockingGet(@NotNull Answer<S> answer, int timeout, @NotNull TimeUnit unit,
            Supplier<S> defaultValueOnTimeout)
            throws P4CommandRunner.ServerResultException {
        try {
            return new BlockingAnswer<>(answer).blockingGet(timeout, unit);
        } catch (InterruptedException | CancellationException e) {
            return defaultValueOnTimeout.get();
        }
    }


    private BlockingAnswer(@NotNull Answer<S> answer) {
        answer.whenCompleted((s) -> {
            result = s;
            latch.countDown();
        })
        .whenFailed((t) -> {
            error = t;
            latch.countDown();
        });
    }


    /**
     *
     * @param timeout time to wait
     * @param unit time unit of the time to wait
     * @return the result of the answer, possibly null.
     * @throws InterruptedException if the thread was interrupted while waiting
     * @throws CancellationException if the timeout was exceeded
     * @throws P4CommandRunner.ServerResultException the failure of the answer.
     */
    @Nullable
    public S blockingGet(int timeout, @NotNull TimeUnit unit) throws InterruptedException, CancellationException,
            P4CommandRunner.ServerResultException {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            throw new IllegalStateException("Cannot call blocking get from within the EDT.");
        }

        if (! latch.await(timeout, unit)) {
            throw new CancellationException("Timed out after " + timeout + " " + unit.toString().toLowerCase());
        }
        if (error != null) {
            throw error;
        }
        return result;
    }
}
