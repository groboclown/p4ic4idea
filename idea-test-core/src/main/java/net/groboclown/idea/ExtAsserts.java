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

package net.groboclown.idea;

import java.util.Collection;

import static net.groboclown.idea.ExtMatchers.containsAll;
import static net.groboclown.idea.ExtMatchers.containsExactly;
import static net.groboclown.idea.ExtMatchers.hasSize;
import static net.groboclown.idea.ExtMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExtAsserts {
    public static <T> void assertEmpty(Collection<T> actual) {
        assertThat(actual, isEmpty());
    }

    public static <T> void assertEmpty(Collection<T> actual, String msg) {
        assertThat(msg, actual, isEmpty());
    }

    @SafeVarargs
    public static <T> void assertContainsAll(Collection<T> actual, T... expected) {
        assertThat(actual, containsAll(expected));
    }

    public static <T> void assertContainsAll(Collection<T> actual, Collection<T> expected) {
        assertThat(actual, containsAll(expected));
    }

    @SafeVarargs
    public static <T> void assertContainsExactly(Collection<T> actual, T... expected) {
        assertThat(actual, containsExactly(expected));
    }

    public static <T> void assertSize(int expectedSize, Collection<T> actual) {
        assertThat(actual, hasSize(expectedSize));
    }
}
