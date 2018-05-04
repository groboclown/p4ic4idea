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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EdtConsumerProcessorTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void process_callable_noListeners_noRunners_edtBackground() {
        MockThreadRunner mtr = new MockThreadRunner(idea);

        EdtConsumerProcessor<Object> ecp = new EdtConsumerProcessor<>();
        // The pooled thread
        mtr.setNextWaitKey("pooled-1");

        Object returned = new Object();
        ecp.load(() -> returned, true);
        // wait for the background pool to start up
        mtr.startAndWaitFor("pooled-1");

        mtr.assertNoExceptions();
        assertSame(returned, ecp.getItem());
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(0, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
    }

    @Test
    void process_callable_oneListener_oneRunner_edtBackground() {
        final MockThreadRunner mtr = new MockThreadRunner(idea);

        final EdtConsumerProcessor<Object> ecp = new EdtConsumerProcessor<>();

        // edt-runner-1
        final AtomicInteger listenerInvokedCount = new AtomicInteger(0);
        ecp.addListener(listenerInvokedCount::incrementAndGet);

        // edt-listener-1
        final List<Object> consumedValues = Collections.synchronizedList(new ArrayList<>());
        ecp.addListener(consumedValues::add);

        // The pooled thread
        mtr.setNextWaitKey("pooled-1");
        // The EDT background - listener 1
        mtr.setNextWaitKey("edt-listener-1");
        // The EDT background - runner 1
        mtr.setNextWaitKey("edt-runner-1");

        Object returned = new Object();
        ecp.load(() -> returned, true);

        // Let the pooled thread run to completion, while
        // the background runner threads wait.
        mtr.startAndWaitFor("pooled-1");

        assertSame(returned, ecp.getItem());
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(0, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());

        // Run the background edt stuff.
        mtr.startAndWaitFor("edt-listener-1");
        assertSame(returned, ecp.getItem());
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(1, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
        assertContainsAll(consumedValues, returned);

        // Run the background runner
        mtr.startAndWaitFor("edt-runner-1");
        assertSame(returned, ecp.getItem());
        assertEquals(1, mtr.getPooledThreadsRunCount());
        assertEquals(2, mtr.getEdtLaterRunCount());
        assertEquals(0, mtr.getEdtWaitRunCount());
    }
}