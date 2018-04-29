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
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

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
 */
public interface IdeChangelistMap {
    @Nullable
    LocalChangeList getIdeChangeFor(@NotNull P4ChangelistId changelistId);

    @Nullable
    P4ChangelistId getP4ChangeFor(@NotNull LocalChangeList changeList);

    /**
     * Returns the current mapping of Perforce changelists to IDE change lists.
     * This is mostly used for synchronization, to find if any of these Perforce
     * changes are submitted or deleted, and, if so, to remove the link in a call
     * to {@link #updateForDeletedSubmittedChanges(Stream, boolean)}.
     *
     * @return all stored linked IDE ChangeList instances for the given client.
     */
    @NotNull
    Map<P4ChangelistId, LocalChangeList> getLinkedIdeChanges();

    /**
     * Ensures that all the open Perforce changelists for the given client are in the
     * IDE.  This will only add new links and IDE changelists, so if an IDE changelist
     * contains a link to a changelist not present in the arguments, it will not be
     * changed; use {@link #updateForDeletedSubmittedChanges(Stream, boolean)} to
     * alter those mappings.
     * <p>
     * Because this will move files between changelists, the map requires an updated
     * file mapping.  If files in the changelist are not in the mapping, then the
     * mapping will broadcast an appropriate error message to the message bus, but ignore
     * that file.  If the IDE records additional files in changelists that aren't in the
     * {@literal openChanges}, then they will be moved to the unmarked list.
     *
     * @param fileMap the mapping of Perforce files to the IDE files.
     * @param openChanges all changelists which are open for the client on the server.
     *                    This includes the default changelist.
     */
    void updateForOpenChanges(@NotNull IdeFileMap fileMap, @NotNull Stream<P4RemoteChangelist> openChanges);

    /**
     * Updates the links between changelists.  Should be called with the list of linked
     * changes from {@link #getLinkedIdeChanges()}, which were queried against
     * changes on the server.  These closed (submitted or deleted) changelists will be
     * removed from the mapping, and, if the IDE change list exists, it will be removed if
     * it is empty.
     *
     * @param closedChanges list of Perforce changelists known to be closed.
     * @param deleteNotEmpty if the IDE changelist is to be deleted, but it still has files
     *                        in it, then still remove it, and move the contained files to
     *                        the default IDE changelist.  Regardless of this flag, the default
     *                        IDE changelist will not be removed.
     */
    void updateForDeletedSubmittedChanges(@NotNull Stream<P4ChangelistSummary> closedChanges,
            boolean deleteNotEmpty);
}
