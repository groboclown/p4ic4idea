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

package net.groboclown.idea.p4ic.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class EqualUtilTest {
    @Test
    public void testEqualsNull2() {
        assertThat("2 null objects",
                EqualUtil.isEqual(null, null),
                is(true));
    }

    @Test
    public void testEqualsNullLeft() {
        assertThat("1 null object - left",
                EqualUtil.isEqual(null, new Object()),
                is(false));
    }

    @Test
    public void testEqualsNullRight() {
        assertThat("1 null object - left",
                EqualUtil.isEqual(new Object(), null),
                is(false));
    }

    @Test
    public void testEqualsNonNullNot() {
        assertThat("not equal",
                EqualUtil.isEqual(new Object(), new Object()),
                is(false));
    }

    @Test
    public void testEquals() {
        Object a = new Object();
        assertThat("not equal",
                EqualUtil.isEqual(a, a),
                is(true));
    }
}
