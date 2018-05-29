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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.config.LockTimeoutProvider;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.impl.config.LockTimeoutProviderImpl;
import net.groboclown.p4.server.impl.values.P4LocalChangelistBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IdeChangelistCacheStore {
    private static final Logger LOG = Logger.getInstance(IdeChangelistCacheStore.class);


    // Map of IDE changelist IDs -> Perforce changelist IDs (possibly pending).
    private final Map<P4ChangelistId, String> linkedChangelistIds = new HashMap<>();

    // create changelist action IDs -> changelist.
    private final Map<String, P4LocalChangelist> pendingChangelists = new HashMap<>();

    private final AtomicInteger pendingChangelistIdCounter = new AtomicInteger(-2);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private LockTimeoutProvider lockTimeout = new LockTimeoutProviderImpl();

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public int lastPendingChangelistId;
        public List<LinkedChangelistState> linkedChangelistMap;
        public List<PendingChangelistState> pendingChangelistMap;
    }


    @SuppressWarnings("WeakerAccess")
    public static class LinkedChangelistState {
        public P4ChangelistIdStore.State changelistId;
        public String linkedLocalChangeId;
    }


    @SuppressWarnings("WeakerAccess")
    public static class PendingChangelistState {
        // note: this duplicates data that's stored with the action.
        public P4LocalChangelistStore.State p4Changelist;
        public String linkedLocalChangeId;
    }


    public String getMappedIdeChangeListId(P4ChangelistId p4id)
            throws InterruptedException {
        return lockTimeout.withLock(lock.readLock(), () -> linkedChangelistIds.get(p4id));
    }

    @NotNull
    public List<P4ChangelistId> getLinkedChangelists(@NotNull String ideId)
            throws InterruptedException {
        return lockTimeout.withLock(lock.readLock(), () -> {
            List<P4ChangelistId> ret = new ArrayList<>();
            for (Map.Entry<P4ChangelistId, String> entry : linkedChangelistIds.entrySet()) {
                if (entry.getValue().equals(ideId)) {
                    ret.add(entry.getKey());
                }
            }
            return ret;
        });
    }

    @NotNull
    public Map<P4ChangelistId, String> getLinkedChangelistIds()
            throws InterruptedException {
        return lockTimeout.withLock(lock.readLock(), () -> new HashMap<>(linkedChangelistIds));
    }


    @NotNull
    public List<P4LocalChangelist> getPendingChangelists()
            throws InterruptedException {
        return lockTimeout.withLock(lock.readLock(), () -> new ArrayList<>(pendingChangelists.values()));
    }


    @Nullable
    public P4LocalChangelist getPendingChangelist(CreateChangelistAction action, boolean create)
            throws InterruptedException {
        String actionId = action.getActionId();
        return lockTimeout.withLock(lock.readLock(), () -> {
            P4LocalChangelist pending = pendingChangelists.get(actionId);
            if (pending == null && create) {
                pending = lockTimeout.withLock(lock.writeLock(), () -> {
                    Integer next = pendingChangelistIdCounter.decrementAndGet();
                    P4LocalChangelist changelist = new P4LocalChangelistBuilder()
                            .withChangelistId(action.getClientServerRef(), next)
                            .withClientname(action.getClientServerRef().getClientName())
                            .withComment(action.getComment())
                            .build();

                    pendingChangelists.put(actionId, changelist);
                    return changelist;
                });
            }
            return pending;
        });
    }

    public void mapPendingChangelistId(@NotNull CreateChangelistAction action, @NotNull String localId)
            throws InterruptedException {
        P4LocalChangelist changelist = getPendingChangelist(action, true);

        // Should always be non-null, because of create=true, but just to be sure...
        if (changelist != null) {
            lockTimeout.withLock(lock.writeLock(), () -> {
                linkedChangelistIds.put(changelist.getChangelistId(), localId);
            });
        }
    }

    public void setLink(@NotNull P4ChangelistId p4ChangelistId, @NotNull String localId)
            throws InterruptedException {
        lockTimeout.withLock(lock.writeLock(), () -> {
            linkedChangelistIds.put(p4ChangelistId, localId);
        });
    }


    public void updateCreatedChangelist(@NotNull P4ChangelistId serverChangelistId, @NotNull CreateChangelistAction action)
            throws InterruptedException {
        lockTimeout.withLock(lock.writeLock(), () -> {
            P4LocalChangelist pendingChange = pendingChangelists.get(action.getActionId());
            if (pendingChange != null) {
                // Update its link
                String ideId = linkedChangelistIds.remove(pendingChange.getChangelistId());
                if (ideId != null) {
                    linkedChangelistIds.put(serverChangelistId, ideId);
                }
            } else {
                LOG.warn("Attempted to remap pending Create Changelist action (" + action +
                        "), but it wasn't known");
            }
        });
    }


    public void removeAction(@NotNull CreateChangelistAction action)
            throws InterruptedException {
        lockTimeout.withLock(lock.writeLock(), () -> {
            P4LocalChangelist pendingChange = pendingChangelists.remove(action.getActionId());
            if (pendingChange != null) {
                linkedChangelistIds.remove(pendingChange.getChangelistId());
            }
        });
    }


    public void deleteChangelist(@NotNull P4ChangelistId changelistId)
            throws InterruptedException {
        lockTimeout.withLock(lock.writeLock(), () -> {
            linkedChangelistIds.remove(changelistId);
            for (Map.Entry<String, P4LocalChangelist> entry : pendingChangelists.entrySet()) {
                if (entry.getValue().getChangelistId().equals(changelistId)) {
                    pendingChangelists.remove(entry.getKey());
                    break;
                }
            }
        });
    }


    @NotNull
    State getState()
            throws InterruptedException {
        final State ret = new State();

        lockTimeout.withLock(lock.readLock(), () -> {
            ret.linkedChangelistMap = new ArrayList<>(linkedChangelistIds.size());
            for (Map.Entry<P4ChangelistId, String> entry : linkedChangelistIds.entrySet()) {
                LinkedChangelistState state = new LinkedChangelistState();
                state.changelistId = P4ChangelistIdStore.getState(entry.getKey());
                state.linkedLocalChangeId = entry.getValue();
                ret.linkedChangelistMap.add(state);
            }

            ret.pendingChangelistMap = new ArrayList<>(pendingChangelists.size());
            for (Map.Entry<String, P4LocalChangelist> entry : pendingChangelists.entrySet()) {
                PendingChangelistState state = new PendingChangelistState();
                state.linkedLocalChangeId = entry.getKey();
                state.p4Changelist = P4LocalChangelistStore.getState(entry.getValue());
                ret.pendingChangelistMap.add(state);
            }

            ret.lastPendingChangelistId = pendingChangelistIdCounter.get();
        });

        return ret;
    }

    void setState(@Nullable State state)
            throws InterruptedException {
        if (state != null) {
            lockTimeout.withLock(lock.writeLock(), () -> {
                linkedChangelistIds.clear();
                for (LinkedChangelistState linkedChangelistState : state.linkedChangelistMap) {
                    P4ChangelistId p4id = P4ChangelistIdStore.read(linkedChangelistState.changelistId);
                    linkedChangelistIds.put(p4id, linkedChangelistState.linkedLocalChangeId);
                }

                pendingChangelists.clear();
                for (PendingChangelistState pendingChangelistState : state.pendingChangelistMap) {
                    P4LocalChangelist p4cl = P4LocalChangelistStore.read(pendingChangelistState.p4Changelist);
                    pendingChangelists.put(pendingChangelistState.linkedLocalChangeId, p4cl);
                }

                pendingChangelistIdCounter.set(state.lastPendingChangelistId);
            });
        }
    }

}
