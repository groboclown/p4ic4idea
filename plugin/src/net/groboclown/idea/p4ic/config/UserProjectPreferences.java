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
                file = StoragePathMacros.PROJECT_FILE
        )
    }
)
public class UserProjectPreferences implements PersistentStateComponent<UserProjectPreferences.State> {
    public static final int MIN_SERVER_CONNECTIONS = 1;
    public static final int MAX_SERVER_CONNECTIONS = 5;
    public static final int DEFAULT_SERVER_CONNECTIONS = 2;
    public static final int MIN_CONNECTION_WAIT_TIME_MILLIS = 500;
    public static final int MAX_CONNECTION_WAIT_TIME_MILLIS = 5 * 60 * 1000;
    public static final int DEFAULT_CONNECTION_WAIT_TIME_MILLIS = 30 * 1000;
    public static final boolean DEFAULT_INTEGRATE_ON_COPY = false;
    public static final boolean DEFAULT_EDIT_IN_SEPARATE_THREAD = false;
    public static final boolean DEFAULT_PREFER_REVISIONS_FOR_FILES = true;

    @NotNull
    private State state = new State();

    public static class State {
        @Deprecated
        public int maxServerConnections = DEFAULT_SERVER_CONNECTIONS;

        @Deprecated
        public int maxConnectionWaitTimeMillis = DEFAULT_CONNECTION_WAIT_TIME_MILLIS;

        public boolean integrateOnCopy = DEFAULT_INTEGRATE_ON_COPY;

        public boolean editInSeparateThread = DEFAULT_EDIT_IN_SEPARATE_THREAD;

        public boolean preferRevisionsForFiles = DEFAULT_PREFER_REVISIONS_FOR_FILES;
    }

    @Nullable
    public static UserProjectPreferences getInstance(@NotNull final Project project) {
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


    public int getMaxServerConnections() {
        return Math.max(MIN_SERVER_CONNECTIONS,
                Math.min(MAX_SERVER_CONNECTIONS, state.maxServerConnections));
    }

    public void setMaxServerConnections(int maxServerConnections) {
        state.maxServerConnections =
                Math.max(MIN_SERVER_CONNECTIONS,
                    Math.min(MAX_SERVER_CONNECTIONS, maxServerConnections));
    }


    public int getMaxConnectionWaitTimeMillis() {
        return Math.max(MIN_CONNECTION_WAIT_TIME_MILLIS,
                Math.min(MAX_CONNECTION_WAIT_TIME_MILLIS,
                state.maxConnectionWaitTimeMillis));
    }


    public void setMaxConnectionWaitTimeMillis(int value) {
        state.maxConnectionWaitTimeMillis =
                Math.max(MIN_CONNECTION_WAIT_TIME_MILLIS,
                        Math.min(MAX_CONNECTION_WAIT_TIME_MILLIS, value));
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
}
