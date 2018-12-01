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

package net.groboclown.idea.p4ic.v2.server.connection;

import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Executes a query against the Perforce server.  Unlike the
 * {@link ServerUpdateAction}, these are not persisted.  Therefore,
 * the implementations do not need a method for their construction
 * outside the requesting object.
 */
public interface ServerQuery<T> {
    /**
     * Performs the action.  If the action could not be completed, but needs to
     * be, it should put itself back into the queue (via the P4Server).
     * In all cases, after this executes, it will be removed from the store.
     * @param exec   Perforce execution object
     * @param cacheManager
     * @param runner
     * @param alerts graceful error handling
     */
    @Nullable
    T query(@NotNull P4Exec2 exec, @NotNull ClientCacheManager cacheManager,
            @NotNull ServerConnection connection, @NotNull SynchronizedActionRunner runner,
            @NotNull AlertManager alerts)
            throws InterruptedException;

}
