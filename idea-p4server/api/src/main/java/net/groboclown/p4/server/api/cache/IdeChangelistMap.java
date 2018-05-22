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

package net.groboclown.p4.server.api.cache;


import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Keeps a mapping between IDE ChangeList objects and Perforce changelist ID objects.
 * This is maintained per-project, and the underlying state must be peristed between
 * executions.
 * <p>
 * The mapping is intentionally simple, and based on the server.  The plugin will store
 * a mapping between the internal IDE changelist key against the Perforce changelist ID.
 * If Perforce has a changelist that isn't stored in the map, a new changelist is created
 * in the IDE, and the link is created.  If a file is moved to an IDE changelist that
 * isn't in the map, then a new Perforce changelist is created, and the link is created.
 * <p>
 * Because of the potential for out-of-sync issues if the file map, update open changes,
 * and update deleted/submitted changes calls, the owner may wish to create temporary versions
 * of the maps, update them, then swap them out as the real versions.
 * <p>
 * Note that multiple Perforce changelists can be associated with a single IDE changelist.
 */
public interface IdeChangelistMap {
    @Nullable
    LocalChangeList getIdeChangeFor(@NotNull P4ChangelistId changelistId);

    @Nullable
    Collection<P4ChangelistId> getP4ChangesFor(@NotNull LocalChangeList changeList);

    /**
     * Returns the current mapping of Perforce changelists to IDE change lists.
     *
     * @return all stored linked IDE ChangeList instances for the given client.
     */
    @NotNull
    Map<P4ChangelistId, LocalChangeList> getLinkedIdeChanges();

    void setMapping(P4ChangelistId p4ChangelistId, LocalChangeList changeList);
}
