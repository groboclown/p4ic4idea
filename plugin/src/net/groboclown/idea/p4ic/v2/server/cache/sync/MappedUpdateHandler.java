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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 *
 * @param <T> the state object that each {@link PendingUpdateState} relates to.
 */
public class MappedUpdateHandler<T> {
    private static final Logger LOG = Logger.getInstance(MappedUpdateHandler.class);
    private final Map<PendingUpdateState, T> mapping = new HashMap<PendingUpdateState, T>();
    private final Collection<PendingUpdateState> pendingUpdateStates;
    private final List<PendingUpdateState> failed = new ArrayList<PendingUpdateState>();
    private final List<PendingUpdateState> success = new ArrayList<PendingUpdateState>();
    private final List<T> unmappedSuccess = new ArrayList<T>();
    private final List<T> unmappedFailed = new ArrayList<T>();


    public interface StateClearHandler<T> {
        void clearState(@NotNull T state, @NotNull ClientCacheManager clientCacheManager);
    }



    public MappedUpdateHandler(@NotNull Collection<PendingUpdateState> pendingUpdateStates) {
        this.pendingUpdateStates = Collections.unmodifiableCollection(pendingUpdateStates);
    }


    @NotNull
    public Collection<PendingUpdateState> getPendingUpdateStates() {
        return pendingUpdateStates;
    }


    public void markFailed(@NotNull PendingUpdateState update) {
        validateUpdate(update);
        if (! mapping.containsKey(update)) {
            LOG.warn("Did not create mapping for " + update);
        }
        failed.add(update);
    }


    public void markStateFailed(@NotNull T state) {
        unmappedFailed.add(state);
    }


    public void markSuccess(@NotNull PendingUpdateState update) {
        validateUpdate(update);
        if (!mapping.containsKey(update)) {
            LOG.warn("Did not create mapping for " + update);
        }
        success.add(update);
    }


    public void markStateSuccess(@NotNull T state) {
        unmappedSuccess.add(state);
    }


    public void onSuccess(@NotNull StateClearHandler<T> handler, @NotNull ClientCacheManager clientCacheManager) {
        List<PendingUpdateState> unhandled = new ArrayList<PendingUpdateState>(pendingUpdateStates);
        clearFor(handler, success, unmappedSuccess, unhandled, "succeeded", clientCacheManager);
        clearFor(handler, failed, unmappedFailed, unhandled, "failed", clientCacheManager);
        if (! unhandled.isEmpty()) {
            LOG.warn("Did not mark as handled: " + unhandled);
        }
    }


    public void onFailure(@NotNull StateClearHandler<T> handler, @NotNull ClientCacheManager clientCacheManager) {
        // success and failure end up doing the same thing - clearing out both the
        // failed and succeeded updates.
        onSuccess(handler, clientCacheManager);
    }


    public MappedUpdateHandler<T> onRetry(@NotNull StateClearHandler<T> handler,
            @NotNull ClientCacheManager clientCacheManager) {
        // clear out just the succeeded updates, and return a new update for just the
        // failed and not run marked ones.

        List<PendingUpdateState> unhandled = new ArrayList<PendingUpdateState>(pendingUpdateStates);
        clearFor(handler, success, unmappedSuccess, unhandled, "succeeded", clientCacheManager);

        return new MappedUpdateHandler<T>(unhandled);
    }



    public void map(@NotNull PendingUpdateState update, @NotNull T state) {
        validateUpdate(update);
        this.mapping.put(update, state);
    }

    @Override
    public String toString() {
        return pendingUpdateStates.toString();
    }

    private void clearFor(@NotNull StateClearHandler<T> handler, Collection<PendingUpdateState> toClear,
            final List<T> unmapped, List<PendingUpdateState> unhandledList, String type,
            final ClientCacheManager clientCacheManager) {
        for (T state: unmapped) {
            handler.clearState(state, clientCacheManager);
        }
        for (PendingUpdateState update : toClear) {
            unhandledList.remove(update);
            final T state = mapping.get(update);
            if (state == null) {
                LOG.error("Did not create mapping for " + type + " update " + update);
            } else {
                handler.clearState(state, clientCacheManager);
            }
        }
    }


    private void validateUpdate(@NotNull PendingUpdateState update) {
        if (!pendingUpdateStates.contains(update)) {
            throw new IllegalArgumentException("update " + update + " not associated with this handler");
        }
    }

}
