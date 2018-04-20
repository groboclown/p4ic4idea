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

package net.groboclown.p4.server.cache.state;

import com.intellij.openapi.vcs.FileStatus;
import com.perforce.p4java.core.file.FileAction;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4ShelvedFile {
    private static final String BASE_PREFIX = "//";

    private final String depotPath;
    private final String localPath;
    private final FileStatus status;

    // Really, files shouldn't have the changelist association, because they can
    // move between them, and so the association should be changelist HAS-A file,
    // but shelved files are special; you can have multiple shelved versions of
    // the same file path in different changelists.
    private final int changeListId;

    P4ShelvedFile(@NotNull String depotPath, @NotNull String localPath, @NotNull FileStatus status,
            int changeListId) {
        if (! depotPath.startsWith(BASE_PREFIX)) {
            throw new IllegalArgumentException("Invalid depot path " + depotPath);
        }
        this.depotPath = depotPath;
        this.localPath = localPath;
        this.status = status;
        this.changeListId = changeListId;
    }

    @NotNull
    public String getDepotPath() {
        return depotPath;
    }

    @NotNull
    public FileStatus getStatus() {
        return status;
    }

    public int getChangeListId() {
        return changeListId;
    }

    public boolean isDeleted() {
        return P4Vcs.SHELVED_DELETED.getId().equals(status.getId());
    }

    public boolean isAdded() {
        return P4Vcs.SHELVED_ADDED.getId().equals(status.getId());
    }

    public boolean isEdited() {
        return P4Vcs.SHELVED_MODIFIED.getId().equals(status.getId());
    }

    @Override
    public int hashCode() {
        return depotPath.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (that == null || !(that.getClass().equals(P4ShelvedFile.class))) {
            return false;
        }
        P4ShelvedFile st = (P4ShelvedFile) that;
        return st.depotPath.equals(depotPath) && st.changeListId == changeListId;
    }

    public String getLocalPath() {
        return localPath;
    }

    public static boolean isShelvedPath(@Nullable String path) {
        return (path != null && path.startsWith(BASE_PREFIX));
    }


    @NotNull
    private static FileStatus getShelvedFileStatusFor(@NotNull FileAction action) {
        switch (action) {
            case ADD:
            case ADD_EDIT:
            case ADDED:
            case BRANCH:
            case MOVE:
            case MOVE_ADD:
            case COPY_FROM:
            case MERGE_FROM:
                return P4Vcs.SHELVED_ADDED;

            case EDIT:
            case INTEGRATE:
            case REPLACED:
            case UPDATED:
            case EDIT_FROM:
                return P4Vcs.SHELVED_MODIFIED;

            case DELETE:
            case DELETED:
            case MOVE_DELETE:
                return P4Vcs.SHELVED_DELETED;

            case SYNC:
            case REFRESHED:
            case IGNORED:
            case ABANDONED:
            case EDIT_IGNORED:
            case RESOLVED:
            case UNRESOLVED:
            case PURGE:
            case IMPORT:
            case ARCHIVE:
            case UNKNOWN:
            default:
                return P4Vcs.SHELVED_UNKNOWN;
        }
    }
}
