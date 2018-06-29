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

import com.intellij.openapi.util.Pair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


    public static class ContainsAllMatcher<T> extends BaseMatcher<Collection<T>> {
        private final List<T> items;

        public ContainsAllMatcher(T... items) {
            this.items = Arrays.asList(items);
        }

        public ContainsAllMatcher(Collection<T> items) {
            this.items = new ArrayList<>(items);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object o) {
            Set<T> remaining = new HashSet<>(items);
            Collection<T> that = (Collection<T>) o;
            for (T t : that) {
                if (!remaining.remove(t)) {
                    return false;
                }
            }
            return remaining.isEmpty();
        }

        @Override
        public void describeTo(Description description) {
            description.appendValueList("a collection which contains ", ", ", ".", items);
        }
    }


    public static class ContainsExactlyMatcher<T> extends BaseMatcher<Collection<T>> {
        private final List<T> items;

        public ContainsExactlyMatcher(T... items) {
            this.items = Arrays.asList(items);
        }

        public ContainsExactlyMatcher(Collection<T> items) {
            this.items = new ArrayList<>(items);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object o) {
            Iterator<T> that = ((Collection<T>) o).iterator();
            Iterator<T> mine = items.iterator();
            while (true) {
                if (that.hasNext() && mine.hasNext()) {
                    T tn = that.next();
                    T mn = mine.next();
                    if ((mn == null && tn != null) || (mn != null && !mn.equals(tn))) {
                        return false;
                    }
                } else if (!that.hasNext() && !mine.hasNext()) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendValueList("a collection which contains ", ", ", ".", items);
        }
    }

    public static class MapContainsAllMatcher<K,V> extends BaseMatcher<Map<K,V>> {
        Map<K,V> values = new HashMap<>();

        public MapContainsAllMatcher(Pair<K,V>... pairs) {
            for (Pair<K, V> pair : pairs) {
                values.put(pair.first, pair.second);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object o) {
            Map<K,V> remaining = new HashMap<>(values);
            for (Map.Entry<K, V> entry : ((Map<K, V>) o).entrySet()) {
                if (!remaining.containsKey(entry.getKey())) {
                    return false;
                }
                V v = remaining.remove(entry.getKey());
                if ((v == null && entry.getValue() != null) ||
                        (v != null && entry.getValue() == null) ||
                        (v != null && !v.equals(entry.getValue()))) {
                    return false;
                }
            }
            return remaining.isEmpty();
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(values);
        }
    }
}
