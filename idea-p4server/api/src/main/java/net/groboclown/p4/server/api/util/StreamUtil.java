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

package net.groboclown.p4.server.api.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class StreamUtil {
    public static <K,V> Map<K,V> asMap(Collection<K> s, Function<K, ? extends V> f) {
        Map<K,V> ret = new HashMap<>();
        s.forEach((k) -> ret.put(k, f.apply(k)));
        return ret;
    }


    public static <K,V> Map<K,V> asMap(Stream<K> s, Function<K, ? extends V> f) {
        Map<K,V> ret = new HashMap<>();
        s.forEach((k) -> ret.put(k, f.apply(k)));
        return ret;
    }

    public static <K,V> Map<K,V> asReversedMap(Collection<V> s, Function<V, ? extends K> f) {
        Map<K,V> ret = new HashMap<>();
        s.forEach((k) -> ret.put(f.apply(k), k));
        return ret;
    }


    public static <K,V> Map<K,V> asReversedMap(Stream<V> s, Function<V, ? extends K> f) {
        Map<K,V> ret = new HashMap<>();
        s.forEach((k) -> ret.put(f.apply(k), k));
        return ret;
    }
}
