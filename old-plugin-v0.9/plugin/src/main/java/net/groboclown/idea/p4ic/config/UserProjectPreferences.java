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

package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "UserProjectPreferences",
    storages = {
        @Storage(
                // removed in 181
                // id = "default",
                file = StoragePathMacros.PROJECT_FILE
        )
    }
)
public class UserProjectPreferences implements PersistentStateComponent<UserProjectPreferences.State> {
    public static final int DEFAULT_SERVER_CONNECTIONS = 2;
    public static final boolean DEFAULT_INTEGRATE_ON_COPY = false;
    public static final boolean DEFAULT_EDIT_IN_SEPARATE_THREAD = false;
    public static final boolean DEFAULT_PREFER_REVISIONS_FOR_FILES = true;
    public static final boolean DEFAULT_EDITED_WITHOUT_CHECKOUT_DONT_VERIFY = false;
    public static final int DEFAULT_MAX_AUTHENTICATION_RETRIES = 3;
    public static final int MIN_MAX_AUTHENTICATION_RETRIES = 0;
    public static final int MAX_MAX_AUTHENTICATION_RETRIES = 5;
    public static final boolean DEFAULT_RECONNECT_WITH_EACH_REQUEST = false;
    public static final boolean DEFAULT_CONCATENATE_CHANGELIST_NAME_COMMENT = false;
    public static final boolean DEFAULT_AUTO_OFFLINE = false;
    public static final int DEFAULT_SOCKET_SO_TIMEOUT_MILLIS = 30000;
    public static final int MIN_SOCKET_SO_TIMEOUT_MILLIS = 20000;
    public static final int MAX_SOCKET_SO_TIMEOUT_MILLIS = 5 * 60 * 1000;
    public static final int MIN_LOCK_WAIT_TIMEOUT_MILLIS = 20 * 1000;
    public static final int MAX_LOCK_WAIT_TIMEOUT_MILLIS = 5 * 60 * 1000;
    public static final int DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS = 30 * 1000;
    public static final boolean DEFAULT_SHOW_DIALOG_CONNECTION_MESSAGES = true;

    @NotNull
    private State state = new State();

    // Fields need public access for IDEA state management.
    @SuppressWarnings("WeakerAccess")
    public static class State {
        @Deprecated
        public int maxServerConnections = DEFAULT_SERVER_CONNECTIONS;

        @Deprecated
        public int maxConnectionWaitTimeMillis = 0;

        public boolean integrateOnCopy = DEFAULT_INTEGRATE_ON_COPY;

        public boolean editInSeparateThread = DEFAULT_EDIT_IN_SEPARATE_THREAD;

        public boolean preferRevisionsForFiles = DEFAULT_PREFER_REVISIONS_FOR_FILES;

        // This value needs to default to "false" so that existing
        // users of the plugin will continue to work as it was before.
        // This makes for the cumbersome naming here, and the inverse
        // getter / setter.
        public boolean editedWithoutCheckoutDontVerify = DEFAULT_EDITED_WITHOUT_CHECKOUT_DONT_VERIFY;

        public int maxAuthenticationRetries = DEFAULT_MAX_AUTHENTICATION_RETRIES;

        public boolean reconnectWithEachRequest = DEFAULT_RECONNECT_WITH_EACH_REQUEST;

        public boolean concatenateChangelistNameComment = DEFAULT_CONCATENATE_CHANGELIST_NAME_COMMENT;

        public boolean isAutoOffline = DEFAULT_AUTO_OFFLINE;

        public int socketSoTimeoutMillis = DEFAULT_SOCKET_SO_TIMEOUT_MILLIS;

        public int lockWaitTimeoutMillis = DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS;

        public boolean showDialogConnectionMessages = DEFAULT_SHOW_DIALOG_CONNECTION_MESSAGES;
    }

    @Nullable
    public static UserProjectPreferences getInstance(@NotNull final Project project) {
        if (project.isDisposed()) {
            return null;
        }
        return ServiceManager.getService(project, UserProjectPreferences.class);
    }


    public UserProjectPreferences() {
    }


    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Deprecated /* do not directly call */
    @Override
    public void loadState(State state) {
        if (state == null) {
            state = new State();
        }
        this.state = state;
    }


    public boolean getIntegrateOnCopy() {
        return state.integrateOnCopy;
    }


    public void setIntegrateOnCopy(boolean value) {
        state.integrateOnCopy = value;
    }


    public static boolean getEditInSeparateThread(@Nullable Project project) {
        if (project == null) {
            return DEFAULT_EDIT_IN_SEPARATE_THREAD;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_EDIT_IN_SEPARATE_THREAD;
        }
        return prefs.getEditInSeparateThread();
    }


    public boolean getEditInSeparateThread() {
        return state.editInSeparateThread;
    }


    public void setEditInSeparateThread(boolean value) {
        state.editInSeparateThread = value;
    }

    public static boolean getPreferRevisionsForFiles(@Nullable Project project) {
        if (project == null) {
            return DEFAULT_PREFER_REVISIONS_FOR_FILES;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_PREFER_REVISIONS_FOR_FILES;
        }
        return prefs.getPreferRevisionsForFiles();
    }

    public boolean getPreferRevisionsForFiles() {
        return state.preferRevisionsForFiles;
    }

    public void setPreferRevisionsForFiles(boolean value) {
        state.preferRevisionsForFiles = value;
    }

    public static boolean getEditedWithoutCheckoutVerify(@Nullable Project project) {
        if (project == null) {
            return ! DEFAULT_EDITED_WITHOUT_CHECKOUT_DONT_VERIFY;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return ! DEFAULT_EDITED_WITHOUT_CHECKOUT_DONT_VERIFY;
        }
        return prefs.getEditedWithoutCheckoutVerify();
    }

    public boolean getEditedWithoutCheckoutVerify() {
        return ! state.editedWithoutCheckoutDontVerify;
    }

    public void setEditedWithoutCheckoutVerify(boolean value) {
        state.editedWithoutCheckoutDontVerify = ! value;
    }

    public static int getMaxAuthenticationRetries(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_MAX_AUTHENTICATION_RETRIES;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_MAX_AUTHENTICATION_RETRIES;
        }
        return prefs.getMaxAuthenticationRetries();
    }

    public int getMaxAuthenticationRetries() {
        return state.maxAuthenticationRetries;
    }

    public void setMaxAuthenticationRetries(int value) {
        state.maxAuthenticationRetries = value;
    }

    public boolean isAutoOffline() {
        return state.isAutoOffline;
    }

    public void setAutoOffline(boolean value) {
        state.isAutoOffline = value;
    }

    public static boolean isAutoOffline(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_AUTO_OFFLINE;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_AUTO_OFFLINE;
        }
        return prefs.isAutoOffline();
    }

    public static boolean getReconnectWithEachRequest(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_RECONNECT_WITH_EACH_REQUEST;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_RECONNECT_WITH_EACH_REQUEST;
        }
        return prefs.getReconnectWithEachRequest();
    }

    public boolean getReconnectWithEachRequest() {
        return state.reconnectWithEachRequest;
    }

    public void setReconnectWithEachRequest(boolean value) {
        state.reconnectWithEachRequest = value;
    }


    public static boolean getConcatenateChangelistNameComment(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_CONCATENATE_CHANGELIST_NAME_COMMENT;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_CONCATENATE_CHANGELIST_NAME_COMMENT;
        }
        return prefs.getConcatenateChangelistNameComment();
    }

    public boolean getConcatenateChangelistNameComment() {
        return state.concatenateChangelistNameComment;
    }

    public void setConcatenateChangelistNameComment(final boolean concatenateChangelistNameComment) {
        state.concatenateChangelistNameComment = concatenateChangelistNameComment;
    }


    public static boolean getShowDialogConnectionMessages(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_SHOW_DIALOG_CONNECTION_MESSAGES;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_SHOW_DIALOG_CONNECTION_MESSAGES;
        }
        return prefs.getShowDialogConnectionMessages();
    }

    public boolean getShowDialogConnectionMessages() {
        return state.showDialogConnectionMessages;
    }

    public void setShowDialogConnectionMessages(final boolean showDialogConnectionMessages) {
        state.showDialogConnectionMessages = showDialogConnectionMessages;
    }


    public static int getSocketSoTimeoutMillis(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_SOCKET_SO_TIMEOUT_MILLIS;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_SOCKET_SO_TIMEOUT_MILLIS;
        }
        return prefs.getSocketSoTimeoutMillis();
    }

    public int getSocketSoTimeoutMillis() {
        return state.socketSoTimeoutMillis;
    }

    public void setSocketSoTimeoutMillis(final int socketSoTimeoutMillis) {
        state.socketSoTimeoutMillis = socketSoTimeoutMillis;
    }


    public static int getLockWaitTimeoutMillis(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS;
        }
        return prefs.getLockWaitTimeoutMillis();
    }

    public int getLockWaitTimeoutMillis() {
        return state.lockWaitTimeoutMillis;
    }

    public void setLockWaitTimeoutMillis(final int lockWaitTimeoutMillis) {
        state.lockWaitTimeoutMillis = lockWaitTimeoutMillis;
    }
}
