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

package net.groboclown.p4.server.cache.sync;

import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.ServerUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public abstract class ImmediateServerUpdateAction implements ServerUpdateAction {
    @Override
    @NotNull
    public Collection<PendingUpdateState> getPendingUpdateStates() {
        return Collections.emptyList();
    }


    @Override
    public void abort(@NotNull ClientCacheManager clientCacheManager) {
        // intentionally empty
    }
}
