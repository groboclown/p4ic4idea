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

package net.groboclown.p4.server.impl.cache;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nullable;

/**
 * FIXME this is an experiment.  Don't think of it as the final form.
 * Rather than trying to make this super generic, we'll probably just have
 * hard-coded state objects and mappings.
 *
 *
 * @param <R>
 * @param <C>
 */
public interface SelfRefreshingCache<R, C> {
    void loadFromCache(@NotNull ClientConfig config, @Nullable C cachedState);

    C getCachedState();

    /**
     * Reload the current object from the server, and return itself (not a new copy).
     *
     * @param runner the object to run server commands against.
     * @return the promise that completes when the load completes.
     */
    Promise<R> reloadSelf(@NotNull P4CommandRunner runner);
}
