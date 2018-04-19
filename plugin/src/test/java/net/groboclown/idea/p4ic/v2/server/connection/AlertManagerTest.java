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
package net.groboclown.idea.p4ic.v2.server.connection;

import net.groboclown.idea.p4ic.v2.server.connection.AlertManager.ThrowableHandled;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AlertManagerTest {
    @Test
    public void testThrowableHandled1() {
        Exception ex = new Exception();
        final ThrowableHandled th = new ThrowableHandled();
        assertThat("first handle of error",
                th.isHandled(ex),
                is(false));

        assertThat("second handle of error",
                th.isHandled(ex),
                is(true));

        assertThat("embedded handle of error",
                th.isHandled(new Exception(ex)),
                is(true));

        assertThat("brand new error",
                th.isHandled(new Exception()),
                is(false));
    }
}
