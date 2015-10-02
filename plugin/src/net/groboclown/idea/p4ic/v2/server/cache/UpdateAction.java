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

import com.perforce.p4java.core.file.FileAction;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * All possible update actions.  Each one maps to a specific
 * action class.
 * <p/>
 * A note about groups vs. actions: it seems a bit redundant to have the groups
 * be separate, and not just have the factory instead of the group.  However,
 * the point of the groups is to allow several pending updates to be slightly
 * different actions, but with the same underlying operations, so that they
 * can be run in batch together.
 * <p/>
 * For example, the ADD_EDIT_FILE and EDIT_FILE use the same group, so that
 * they can run together.  They both will share the same underlying commands,
 * so they should run together.  The actual determination if the file is
 * able to be added or not is based upon the UpdateAction itself.
 */
public enum UpdateAction {
    CHANGE_CHANGELIST_DESCRIPTION(UpdateGroup.CHANGELIST,
            UpdateParameterNames.CHANGELIST, UpdateParameterNames.DESCRIPTION),
    ADD_JOB_TO_CHANGELIST(UpdateGroup.CHANGELIST,
            UpdateParameterNames.CHANGELIST, UpdateParameterNames.JOB),
    REMOVE_JOB_FROM_CHANGELIST(UpdateGroup.CHANGELIST,
            UpdateParameterNames.CHANGELIST, UpdateParameterNames.JOB),
    SET_CHANGELIST_FIX_STATE(UpdateGroup.CHANGELIST,
            UpdateParameterNames.CHANGELIST, UpdateParameterNames.FIX_STATE),
    // Changelists are indirectly created through the REOPEN_FILES_INTO_CHANGELIST action.
    //CREATE_CHANGELIST(UpdateGroup.CHANGELIST,
    //        UpdateParameterNames.CHANGELIST, UpdateParameterNames.DESCRIPTION),
    DELETE_CHANGELIST(UpdateGroup.CHANGELIST,
            UpdateParameterNames.CHANGELIST),

    SET_JOB_DETAIL(UpdateGroup.JOB,
            UpdateParameterNames.JOB, UpdateParameterNames.FIELD, UpdateParameterNames.VALUE),
    SET_JOB_DESCRIPTION(UpdateGroup.JOB,
            UpdateParameterNames.JOB, UpdateParameterNames.DESCRIPTION),
    CREATE_JOB(UpdateGroup.JOB,
            UpdateParameterNames.JOB, UpdateParameterNames.DESCRIPTION),
    DELETE_JOB(UpdateGroup.JOB,
            UpdateParameterNames.JOB),

    // Note: changing a file to a different changelist is not an independent action;
    // it's considered keeping the current action but with a new changelist number.
    ADD_EDIT_FILE(UpdateGroup.FILE_ADD_EDIT,
            UpdateParameterNames.DEPOT, UpdateParameterNames.FILE, UpdateParameterNames.CHANGELIST),
    DELETE_FILE(UpdateGroup.FILE_DELETE,
            UpdateParameterNames.DEPOT, UpdateParameterNames.FILE, UpdateParameterNames.CHANGELIST),
    MOVE_FILE(UpdateGroup.FILE,
            UpdateParameterNames.DEPOT, UpdateParameterNames.DEPOT_SOURCE,
            UpdateParameterNames.FILE, UpdateParameterNames.FILE_SOURCE, UpdateParameterNames.CHANGELIST),
    // TODO is this the right update group?
    MOVE_DELETE_FILE(UpdateGroup.FILE_DELETE,
            UpdateParameterNames.DEPOT, UpdateParameterNames.FILE, UpdateParameterNames.CHANGELIST),
    INTEGRATE_FILE(UpdateGroup.FILE,
            UpdateParameterNames.DEPOT, UpdateParameterNames.DEPOT_SOURCE,
            UpdateParameterNames.FILE, UpdateParameterNames.FILE_SOURCE, UpdateParameterNames.CHANGELIST),
    EDIT_FILE(UpdateGroup.FILE_ADD_EDIT,
            UpdateParameterNames.FILE, UpdateParameterNames.CHANGELIST),
    REVERT_FILE(UpdateGroup.FILE,
            UpdateParameterNames.DEPOT, UpdateParameterNames.FILE),

    // A hybrid action, due to the nature of creating local changelists and their relationship to
    // files moved into them.
    REOPEN_FILES_INTO_CHANGELIST(UpdateGroup.CHANGELIST,
            UpdateParameterNames.CHANGELIST, UpdateParameterNames.DESCRIPTION,
            UpdateParameterNames.FIELD),

    ADD_IGNORE_PATTERN(UpdateGroup.IGNORE_PATTERNS,
            UpdateParameterNames.PATTERN),
    IGNORE_FILE_UPDATE(UpdateGroup.IGNORE_PATTERNS)
    ;

    public enum UpdateParameterNames {
        JOB,
        DESCRIPTION,
        FIX_STATE,
        FIELD {
            @Override
            public boolean matches(@NotNull String key) {
                return key.toLowerCase().startsWith(getKeyName());
            }

            public String getKeyName(@NotNull String suffix) {
                return getKeyName() + suffix;
            }
        },
        VALUE {
            @Override
            public boolean matches(@NotNull String key) {
                return key.toLowerCase().startsWith(getKeyName());
            }

            public String getKeyName(@NotNull String suffix) {
                return getKeyName() + suffix;
            }
        },
        DEPOT,
        DEPOT_SOURCE,
        FILE,
        FILE_SOURCE,
        PATTERN,
        CHANGELIST {
            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public <T> T deserialize(@NotNull String value) {
                try {
                    return (T) new Integer(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        ;

        public String getKeyName() {
            return toString().toLowerCase();
        }

        @Nullable
        public <T> T getParameterValue(@NotNull PendingUpdateState updateState) {
            Object val = updateState.getParameters().get(getKeyName());
            if (val == null) {
                return null;
            }
            return getValue(val);
        }

        @Nullable
        @SuppressWarnings("unchecked")
        public <T> T getValue(@NotNull Object value) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                return null;
            }
        }

        @NotNull
        public String serialize(@NotNull Object value) {
            return value.toString();
        }

        @Nullable
        @SuppressWarnings("unchecked")
        public <T> T deserialize(@NotNull String value) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                return null;
            }
        }

        public boolean matches(@NotNull String key) {
            return key.equalsIgnoreCase(getKeyName());
        }
    }


    private final UpdateGroup group;
    private final List<UpdateParameterNames> parameterNames;

    UpdateAction(@NotNull UpdateGroup group, @NotNull UpdateParameterNames... parameterNames) {
        this.group = group;
        this.parameterNames = Collections.unmodifiableList(Arrays.asList(parameterNames));
    }

    @NotNull
    public UpdateGroup getGroup() {
        return group;
    }


    @NotNull
    public List<UpdateParameterNames> getParameterNames() {
        return parameterNames;
    }

    public boolean isValidParameterName(@Nullable final String key) {
        if (key == null) {
            return false;
        }
        for (UpdateParameterNames parameterName : parameterNames) {
            if (parameterName.matches(key)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public String serialize(@NotNull String key, @NotNull Object value) {
        for (UpdateParameterNames parameterName : parameterNames) {
            if (parameterName.matches(key)) {
                return parameterName.serialize(value);
            }
        }
        // Coding error
        throw new IllegalArgumentException("unknown key " + key + " for " + this);
    }

    @Nullable
    public <T> T deserialize(@Nullable final String key, @Nullable final String value) {
        if (key == null || value == null) {
            return null;
        }
        for (UpdateParameterNames parameterName : parameterNames) {
            if (parameterName.matches(key)) {
                return parameterName.deserialize(value);
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------------------------
    // Helper methods to map the "action" status from a Perforce command into an UpdateAction.
    // See the corresponding Perforce command reference documentation; it describes exactly
    // which action is returned by each operation.

    /**
     * Map the {@link FileAction} to an {@link UpdateAction} based on the valid
     * "p4 opened" command expected actions.
     *
     * @param action the file action; there are some reasons why it could be null,
     *               primarily because the method can return null.
     * @return the corresponding action, or {@code null} if it doesn't map.
     */
    public static UpdateAction getUpdateActionForOpened(@Nullable FileAction action) {
        if (action == null) {
            return null;
        }

        // http://www.perforce.com/perforce/doc.current/manuals/cmdref/p4_opened.html
        // Note that the description gives a complete list of expected actions, whereas
        // the field "action" does not.
        switch (action) {
            case ADD_EDIT:
                return ADD_EDIT_FILE;
            case EDIT:
                return EDIT_FILE;
            case DELETE:
                return DELETE_FILE;
            case BRANCH:
                // indicates that the file was added when the
                // integration happened; there is no pre-existing
                // file at the destination.
                return INTEGRATE_FILE;
            case INTEGRATE:
                // indicates that there was already a file at the
                // destination, and that the destination file needs
                // a potential resolve.
                return INTEGRATE_FILE;
            case MOVE_ADD:
                return MOVE_FILE;
            case MOVE_DELETE:
                return MOVE_DELETE_FILE;
            case IMPORT:
                // integrate from a remote depot
                return INTEGRATE_FILE;
            case PURGE:
                // The revision was moved into an archive depot, and was purged from the
                // source.
                // These operations are not valid in the context of the IDE; it means that
                // the user is performing administration actions on the depot.
                return null;
            case ARCHIVE:
                // The revision is marked for pushing into an archive depot.
                // These operations are not valid in the context of the IDE; it means that
                // the user is performing administration actions on the depot.
                return null;
            default:
                return null;
        }
    }

}
