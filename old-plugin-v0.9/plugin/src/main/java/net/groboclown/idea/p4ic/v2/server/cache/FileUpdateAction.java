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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All UpdateGroup.FILE actions.
 */
public enum FileUpdateAction {
    // TODO include a "pure" ADD_FILE action, so that file status can be accurate when we know what the server has.

    /** Indeterminate state, for when the user requests an edit or an add, but we don't
     * know yet if the server knows about the file. */
    ADD_EDIT_FILE(UpdateAction.ADD_EDIT_FILE),

    ADD_FILE(UpdateAction.ADD_FILE),
    DELETE_FILE(UpdateAction.DELETE_FILE),
    MOVE_FILE(UpdateAction.MOVE_FILE),
    MOVE_DELETE_FILE(UpdateAction.MOVE_DELETE_FILE),
    INTEGRATE_FILE(UpdateAction.INTEGRATE_FILE),
    EDIT_FILE(UpdateAction.EDIT_FILE),
    REVERT_FILE(UpdateAction.REVERT_FILE)
    ;

    private final UpdateAction action;

    FileUpdateAction(@NotNull final UpdateAction action) {
        this.action = action;
    }

    @NotNull
    public UpdateAction getUpdateAction() {
        return action;
    }

    @Nullable
    public static FileUpdateAction getFileUpdateAction(@NotNull UpdateAction ua) {
        for (FileUpdateAction fua: FileUpdateAction.values()) {
            if (fua.action == ua) {
                return fua;
            }
        }
        return null;
    }
}
