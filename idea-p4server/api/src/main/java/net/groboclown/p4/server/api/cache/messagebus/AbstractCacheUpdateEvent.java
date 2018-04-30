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

package net.groboclown.p4.server.api.cache.messagebus;

import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.ClientServerRef;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class AbstractCacheUpdateEvent<E extends AbstractCacheUpdateEvent<E>> {
    private final ClientServerRef ref;
    private final Set<String> visitedCacheIds = new HashSet<>();
    private final Date created = new Date();

    /**
     * Used by messages to receive events posted to the message bus.
     *
     * @param <E> event type
     */
    public interface Visitor<E extends AbstractCacheUpdateEvent<E>> {
        void visit(@NotNull E event);
    }

    protected AbstractCacheUpdateEvent(@NotNull ClientServerRef ref) {
        this.ref = ref;
    }

    void visit(@NotNull String cacheId, @NotNull Visitor<E> visitor) {
        boolean run;
        synchronized (visitedCacheIds) {
            run = !visitedCacheIds.contains(cacheId);
            if (run) {
                visitedCacheIds.add(cacheId);
            }
        }
        if (run) {
            //noinspection unchecked
            visitor.visit((E) this);
        }
    }

    @NotNull
    public Date getCreated() {
        return created;
    }

    @NotNull
    public ClientServerRef getRef() {
        return ref;
    }
}
