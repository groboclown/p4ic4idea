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

import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ChangeListServerCacheSync;
import net.groboclown.idea.p4ic.v2.server.cache.sync.FileActionsServerCacheSync;
import net.groboclown.idea.p4ic.v2.server.connection.ServerUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * The general category of update that this belongs to.  All updates that share
 * the same {@link UpdateGroup} will run in the same {@link ServerUpdateAction}.
 */
public enum UpdateGroup {
    /** Updates to the job description or whether the job exists */
    JOB(new NIF()), // FIXME

    /** CreateUpdate sto the workspace mappings or root directories; workspace existence should be
     * handled at a higher level (connection level). */
    WORKSPACE(new NIF()), // FIXME

    /** Updates to the description, job association, fix state, and existence. */
    CHANGELIST(new NIF()), // FIXME

    /** Update the file association on files. */
    CHANGELIST_FILES(new ChangeListServerCacheSync.MoveFileFactory()),

    /** Remove a changelist */
    CHANGELIST_DELETE(new ChangeListServerCacheSync.DeleteFactory()),

    /** Updates to the add or edit state and changelist association on files. */
    FILE_ADD_EDIT(new FileActionsServerCacheSync.AddEditFactory()),

    /** Updates to the delete state and changelist association on files. */
    FILE_DELETE(new FileActionsServerCacheSync.DeleteFactory()),

    FILE(new NIF()), // FIXME

    /** Updates to the ignore file */
    IGNORE_PATTERNS(new NIF()) // FIXME
    ;

    private final ServerUpdateActionFactory factory;

    UpdateGroup(@NotNull final ServerUpdateActionFactory factory) {
        this.factory = factory;
    }


    @NotNull
    public ServerUpdateActionFactory getServerUpdateActionFactory() {
        return factory;
    }

    static class NIF implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull final Collection<PendingUpdateState> states) {
            throw new IllegalStateException("Not implemented: " + states);
        }
    }
}
