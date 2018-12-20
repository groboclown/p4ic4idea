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

package net.groboclown.p4plugin.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("WeakerAccess")
@State(name = "p4-UserProjectPreferences")
public class UserProjectPreferences
        implements PersistentStateComponent<UserProjectPreferences.State> {
    public static final int USER_MESSAGE_LEVEL_VERBOSE = 2;
    public static final int USER_MESSAGE_LEVEL_INFO = 3;
    public static final int USER_MESSAGE_LEVEL_WARNING = 4;
    public static final int USER_MESSAGE_LEVEL_ERROR = 5;
    public static final int USER_MESSAGE_LEVEL_ALWAYS = Integer.MAX_VALUE;


    public static final int DEFAULT_SERVER_CONNECTIONS = 2;
    public static final int MIN_SERVER_CONNECTIONS = 1;
    public static final int MAX_SERVER_CONNECTIONS = 5;
    public static final boolean DEFAULT_PREFER_REVISIONS_FOR_FILES = true;
    public static final boolean DEFAULT_CONCATENATE_CHANGELIST_NAME_COMMENT = false;
    public static final int DEFAULT_SOCKET_SO_TIMEOUT_MILLIS = 30000;
    public static final int MIN_SOCKET_SO_TIMEOUT_MILLIS = 20000;
    public static final int MAX_SOCKET_SO_TIMEOUT_MILLIS = 15 * 60 * 1000;
    public static final int MIN_LOCK_WAIT_TIMEOUT_MILLIS = 20 * 1000;
    public static final int MAX_LOCK_WAIT_TIMEOUT_MILLIS = 5 * 60 * 1000;
    public static final int DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS = 30 * 1000;
    public static final int DEFAULT_MAX_CLIENT_RETRIEVE_COUNT = 500;
    public static final int MIN_CLIENT_RETRIEVE_COUNT = 20;
    public static final int MAX_CLIENT_RETRIEVE_COUNT = 1000;
    public static final int DEFAULT_MAX_CHANGELIST_RETRIEVE_COUNT = 500;
    public static final int MIN_CHANGELIST_RETRIEVE_COUNT = 20;
    public static final int MAX_CHANGELIST_RETRIEVE_COUNT = 1000;
    public static final int DEFAULT_MAX_FILE_RETRIEVE_COUNT = 5000;
    public static final int MIN_FILE_RETRIEVE_COUNT = 500;
    public static final int MAX_FILE_RETRIEVE_COUNT = 50000;
    public static final boolean DEFAULT_AUTO_CHECKOUT_MODIFIED_FILES = false;
    public static final boolean DEFAULT_REMOVE_P4_CHANGELISTS = true;
    public static final int DEFAULT_USER_MESSAGE_LEVEL = USER_MESSAGE_LEVEL_WARNING;
    public static final int DEFAULT_MAX_CHANGELIST_NAME_LENGTH = 67;
    public static final int MIN_CHANGELIST_NAME_LENGTH = 33;
    public static final int MAX_CHANGELIST_NAME_LENGTH = 200;
    public static final int DEFAULT_RETRY_ACTION_COUNT = 2;
    public static final int MIN_RETRY_ACTION_COUNT = 0;
    public static final int MAX_RETRY_ACTION_COUNT = 5;

    @NotNull
    private State state = new State();

    // Fields need public access for IDEA state management.
    @SuppressWarnings("WeakerAccess")
    public static class State {
        public int maxServerConnections = DEFAULT_SERVER_CONNECTIONS;

        @Deprecated
        public int maxConnectionWaitTimeMillis = 0;

        @Deprecated
        public boolean integrateOnCopy = false;

        @Deprecated
        public boolean editInSeparateThread = false;

        public boolean preferRevisionsForFiles = DEFAULT_PREFER_REVISIONS_FOR_FILES;

        @Deprecated
        public boolean editedWithoutCheckoutDontVerify = false;

        @Deprecated
        public int maxAuthenticationRetries = 0;

        @Deprecated
        public boolean reconnectWithEachRequest = false;

        public boolean concatenateChangelistNameComment = DEFAULT_CONCATENATE_CHANGELIST_NAME_COMMENT;

        @Deprecated
        public boolean isAutoOffline = false;

        public int socketSoTimeoutMillis = DEFAULT_SOCKET_SO_TIMEOUT_MILLIS;

        public int lockWaitTimeoutMillis = DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS;

        @Deprecated
        public boolean showDialogConnectionMessages = false;

        public int maxClientRetrieveCount = DEFAULT_MAX_CLIENT_RETRIEVE_COUNT;

        public int maxChangelistRetrieveCount = DEFAULT_MAX_CHANGELIST_RETRIEVE_COUNT;

        public int maxFileRetrieveCount = DEFAULT_MAX_FILE_RETRIEVE_COUNT;

        public boolean autoCheckoutModifiedFiles = DEFAULT_AUTO_CHECKOUT_MODIFIED_FILES;

        public boolean removeP4Changelists = DEFAULT_REMOVE_P4_CHANGELISTS;

        public int userMessageLevel = DEFAULT_USER_MESSAGE_LEVEL;

        public int maxChangelistNameLength = DEFAULT_MAX_CHANGELIST_NAME_LENGTH;

        public int retryActionCount = DEFAULT_RETRY_ACTION_COUNT;
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


    @SuppressWarnings("NullableProblems")
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

    // ====================================
    // Used by P4ServerComponent
    public static int getMaxServerConnections(@Nullable Project project) {
        return getValue(project, DEFAULT_SERVER_CONNECTIONS, (p) -> p.getMaxServerConnections());
    }

    public int getMaxServerConnections() {
        return state.maxServerConnections;
    }

    public void setMaxServerConnections(int count) {
        state.maxServerConnections = count;
    }


    // ====================================
    // Used by P4AnnotatedFileImpl and others
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

    // ====================================
    // Used by ChangelistUtil
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


    // ====================================
    // Used by P4ServerComponent
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


    // ====================================
    // Used just about everywhere.
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


    // ====================================
    // Used by P4ServerComponent
    public static int getMaxClientRetrieveCount(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_MAX_CLIENT_RETRIEVE_COUNT;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_MAX_CLIENT_RETRIEVE_COUNT;
        }
        return prefs.getMaxClientRetrieveCount();
    }

    public int getMaxClientRetrieveCount() {
        return state.maxClientRetrieveCount;
    }

    public void setMaxClientRetrieveCount(final int count) {
        state.maxClientRetrieveCount = count;
    }


    // ====================================
    // Used by CacheComponent
    public static int getMaxChangelistRetrieveCount(@Nullable final Project project) {
        if (project == null) {
            return DEFAULT_MAX_CHANGELIST_RETRIEVE_COUNT;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return DEFAULT_MAX_CHANGELIST_RETRIEVE_COUNT;
        }
        return prefs.getMaxChangelistRetrieveCount();
    }

    public int getMaxChangelistRetrieveCount() {
        return state.maxChangelistRetrieveCount;
    }

    public void setMaxChangelistRetrieveCount(final int count) {
        state.maxChangelistRetrieveCount = count;
    }


    // ====================================
    // Used by CacheComponent
    public static int getMaxFileRetrieveCount(@Nullable final Project project) {
        return getValue(project, DEFAULT_MAX_FILE_RETRIEVE_COUNT,
                (prefs) -> prefs.getMaxFileRetrieveCount());
    }

    public int getMaxFileRetrieveCount() {
        return state.maxFileRetrieveCount;
    }

    public void setMaxFileRetrieveCount(final int count) {
        state.maxFileRetrieveCount = count;
    }


    // ====================================
    // Used by P4ChangeProvider
    public static boolean getAutoCheckoutModifiedFiles(@Nullable final Project project) {
        return getValue(project, DEFAULT_AUTO_CHECKOUT_MODIFIED_FILES,
                (prefs) -> prefs.getAutoCheckoutModifiedFiles());
    }

    public boolean getAutoCheckoutModifiedFiles() {
        return state.autoCheckoutModifiedFiles;
    }

    public void setAutoCheckoutModifiedFiles(final boolean value) {
        state.autoCheckoutModifiedFiles = value;
    }


    // ====================================
    // Used by P4ChangelistListener
    public static boolean getRemoveP4Changelist(@Nullable final Project project) {
        return getValue(project, DEFAULT_REMOVE_P4_CHANGELISTS,
                (prefs) -> prefs.getRemoveP4Changelist());
    }

    public boolean getRemoveP4Changelist() {
        return state.removeP4Changelists;
    }

    public void setRemoveP4Changelist(final boolean value) {
        state.removeP4Changelists = value;
    }


    // ====================================
    // Used by UserMessage
    public static boolean isUserMessageLevel(@Nullable final Project project, int level) {
        return level >= getUserMessageLevel(project);
    }

    public static int getUserMessageLevel(@Nullable final Project project) {
        return getValue(project, DEFAULT_USER_MESSAGE_LEVEL,
                (prefs) -> prefs.getUserMessageLevel());
    }

    public int getUserMessageLevel() {
        return state.userMessageLevel;
    }

    public void setUserMessageLevel(final int value) {
        state.userMessageLevel = value;
    }


    // ====================================
    // Used by P4ChangeProvider
    public static int getMaxChangelistNameLength(@Nullable final Project project) {
        return getValue(project, DEFAULT_MAX_CHANGELIST_NAME_LENGTH,
                (prefs) -> prefs.getMaxChangelistNameLength());
    }

    public int getMaxChangelistNameLength() {
        return Math.max(MIN_CHANGELIST_NAME_LENGTH, state.maxChangelistNameLength);
    }

    public void setMaxChangelistNameLength(int len) {
        state.maxChangelistNameLength = Math.max(MIN_CHANGELIST_NAME_LENGTH, len);
    }


    // ====================================
    // Used by MessageErrorHandler
    public static int getRetryActionCount(@Nullable final Project project) {
        return getValue(project, DEFAULT_RETRY_ACTION_COUNT,
                (prefs) -> prefs.getRetryActionCount());
    }

    public int getRetryActionCount() {
        return Math.min(MAX_RETRY_ACTION_COUNT, Math.max(MIN_RETRY_ACTION_COUNT, state.retryActionCount));
    }

    public void setRetryActionCount(int count) {
        state.retryActionCount = Math.min(MAX_RETRY_ACTION_COUNT, Math.max(MIN_RETRY_ACTION_COUNT, count));
    }


    private static <T> T getValue(@Nullable final Project project, @NotNull T defaultValue,
            @NotNull Function<UserProjectPreferences, T> fetcher) {
        if (project == null) {
            return defaultValue;
        }
        UserProjectPreferences prefs = UserProjectPreferences.getInstance(project);
        if (prefs == null) {
            return defaultValue;
        }
        return fetcher.apply(prefs);
    }
}
