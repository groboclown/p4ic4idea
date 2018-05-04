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
package net.groboclown.p4plugin.ui;

import net.groboclown.idea.extensions.IdeaLightweightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.groboclown.idea.ExtAsserts.assertContainsAll;
import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EdtSinkProcessorTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void processSingle_callable_noListeners_noRunners_edtBackground() {
        MockThreadRunner mtr = new MockThreadRunner(idea);

        EdtSinkProcessor<Object> ecp = new EdtSinkProcessor<>();
        // The pooled thread
        mtr.setNextWaitKey("pooled-1");

        Object returned = new Object();
        ecp.processSingle(() -> returned, true);
        // wait for the background pool to start up
        mtr.startAndWaitFor("pooled-1");

        mtr.assertNoExceptions();
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(0, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
    }

    @Test
    void processSingle_callable_oneOfEverything_edtBackground() {
        // Well, no error trapping.  But we still listen for it.

        final MockThreadRunner mtr = new MockThreadRunner(idea);

        final EdtSinkProcessor<Object> ecp = new EdtSinkProcessor<>();

        // edt-starter-1
        final AtomicInteger startInvokedCount = new AtomicInteger(0);
        ecp.addStarter(startInvokedCount::incrementAndGet);

        // edt-end-1
        final AtomicInteger endInvokedCount = new AtomicInteger(0);
        ecp.addFinalizer(endInvokedCount::incrementAndGet);

        // edt-consumer-1
        final List<Object> consumedValues = Collections.synchronizedList(new ArrayList<>());
        ecp.addConsumer(consumedValues::add);

        // edt-consumer-2
        final List<Object> batchConsumedValues = Collections.synchronizedList(new ArrayList<>());
        ecp.addBatchConsumer(batchConsumedValues::addAll);

        // shouldn't ever be called
        final List<Throwable> caughtExceptions = Collections.synchronizedList(new ArrayList<>());
        ecp.addErrorHandler(caughtExceptions::add);

        // The pooled thread
        mtr.setNextWaitKey("pooled-1");
        // The EDT background - starter 1
        mtr.setNextWaitKey("edt-starter-1");
        // The EDT background - consumer 1
        mtr.setNextWaitKey("edt-consumer-1");
        // The EDT background - consumer 2
        mtr.setNextWaitKey("edt-consumer-2");
        // The EDT background - finalizer 1
        mtr.setNextWaitKey("edt-end-1");

        Object returned = new Object();
        ecp.processSingle(() -> returned, true);

        // Let the pooled thread run to completion, while
        // the background runner threads wait.
        mtr.startAndWaitFor("pooled-1");
        mtr.assertNoExceptions();
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(0, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
        assertEquals(0, startInvokedCount.get());
        assertEmpty(consumedValues);
        assertEmpty(batchConsumedValues);
        assertEmpty(caughtExceptions);
        assertEquals(0, endInvokedCount.get());

        // starters
        mtr.startAndWaitFor("edt-starter-1");
        mtr.assertNoExceptions();
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(1, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
        assertEquals(1, startInvokedCount.get());
        assertEmpty(consumedValues);
        assertEmpty(batchConsumedValues);
        assertEmpty(caughtExceptions);
        assertEquals(0, endInvokedCount.get());

        // Run the consumers
        mtr.startAndWaitFor("edt-consumer-1");
        mtr.assertNoExceptions();
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(2, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
        assertEquals(1, startInvokedCount.get());
        assertContainsAll(consumedValues, returned);
        assertEmpty(batchConsumedValues);
        assertEmpty(caughtExceptions);
        assertEquals(0, endInvokedCount.get());

        mtr.startAndWaitFor("edt-consumer-2");
        mtr.assertNoExceptions();
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(3, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
        assertEquals(1, startInvokedCount.get());
        assertContainsAll(consumedValues, returned);
        assertContainsAll(batchConsumedValues, returned);
        assertEmpty(caughtExceptions);
        assertEquals(0, endInvokedCount.get());

        // enders
        mtr.startAndWaitFor("edt-end-1");
        mtr.assertNoExceptions();
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(4, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
        assertEquals(1, startInvokedCount.get());
        assertContainsAll(consumedValues, returned);
        assertContainsAll(batchConsumedValues, returned);
        assertEmpty(caughtExceptions);
        assertEquals(1, endInvokedCount.get());
    }
}