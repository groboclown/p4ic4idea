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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class FilteredIterable<T> implements Iterable<T> {
    private final Iterable<T> src;
    private final Function<T, Boolean> filter;

    public FilteredIterable(Iterable<T> src, Function<T, Boolean> filter) {
        this.src = src;
        this.filter = filter;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new FilteredIterator<>(src, filter);
    }


    public static class FilteredIterator<T> implements Iterator<T> {
        private final Iterator<T> proxy;
        private final Function<T, Boolean> filter;
        private boolean hasNext = true;
        private T next;

        public FilteredIterator(Iterable<T> src, Function<T, Boolean> filter) {
            this(src.iterator(), filter);
        }
        public FilteredIterator(Iterator<T> src, Function<T, Boolean> filter) {
            this.proxy = src;
            this.filter = filter;
            checkNext();
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            T ret = next;
            checkNext();
            return ret;
        }

        private void checkNext() {
            while (proxy.hasNext()) {
                next = proxy.next();
                if (filter.apply(next)) {
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
            next = null;
        }
    }
}
