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

package net.groboclown.idea.p4ic.v2.server.cache.remote;

import net.groboclown.idea.p4ic.v2.server.cache.CachedServerState;

public class UpdatedCachedServerState<T extends CachedServerState<T>> {
    private final T original;
    private final T updated;

    public UpdatedCachedServerState(final T original, final T updated) {
        this.original = original;
        this.updated = updated;
    }

    public T getOriginalState() {
        return original;
    }

    public T getUpdatedState() {
        return updated;
    }
}
