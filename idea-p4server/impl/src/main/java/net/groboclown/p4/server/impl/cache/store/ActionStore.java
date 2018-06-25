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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.RemoveJobFromChangelistAction;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4FileType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionStore {


    @SuppressWarnings("WeakerAccess")
    public static class PendingAction {
        public final String sourceId;
        public final P4CommandRunner.ClientAction<?> clientAction;
        public final P4CommandRunner.ServerAction<?> serverAction;

        private PendingAction(@NotNull String sourceId, @NotNull P4CommandRunner.ClientAction<?> clientAction) {
            this.sourceId = sourceId;
            this.clientAction = clientAction;
            this.serverAction = null;
        }

        private PendingAction(@NotNull String  sourceId, @NotNull P4CommandRunner.ServerAction<?> serverAction) {
            this.sourceId = sourceId;
            this.clientAction = null;
            this.serverAction = serverAction;
        }

        public State getState() {
            if (clientAction != null) {
                return ActionStore.getState(sourceId, clientAction);
            }
            assert serverAction != null;
            return ActionStore.getState(sourceId, serverAction);
        }

        public String getActionId() {
            if (clientAction != null) {
                return clientAction.getActionId();
            }
            assert serverAction != null;
            return serverAction.getActionId();
        }

        boolean isClientAction() {
            return clientAction != null;
        }

        boolean isServerAction() {
            return serverAction != null;
        }
    }


    @SuppressWarnings("WeakerAccess")
    public static class State {
        public P4CommandRunner.ClientActionCmd clientActionCmd;
        public P4CommandRunner.ServerActionCmd serverActionCmd;

        public String sourceId;
        public String actionId;
        public Map<String, Object> data;
    }


    @NotNull
    public static PendingAction read(@NotNull State state) {
        if (state.serverActionCmd != null) {
            return new PendingAction(state.sourceId, readServerAction(state));
        }
        if (state.clientActionCmd != null) {
            return new PendingAction(state.sourceId, readClientAction(state));
        }
        throw new IllegalArgumentException("Unknown action " + state.actionId + ": " + state.data);
    }


    @NotNull
    private static P4CommandRunner.ClientAction<?> readClientAction(@NotNull State state) {
        if (state.clientActionCmd == null) {
            throw new IllegalArgumentException("not a client action");
        }
        switch (state.clientActionCmd) {
            case MOVE_FILE:
                return new MoveFileAction(state.actionId,
                        VcsUtil.getFilePath((String) state.data.get("source")),
                        VcsUtil.getFilePath((String) state.data.get("target")));
            case ADD_EDIT_FILE:
                return new AddEditAction(state.actionId,
                        VcsUtil.getFilePath((String) state.data.get("file")),
                        P4FileType.convertNullable((String) state.data.get("type")),
                        P4ChangelistIdStore.readNullable((P4ChangelistIdStore.State) state.data.get("cl-id")),
                        (String) state.data.get("charset"));
            case DELETE_FILE:
                return new DeleteFileAction(state.actionId,
                        VcsUtil.getFilePath((String) state.data.get("file")),
                        P4ChangelistIdStore.read((P4ChangelistIdStore.State) state.data.get("cl-id")));
            case REVERT_FILE:
                return new RevertFileAction(state.actionId,
                        VcsUtil.getFilePath((String) state.data.get("file")));
            case MOVE_FILES_TO_CHANGELIST:
                //noinspection unchecked
                return new MoveFilesToChangelistAction(state.actionId,
                        P4ChangelistIdStore.read((P4ChangelistIdStore.State) state.data.get("cl-id")),
                        getFilePathList((List<String>) state.data.get("files")));
            case EDIT_CHANGELIST_DESCRIPTION:
                return new EditChangelistAction(state.actionId,
                        P4ChangelistIdStore.read((P4ChangelistIdStore.State) state.data.get("cl-id")),
                        (String) state.data.get("comment"));
            case ADD_JOB_TO_CHANGELIST:
                return new AddJobToChangelistAction(state.actionId,
                        P4ChangelistIdStore.read((P4ChangelistIdStore.State) state.data.get("cl-id")),
                        P4JobStore.read((P4JobStore.State) state.data.get("job")));
            case REMOVE_JOB_FROM_CHANGELIST:
                return new RemoveJobFromChangelistAction(state.actionId,
                        P4ChangelistIdStore.read((P4ChangelistIdStore.State) state.data.get("cl-id")),
                        P4JobStore.read((P4JobStore.State) state.data.get("job")));
            case CREATE_CHANGELIST:
                return new CreateChangelistAction(state.actionId,
                        ClientServerRefStore.read((ClientServerRefStore.State) state.data.get("ref")),
                        (String) state.data.get("comment"));
            case DELETE_CHANGELIST:
                return new DeleteChangelistAction(state.actionId,
                        P4ChangelistIdStore.read((P4ChangelistIdStore.State) state.data.get("changelist")));
            case FETCH_FILES:
                throw new IllegalArgumentException("Should not attempt to store a sync action");
            case SUBMIT_CHANGELIST:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
        }
        throw new IllegalArgumentException("Unknown client cmd " + state.clientActionCmd);
    }


    @NotNull
    private static P4CommandRunner.ServerAction<?> readServerAction(@NotNull State state) {
        if (state.serverActionCmd == null) {
            throw new IllegalArgumentException("not a server action");
        }
        switch (state.serverActionCmd) {
            case CREATE_JOB:
                return new CreateJobAction(state.actionId, P4JobStore.read((P4JobStore.State) state.data.get("job")));
            case LOGIN:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
        }
        throw new IllegalArgumentException("Unknown server cmd " + state.serverActionCmd);
    }


    @NotNull
    public static PendingAction createPendingAction(P4ServerName serverName,
            @NotNull P4CommandRunner.ServerAction<?> action) {
        return new PendingAction(getSourceId(serverName), action);
    }


    @NotNull
    public static PendingAction createPendingAction(ClientServerRef ref,
            @NotNull P4CommandRunner.ClientAction<?> action) {
        return new PendingAction(getSourceId(ref), action);
    }


    @NotNull
    static State getState(String sourceId, @NotNull P4CommandRunner.ServerAction<?> action) {
        State ret = new State();
        ret.sourceId = sourceId;
        ret.actionId = action.getActionId();
        ret.serverActionCmd = action.getCmd();
        ret.clientActionCmd = null;
        ret.data = new HashMap<>();
        switch (action.getCmd()) {
            case CREATE_JOB:
                ret.data.put("job", P4JobStore.getState(((CreateJobAction) action).getJob()));
                break;
            case LOGIN:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
            default:
                throw new IllegalArgumentException("Unknown server cmd " + action.getCmd());
        }
        return ret;
    }


    @NotNull
    static State getState(String sourceId, @NotNull P4CommandRunner.ClientAction<?> action) {
        State ret = new State();
        ret.sourceId = sourceId;
        ret.actionId = action.getActionId();
        ret.serverActionCmd = null;
        ret.clientActionCmd = action.getCmd();
        ret.data = new HashMap<>();
        switch (action.getCmd()) {
            case MOVE_FILE: {
                MoveFileAction a = (MoveFileAction) action;
                ret.data.put("source", a.getSourceFile().getPath());
                ret.data.put("target", a.getTargetFile().getPath());
                break;
            }
            case ADD_EDIT_FILE: {
                AddEditAction a = (AddEditAction) action;
                ret.data.put("file", a.getFile().getPath());
                ret.data.put("type", a.getFileType() == null
                        ? null
                        : a.getFileType().toString());
                ret.data.put("cl-id", P4ChangelistIdStore.getStateNullable(a.getChangelistId()));
                ret.data.put("charset", a.getCharset());
                break;
            }
            case DELETE_FILE: {
                DeleteFileAction a = (DeleteFileAction) action;
                ret.data.put("file", a.getFile().getPath());
                ret.data.put("cl-id", P4ChangelistIdStore.getState(a.getChangelistId()));
                break;
            }
            case REVERT_FILE:
                ret.data.put("file", ((RevertFileAction) action).getFile().getPath());
                break;
            case MOVE_FILES_TO_CHANGELIST: {
                MoveFilesToChangelistAction a = (MoveFilesToChangelistAction) action;
                ret.data.put("cl-id", P4ChangelistIdStore.getState(a.getChangelistId()));
                ret.data.put("files", getState(a.getFiles()));
                break;
            }
            case EDIT_CHANGELIST_DESCRIPTION: {
                EditChangelistAction a = (EditChangelistAction) action;
                ret.data.put("cl-id", P4ChangelistIdStore.getState(a.getChangelistId()));
                ret.data.put("comment", a.getComment());
                break;
            }
            case ADD_JOB_TO_CHANGELIST: {
                AddJobToChangelistAction a = (AddJobToChangelistAction) action;
                ret.data.put("cl-id", P4ChangelistIdStore.getState(a.getChangelistId()));
                ret.data.put("job", P4JobStore.getState(a.getJob()));
                break;
            }
            case REMOVE_JOB_FROM_CHANGELIST: {
                RemoveJobFromChangelistAction a = (RemoveJobFromChangelistAction) action;
                ret.data.put("cl-id", P4ChangelistIdStore.getState(a.getChangelistId()));
                ret.data.put("job", P4JobStore.getState(a.getJob()));
                break;
            }
            case CREATE_CHANGELIST: {
                CreateChangelistAction a = (CreateChangelistAction) action;
                ret.data.put("ref", ClientServerRefStore.getState(a.getClientServerRef()));
                ret.data.put("comment", a.getComment());
                break;
            }
            case DELETE_CHANGELIST:
                ret.data.put("changelist",
                        P4ChangelistIdStore.getState(((DeleteChangelistAction) action).getChangelistId()));
                break;
            case FETCH_FILES:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
            case SUBMIT_CHANGELIST:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
            default:
                throw new IllegalArgumentException("Unknown client cmd " + action.getCmd());
        }
        return ret;
    }

    static String getSourceId(ClientConfig config) {
        return getSourceId(config.getClientServerRef());
    }


    static String getSourceId(ClientServerRef config) {
        return "client:" + config.toString();
    }


    static String getSourceId(ServerConfig config) {
        return getSourceId(config.getServerName());
    }


    static String getSourceId(P4ServerName config) {
        return "server:" + config.getUrl();
    }


    @NotNull
    private static List<FilePath> getFilePathList(@NotNull List<String> state) {
        List<FilePath> ret = new ArrayList<>(state.size());
        for (String name : state) {
            ret.add(VcsUtil.getFilePath(name));
        }
        return ret;
    }

    @NotNull
    private static List<String> getState(@NotNull List<FilePath> paths) {
        List<String> ret = new ArrayList<>(paths.size());
        for (FilePath path : paths) {
            ret.add(path.getPath());
        }
        return ret;
    }

}
