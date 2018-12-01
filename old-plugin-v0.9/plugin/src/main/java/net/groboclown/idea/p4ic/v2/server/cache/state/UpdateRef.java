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
package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A state that is associated with a {@link PendingUpdateState}.
 * When the corresponding {@link PendingUpdateState} is removed,
 * this object should be removed too.
 *
 */
public abstract class UpdateRef extends CachedState {
    private static final Logger LOG = Logger.getInstance(UpdateRef.class);

    private int pendingUpdateRefId;


    protected UpdateRef(@Nullable PendingUpdateState state) {
        if (state != null) {
            this.pendingUpdateRefId = state.getRefId();
        }
    }


    public int getPendingUpdateRefId() {
        return pendingUpdateRefId;
    }



    // Take over the parent's meaning, so that this is
    // automatically serialized / deserialized for all subclasses.
    @Override
    protected void serializeDate(@NotNull Element wrapper) {
        super.serializeDate(wrapper);
        wrapper.setAttribute("ur", encodeLong(pendingUpdateRefId));
    }

    @Override
    protected void deserializeDate(@NotNull Element wrapper) {
        super.deserializeDate(wrapper);
        Long refId = decodeLong(getAttribute(wrapper, "ur"));
        if (refId == null) {
            LOG.warn("Orphan " + getClass().getSimpleName() + " - no update ref id");
            pendingUpdateRefId = -1;
        } else {
            pendingUpdateRefId = refId.intValue();
        }
    }
}
