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

package net.groboclown.idea.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.Collection;
import java.util.Map;

public class CollectionsMatchers {
    public static class EmptyCollectionMatcher<T> extends BaseMatcher<Collection<T>> {
        @Override
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an empty collection");
        }
    }

    public static class EmptyMapMatcher<K,V> extends BaseMatcher<Map<K,V>> {
        @Override
        public boolean matches(Object o) {
            return ((Map<?, ?>) o).isEmpty();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an empty map");
        }
    }

    public static class SizedCollectionMatcher<T> extends BaseMatcher<Collection<T>> {
        private final int size;

        public SizedCollectionMatcher(int size) {
            this.size = size;
        }

        @Override
        public boolean matches(Object o) {
            return ((Collection<?>) o).size() == size;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a collection with " + size + " items");
        }
    }

    public static class SizedMapMatcher<K,V> extends BaseMatcher<Map<K,V>> {
        private final int size;

        public SizedMapMatcher(int size) {
            this.size = size;
        }

        @Override
        public boolean matches(Object o) {
            return ((Map<?,?>) o).size() == size;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a map with " + size + " items");
        }
    }


}
