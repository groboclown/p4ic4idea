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

package net.groboclown.p4.server.api.values;

import com.perforce.p4java.core.file.FileAction;
import org.jetbrains.annotations.NotNull;

/**
 * @see com.perforce.p4java.core.file.FileAction
 */
public enum P4FileAction {
    ADD,
    ADD_EDIT,
    EDIT,
    INTEGRATE,
    DELETE,

    // pseudo-actions for internal use
    REVERTED,
    EDIT_RESOLVED,

    MOVE_DELETE,
    MOVE_ADD,
    MOVE_EDIT,
    MOVE_ADD_EDIT,

    // indicates that the file is open for edit, and the
    // file type is changed.
    REOPEN,

    UNKNOWN,

    /** not marked as modified */
    NONE;


    @NotNull
    public static P4FileAction convert(@NotNull FileAction action) {
        switch (action) {
            // Some of these don't make sense in most contexts, but deal with them
            // as best as possible.

            case ADD:
            case ADDED:
                return ADD;
            case ADD_EDIT:
                return ADD_EDIT;
            case EDIT:
                return EDIT;
            case BRANCH:
            case INTEGRATE:
            case RESOLVED:
            case UNRESOLVED:
            case COPY_FROM:
            case MERGE_FROM:
            case EDIT_FROM:
            case IMPORT:
                return INTEGRATE;
            case DELETE:
            case DELETED:
            case PURGE:
            case ARCHIVE:
                return DELETE;
            case MOVE_DELETE:
                return MOVE_DELETE;
            case MOVE:
                // TODO is this right?
                return MOVE_EDIT;
            case MOVE_ADD:
                return MOVE_ADD;

            case SYNC:
            case UPDATED:
            case REFRESHED:
            case REPLACED:
            case IGNORED:
            case ABANDONED:
            case EDIT_IGNORED:
                return NONE;

            case UNKNOWN:
            default:
                return UNKNOWN;
        }
    }
}
