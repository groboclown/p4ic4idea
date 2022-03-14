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

package net.groboclown.idea.mock;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Condition;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import org.jetbrains.annotations.NotNull;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Sets up the mock ApplicationManager to run threads in a controlled way.
 * Due to the way runners are created when needed, there isn't much use in
 * keeping track of which runners were executed.  Instead, we track the
 * number of things run.
 * <p>
 * Usage: when executing a task in a pooled thread (executeOnPooledThread)
 * or in the EDT as a background thread (invokeLater), the background runner will
 * run in a background thread, and wait for a cyclic barrier to trigger.
 * <p>
 * The general pattern is:
 * <pre>
 *     MockThreadRunner mtr = new MockThreadRunner(idea);
 *     mtr.setNextWaitKey("key1");
 *     runCodeThatInvokesABackgroundThreadRunner();
 *     // the background thread launches in the background, and waits for a "waitFor" signal.
 *     mtr.waitFor("key1");
 *     // the background thread runs, then waits for another "waitFor" signal.
 *     mtr.waitFor("key1");
 *     // the background thread ends
 * </pre>
 * Right now, the {@link Application#invokeLater(Runnable, Condition)} doesn't work as expected; it doesn't
 * care about the condition.  Same with the ModalityState version.
 */
public class MockThreadRunner {
    private final AtomicInteger pooledThreadsRun = new AtomicInteger(0);
    private final AtomicInteger edtLaterRun = new AtomicInteger(0);
    private final AtomicInteger edtWaitRun = new AtomicInteger(0);
    private final List<Exception> callableThrown = Collections.synchronizedList(new ArrayList<>());

    private final BlockingQueue<Runnable> runnerQueue = new LinkedBlockingDeque<>();
    private final ThreadPoolExecutor simulatedRunner = new ThreadPoolExecutor(2, 30, 10L,
            TimeUnit.SECONDS, runnerQueue);

    private final long waitTimeoutSeconds;
    private final Map<String, CyclicBarrier> waitForMap = new HashMap<>();
    private final List<String> queuedKeys = new LinkedList<>();
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicInteger totalStartedCount = new AtomicInteger(0);

    public MockThreadRunner(IdeaLightweightExtension extension) {
        this(extension, 4L);
    }

    private MockThreadRunner(IdeaLightweightExtension extension, long waitTimeSeconds) {
        waitTimeoutSeconds = waitTimeSeconds;
        MockApplication application = extension.getMockApplication();

        BlockingRunner pooledRunner = new BlockingRunner(pooledThreadsRun);
        application.setPooledRunner(pooledRunner);

        application.setEdtLaterRunner(new BlockingRunner(edtLaterRun));

        InthreadRunAnswer edtWaitAnswer = new InthreadRunAnswer(edtWaitRun);
        application.setEdtWaitRunner(edtWaitAnswer);
    }

    public void assertNoExceptions() {
        assertEmpty(callableThrown);
    }

    public void assertAllActionsCompleted() {
        assertEquals(0, activeCount.get(), "Should be no running actions.");
    }

    public void assertTotalStartedActionCount(int expectedCount) {
        final int total = totalStartedCount.get();
        assertEquals(expectedCount, total,
                "Expected " + expectedCount + " actions started, found " + total);
    }

    public List<Exception> getCallableThrown() {
        return callableThrown;
    }

    public int getPooledThreadsRunCount() {
        return pooledThreadsRun.get();
    }

    public int getEdtLaterRunCount() {
        return edtLaterRun.get();
    }

    public int getEdtWaitRunCount() {
        return edtWaitRun.get();
    }

    public void setNextWaitKey(@NotNull String key) {
        synchronized (waitForMap) {
            assertNull(waitForMap.get(key), "Already added wait key [" + key + "]");
            queuedKeys.add(key);
            waitForMap.put(key, new CyclicBarrier(2));
        }
    }

    public void waitFor(@NotNull String key) {
        final CyclicBarrier barrier;
        synchronized (waitForMap) {
            barrier = waitForMap.get(key);
        }
        assertNotNull(barrier, "No barrier key [" + key + "] set");
        try {
            barrier.await(waitTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | BrokenBarrierException e) {
            fail("Runner did not join in time", e);
        }
    }

    public void startAndWaitFor(@NotNull String key) {
        waitFor(key);
        waitFor(key);
    }

    private class BlockingRunner implements Function<Callable<Object>, Future<Object>> {
        private final AtomicInteger counter;

        BlockingRunner(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public Future<Object> apply(Callable<Object> objectCallable) {
            CompletableFuture<Object> ret = new CompletableFuture<>();
            totalStartedCount.incrementAndGet();
            activeCount.incrementAndGet();
            try {
                final CyclicBarrier barrier;
                final String lastKey;
                synchronized (waitForMap) {
                    lastKey = queuedKeys.remove(0);
                    barrier = waitForMap.get(lastKey);
                }
                assertNotNull(barrier, "No barrier key [" + lastKey + "] set");
                simulatedRunner.execute(() -> {
                    try {
                        barrier.await(waitTimeoutSeconds, TimeUnit.SECONDS);
                    } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                        fail("Test did not join after " + waitTimeoutSeconds + " seconds", e);
                    }
                    counter.incrementAndGet();
                    try {
                        ret.complete(objectCallable.call());
                    } catch (Exception e) {
                        ret.completeExceptionally(e);
                    }
                    try {
                        barrier.await(waitTimeoutSeconds, TimeUnit.SECONDS);
                    } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                        callableThrown.add(e);
                        fail("Test did not join after " + waitTimeoutSeconds + " seconds", e);
                    }
                });
            } finally {
                activeCount.decrementAndGet();
            }
            return ret;
        }
    }


    private class InthreadRunAnswer implements Consumer<Runnable> {
        private final AtomicInteger counter;

        InthreadRunAnswer(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void accept(Runnable runnable) {
            totalStartedCount.incrementAndGet();
            activeCount.incrementAndGet();
            try {
                counter.incrementAndGet();
                runnable.run();
            } finally {
                activeCount.decrementAndGet();
            }
        }
    }
}
