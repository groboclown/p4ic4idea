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

import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockThreadRunner;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.util.ResultErrorUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncAnswerTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void background_resolve()
            throws InterruptedException {
        MockThreadRunner tr = new MockThreadRunner(idea);
        tr.setNextWaitKey("bg1");
        Object expected1 = new Object();
        Object expected2 = new Object();
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch endLatch = new CountDownLatch(5);

        Answer<Object> answer = AsyncAnswer.background((sink) -> {
            startLatch.countDown();
            try {
                assertTrue(startLatch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail(e);
            }
            sink.resolve(expected1);
            endLatch.countDown();
        })
        .whenCompleted((o) -> {
            assertSame(expected1, o);
            endLatch.countDown();
        })
        .whenFailed(Assertions::fail)
        .map((o) -> {
            assertSame(expected1, o);
            endLatch.countDown();
            return expected2;
        })
        .whenCompleted((o) -> {
            assertSame(expected2, o);
            endLatch.countDown();
        })
        .mapAsync((o) -> {
            assertSame(expected2, o);
            endLatch.countDown();
            return DoneAnswer.resolve(expected1);
        });

        assertFalse(answer.blockingWait(1, TimeUnit.MILLISECONDS));
        tr.waitFor("bg1");
        assertFalse(answer.blockingWait(1, TimeUnit.MILLISECONDS));
        startLatch.countDown();
        assertTrue(startLatch.await(5, TimeUnit.SECONDS));
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, tr.getEdtLaterRunCount());
        assertEquals(0, tr.getEdtWaitRunCount());
        assertEquals(1, tr.getPooledThreadsRunCount());

        tr.assertNoExceptions();
        tr.assertAllActionsCompleted();

        // Now that it completed, ensure that immediate requests are
        // handled right.
        final AtomicInteger completedCount = new AtomicInteger(0);
        answer.whenCompleted((o) -> {
            assertSame(expected1, o);
            completedCount.incrementAndGet();
        });
        assertEquals(1, completedCount.get());
        answer.whenFailed((t) -> {
            fail(t);
            completedCount.incrementAndGet();
        });
        assertEquals(1, completedCount.get());

        tr.assertNoExceptions();
        tr.assertAllActionsCompleted();
        tr.assertTotalStartedActionCount(1);
    }

    @Test
    void background_reject()
            throws InterruptedException {
        MockThreadRunner tr = new MockThreadRunner(idea);
        tr.setNextWaitKey("bg1");
        P4CommandRunner.ServerResultException expected = ResultErrorUtil.createInternalException();
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch endLatch = new CountDownLatch(1);
        CountDownLatch tailLatch = new CountDownLatch(1);

        Answer<Object> answer = AsyncAnswer.background((sink) -> {
            startLatch.countDown();
            try {
                assertTrue(startLatch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail(e);
            }
            sink.reject(expected);
            endLatch.countDown();
        })
        .whenCompleted((o) -> fail("Should not be completed"))
        .whenFailed((t) -> {
            assertSame(expected, t);
            tailLatch.countDown();
        });

        assertFalse(answer.blockingWait(1, TimeUnit.MILLISECONDS));
        tr.waitFor("bg1");
        assertFalse(answer.blockingWait(1, TimeUnit.MILLISECONDS));
        startLatch.countDown();
        assertTrue(startLatch.await(5, TimeUnit.SECONDS));
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, tr.getEdtLaterRunCount());
        assertEquals(0, tr.getEdtWaitRunCount());
        assertEquals(1, tr.getPooledThreadsRunCount());

        // Did it complete correctly?
        assertTrue(tailLatch.await(5, TimeUnit.SECONDS));

        tr.assertNoExceptions();
        tr.assertAllActionsCompleted();
        tr.assertTotalStartedActionCount(1);
    }

    @Test
    void doubleSink() {
        AsyncAnswer<Object> answer1 = new AsyncAnswer<>();
        answer1.resolve("");
        assertThrows(IllegalStateException.class, () -> answer1.resolve(""));
        assertThrows(IllegalStateException.class, () -> answer1.reject(ResultErrorUtil.createInternalException()));
    }

    @Test
    void future_resolved()
            throws InterruptedException, P4CommandRunner.ServerResultException {
        idea.getMockApplication().setIsDispatchThread(false);
        AsyncAnswer<Object> answer1 = new AsyncAnswer<>();
        answer1.resolve("a");
        AsyncAnswer<Object> answer2 = new AsyncAnswer<>();
        Answer<Object> answer3 = answer2.futureMap((x, sink) -> answer1.whenCompleted(sink::resolve));
        answer2.resolve("b");
        assertEquals("a", Answer.blockingGet(answer3, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    void map_reject() {
        AsyncAnswer<Object> answer1 = new AsyncAnswer<>();
        Answer<Object> answer2 = answer1.map((c) -> "b");
        answer2.whenCompleted((c) -> fail("Should never be called"));
        P4CommandRunner.ServerResultException sre = new P4CommandRunner.ServerResultException(
                new P4CommandRunner.ResultError() {
                    @NotNull
                    @Override
                    public P4CommandRunner.ErrorCategory getCategory() {
                        return P4CommandRunner.ErrorCategory.CONNECTION;
                    }

                    @Nls
                    @NotNull
                    @Override
                    public Optional<String> getMessage() {
                        return Optional.empty();
                    }
                });
        answer2.whenFailed((actual) -> assertSame(sre, actual));

        answer1.reject(sre);
    }

    @Test
    void mapAsync_background_resolve()
            throws InterruptedException {
        MockThreadRunner tr = new MockThreadRunner(idea);
        Object expected1 = new Object();
        Object expected2 = new Object();

        tr.setNextWaitKey("bg1");
        Answer<Object> answer = AsyncAnswer.background((sink) -> {
            sink.resolve(expected1);
        })
        .mapAsync((o) -> {
            assertSame(expected1, o);
            tr.setNextWaitKey("bg2");
            return AsyncAnswer.background((sink) -> {
                sink.resolve(expected2);
            });
        });

        // Now that it completed, ensure that immediate requests are
        // handled right.
        final AtomicInteger completedCount = new AtomicInteger(0);
        answer.whenCompleted((o) -> {
            assertSame(expected2, o);
            completedCount.incrementAndGet();
        })
        .whenFailed(Assertions::fail);

        tr.startAndWaitFor("bg1");
        tr.startAndWaitFor("bg2");
        tr.assertNoExceptions();
        assertTrue(answer.blockingWait(500, TimeUnit.SECONDS));
        assertEquals(1, completedCount.get());

        tr.assertTotalStartedActionCount(2);
    }

    // TODO tests for reject errors passed to other promises.

}
