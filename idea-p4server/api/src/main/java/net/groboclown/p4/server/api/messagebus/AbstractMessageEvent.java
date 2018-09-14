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

package net.groboclown.p4.server.api.messagebus;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract, standard message event object for application and project messages.
 *
 * All visitedListeners must use subclasses of this as its first (or only) argument.
 */
public abstract class AbstractMessageEvent {
    private static final AtomicLong EVENT_COUNT = new AtomicLong(0);

    // There are problems with the same listener handling the same event multiple times.
    // There are two approaches to this - either keep track of handled events in the listener,
    // or keep track of the visited visitedListeners in the event.  Because the event object is short
    // lived, the memory won't grow by storing it in the event.
    private final Set<Object> visitedListeners = new HashSet<>();

    private long id;

    protected AbstractMessageEvent() {
        id = EVENT_COUNT.incrementAndGet();
    }

    /**
     *
     * @param listenerOwner owner object
     * @return true if the listener was already visited
     */
    boolean visit(@NotNull Object listenerOwner) {
        if (visitedListeners.contains(listenerOwner)) {
            return true;
        }
        visitedListeners.add(listenerOwner);
        return false;
    }
}
