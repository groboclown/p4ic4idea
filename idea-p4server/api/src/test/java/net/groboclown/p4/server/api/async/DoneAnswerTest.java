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
import net.groboclown.p4.server.api.util.ResultErrorUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


class DoneAnswerTest {
    @Test
    void whenCompleted()
            throws InterruptedException {
        final Object result = new Object();
        final Answer<Object> da = DoneAnswer.resolve(result);
        assertTrue(da.blockingWait(1, TimeUnit.MILLISECONDS));
        final AtomicInteger count = new AtomicInteger(0);
        da.whenCompleted((c) -> {
            assertSame(result, c);
            count.incrementAndGet();
        })
        .whenFailed(Assertions::fail);
        assertEquals(1, count.get());
    }

    @Test
    void whenFailed()
            throws InterruptedException {
        final P4CommandRunner.ServerResultException error = ResultErrorUtil.createInternalException();
        final Answer<Object> da = DoneAnswer.reject(error);
        assertTrue(da.blockingWait(1, TimeUnit.MILLISECONDS));
        final AtomicInteger count = new AtomicInteger(0);
        da.whenCompleted((c) -> fail("Should never have been called"))
                .whenFailed((t) -> {
                    assertSame(error, t);
                    count.incrementAndGet();
                });
        assertEquals(1, count.get());
    }

    @Test
    void map_completed()
            throws InterruptedException {
        final Object result1 = new Object();
        final Object result2 = new Object();
        final Answer<Object> da = DoneAnswer.resolve(result1);
        assertTrue(da.blockingWait(1, TimeUnit.MILLISECONDS));
        final AtomicInteger count = new AtomicInteger(0);
        Answer<Object> mapped = da
                .map((o) -> {
                    assertSame(result1, o);
                    count.incrementAndGet();
                    return result2;
                })
                .whenCompleted((o) -> {
                    assertSame(result2, o);
                    count.incrementAndGet();
                })
                .whenFailed(Assertions::fail);
        assertTrue(mapped.blockingWait(1, TimeUnit.MILLISECONDS));
        assertEquals(2, count.get());
    }

    @Test
    void map_failed() {
        // Chaining answers to answers when the first is rejected causes final chains to still
        // be rejected.
        final P4CommandRunner.ServerResultException error = ResultErrorUtil.createInternalException();
        final Answer<Object> da = DoneAnswer.reject(error);
        final AtomicInteger count = new AtomicInteger(0);
        da.whenCompleted((c) -> fail("Should never have been called"))
                .whenFailed((t) -> {
                    assertSame(error, t);
                    count.incrementAndGet();
                })
                .map((c) -> {
                    fail("Should never be called");
                    return null;
                })
                .whenFailed((t) -> {
                    assertSame(error, t);
                    count.incrementAndGet();
                })
                .map((c) -> {
                    fail("Should never be called");
                    return null;
                })
                .whenFailed((t) -> {
                    assertSame(error, t);
                    count.incrementAndGet();
                });
        assertEquals(3, count.get());
    }

    @Test
    void mapAsync_completed() {

    }

    @Test
    void mapAsync_failed() {

    }

    @Test
    void futureMap_completed() {
    }

    @Test
    void futureMap_failed() {
        // This is where a bug was discovered!
        final P4CommandRunner.ServerResultException error = ResultErrorUtil.createInternalException();
        final Answer<Object> da = DoneAnswer.reject(error);
        final AtomicInteger count = new AtomicInteger(0);
        da
            .futureMap((c, sink) -> {
                fail("Should not be called");
                sink.resolve(null);
            })
            .whenFailed((e) -> {
                count.incrementAndGet();
                assertSame(error, e);
            });
        assertEquals(1, count.get());
    }
}
