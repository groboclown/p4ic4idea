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

package net.groboclown.idea.p4ic.v2.server;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The front-end view of an action on a file object.  This is a copy of the local
 * cached version.
 */
public class P4FileAction {
    private final P4FileUpdateState local;
    private final UpdateAction action;


    public P4FileAction(@NotNull P4FileUpdateState local, @NotNull UpdateAction action) {
        this.local = local;
        this.action = action;
    }


    public FileUpdateAction getFileUpdateAction() {
        return local.getFileUpdateAction();
    }


    public int getChangeList() {
        return local.getActiveChangelist();
    }


    //public P4ClientFileMapping getP4File() {
    //    return local.getClientFileMapping();
    //}


    @Nullable
    public FilePath getFile() {
        return local.getLocalFilePath();
    }

    @Nullable
    public String getDepotPath() {
        return local.getDepotPath();
    }

    public boolean affects(@NotNull FilePath file) {
        final FilePath localFile = local.getLocalFilePath();
        if (localFile == null) {
            return false;
        }
        return FileUtil.filesEqual(file.getIOFile(), localFile.getIOFile());
    }


    @Nullable
    public FileStatus getClientFileStatus() {
        if (getChangeList() < P4ChangeListId.P4_DEFAULT) {
            // offline
            switch (getFileUpdateAction()) {
                case ADD_FILE:
                case MOVE_FILE:
                    return P4Vcs.ADDED_OFFLINE;
                case ADD_EDIT_FILE:
                case INTEGRATE_FILE:
                case EDIT_FILE:
                    return P4Vcs.MODIFIED_OFFLINE;
                case DELETE_FILE:
                case MOVE_DELETE_FILE:
                    return P4Vcs.DELETED_OFFLINE;
                case REVERT_FILE:
                    return P4Vcs.REVERTED_OFFLINE;
                default:
                    return FileStatus.NOT_CHANGED;
            }
        }
        switch (getFileUpdateAction()) {
            case MOVE_FILE:
                return FileStatus.ADDED;
            case INTEGRATE_FILE:
                // TODO determine if conflicts
                return FileStatus.MERGE;
            case ADD_FILE:
                return FileStatus.ADDED;
            case ADD_EDIT_FILE:
                // can't really tell here.
            case EDIT_FILE:
                return FileStatus.MODIFIED;
            case DELETE_FILE:
            case MOVE_DELETE_FILE:
                return FileStatus.DELETED;
            case REVERT_FILE:
                // TODO Weird status that shouldn't happen
                return P4Vcs.REVERTED_OFFLINE;
            default:
                return FileStatus.NOT_CHANGED;
        }
    }


    @Override
    public String toString() {
        return local + "->" + action;
    }
}
