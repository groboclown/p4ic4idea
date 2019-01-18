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

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.messagebus.AbstractMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class AbstractCacheUpdateEvent<E extends AbstractCacheUpdateEvent<E>> extends AbstractMessageEvent {
    private final ClientServerRef ref;
    private final P4ServerName serverName;
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
        this.serverName = ref.getServerName();
    }

    protected AbstractCacheUpdateEvent(@NotNull P4ServerName name) {
        this.ref = null;
        this.serverName = name;
    }

    @SuppressWarnings("unchecked")
    void visit(@NotNull String cacheId, @NotNull Visitor<E> visitor) {
        boolean run = shouldVisitName(cacheId);
        if (run) {
            visitor.visit((E) this);
        }
    }

    @NotNull
    public Date getCreated() {
        return created;
    }

    @Nullable
    public ClientServerRef getClientRef() {
        return ref;
    }

    @NotNull
    public P4ServerName getServerName() {
        return serverName;
    }
}
