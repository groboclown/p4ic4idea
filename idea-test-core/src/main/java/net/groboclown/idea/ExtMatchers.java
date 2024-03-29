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

import com.intellij.openapi.util.Pair;
import net.groboclown.idea.matchers.CollectionsMatchers;
import net.groboclown.idea.matchers.NumberMatchers;

import java.util.Collection;

public class ExtMatchers {
    public static <T> CollectionsMatchers.EmptyCollectionMatcher<T> isEmpty() {
        return new CollectionsMatchers.EmptyCollectionMatcher<>();
    }

    public static <K,V> CollectionsMatchers.EmptyMapMatcher<K,V> isEmptyMap() {
        return new CollectionsMatchers.EmptyMapMatcher<>();
    }

    public static <T> CollectionsMatchers.SizedCollectionMatcher<T> hasSize(int size) {
        return new CollectionsMatchers.SizedCollectionMatcher<>(size);
    }

    public static <K,V> CollectionsMatchers.SizedMapMatcher<K,V> hasMapSize(int size) {
        return new CollectionsMatchers.SizedMapMatcher<>(size);
    }

    @SafeVarargs
    public static <T> CollectionsMatchers.ContainsAllMatcher<T> containsAll(T... items) {
        return new CollectionsMatchers.ContainsAllMatcher<>(items);
    }

    public static <T> CollectionsMatchers.ContainsAllMatcher<T> containsAll(Collection<T> items) {
        return new CollectionsMatchers.ContainsAllMatcher<>(items);
    }

    public static <T> CollectionsMatchers.ContainsExactlyMatcher<T> containsExactly(T... items) {
        return new CollectionsMatchers.ContainsExactlyMatcher<T>(items);
    }

    @SafeVarargs
    public static <K,V> CollectionsMatchers.MapContainsAllMatcher<K,V> mapContainsAll(Pair<K,V>... pairs) {
        return new CollectionsMatchers.MapContainsAllMatcher<>(pairs);
    }

    public static NumberMatchers.GreaterThan greaterThan(Number n) {
        return new NumberMatchers.GreaterThan(n);
    }

    public static NumberMatchers.LessThan lessThan(Number n) {
        return new NumberMatchers.LessThan(n);
    }
}
