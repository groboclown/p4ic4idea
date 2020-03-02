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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * General utilities for working with collections.
 */
public class CollectionUtil {
    /**
     * Filter out null entries from the list.  Returns a modifiable list.
     *
     * @param values values to filter
     * @param <T> type in the array.  It's nullable on the arguments, but not null in the return.
     * @return
     */
    @NotNull
    public static <T> ArrayList<T> filterNulls(@NotNull List<T> values) {
        ArrayList<T> ret = new ArrayList<>(values.size());
        for (T v: values) {
            if (v != null) {
                ret.add(v);
            }
        }
        return ret;
    }
}
