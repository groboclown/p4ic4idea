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

package net.groboclown.idea.p4ic.v2.server.cache;

import net.groboclown.idea.p4ic.v2.server.cache.state.DecodeReferences;
import net.groboclown.idea.p4ic.v2.server.cache.state.EncodeReferences;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Pending updates are descriptive, not prescriptive.  The reconcilers handle
 * changing the state based on updates and states.
 */
public interface PendingUpdate {
    interface Deserializer<T extends PendingUpdate> {
        T deserialize(@NotNull Element wrapper, @NotNull DecodeReferences refs);
    }


    /** the objects that this update relates to */
    Set<ObjectId> getObjectIds();


    /**
     * The group to which this update belongs.  If multiple updates with the same group
     * are in the queue, they can run together before updating the corresponding
     * {@link CachedServerState}.
     *
     * @return null if not part of a group
     */
    String getGroupId();

    void serialize(@NotNull Element wrapper, @NotNull EncodeReferences refs);
}
