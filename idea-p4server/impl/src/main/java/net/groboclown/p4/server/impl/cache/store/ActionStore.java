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

        // The persistence mechanism for serialization / deserialization means that
        // we cannot store dynamic class information in the state - all classes must
        // be defined as concrete without magic polymorphism.
        public PrimitiveMap data;
    }


    @NotNull
    public static PendingAction read(@NotNull State state) throws PrimitiveMap.UnmarshalException {
        if (state.serverActionCmd != null) {
            return new PendingAction(state.sourceId, readServerAction(state));
        }
        if (state.clientActionCmd != null) {
            return new PendingAction(state.sourceId, readClientAction(state));
        }
        throw new PrimitiveMap.UnmarshalException("Unknown action " + state.actionId, "ActionStore.State");
    }


    @NotNull
    private static P4CommandRunner.ClientAction<?> readClientAction(@NotNull State state)
            throws PrimitiveMap.UnmarshalException {
        if (state.clientActionCmd == null) {
            throw new IllegalArgumentException("not a client action");
        }
        switch (state.clientActionCmd) {
            case MOVE_FILE:
                return new MoveFileAction(state.actionId,
                        state.data.getFilePathNotNull("source"),
                        state.data.getFilePathNotNull("target"));
            case ADD_EDIT_FILE:
                return new AddEditAction(state.actionId,
                        state.data.getFilePathNotNull("file"),
                        P4FileType.convertNullable(state.data.getStringNullable("type", null)),
                        state.data.getChangelistIdNullable("cl-id"),
                        state.data.getStringNullable("charset", null));
            case DELETE_FILE:
                return new DeleteFileAction(state.actionId,
                        state.data.getFilePathNotNull("file"),
                        state.data.getChangelistIdNullable("cl-id"));
            case REVERT_FILE:
                return new RevertFileAction(state.actionId,
                        state.data.getFilePathNotNull("file"),
                        state.data.getBooleanNullable("unchanged", false));
            case MOVE_FILES_TO_CHANGELIST:
                return new MoveFilesToChangelistAction(state.actionId,
                        state.data.getChangelistIdNotNull("cl-id"),
                        state.data.getFilePathList("files"));
            case EDIT_CHANGELIST_DESCRIPTION:
                return new EditChangelistAction(state.actionId,
                        state.data.getChangelistIdNotNull("cl-id"),
                        state.data.getStringNotNull("comment"));
            case ADD_JOB_TO_CHANGELIST:
                return new AddJobToChangelistAction(state.actionId,
                        state.data.getChangelistIdNotNull("cl-id"),
                        state.data.getP4Job("job"));
            case REMOVE_JOB_FROM_CHANGELIST:
                return new RemoveJobFromChangelistAction(state.actionId,
                        state.data.getChangelistIdNotNull("cl-id"),
                        state.data.getP4Job("job"));
            case CREATE_CHANGELIST:
                return new CreateChangelistAction(state.actionId,
                        state.data.getClientServerRefNotNull("ref"),
                        state.data.getStringNotNull("comment"));
            case DELETE_CHANGELIST:
                return new DeleteChangelistAction(state.actionId,
                        state.data.getChangelistIdNotNull("cl-id"));
            case FETCH_FILES:
                throw new IllegalArgumentException("Should not attempt to store a sync action");
            case SUBMIT_CHANGELIST:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
        }
        throw new IllegalArgumentException("Unknown client cmd " + state.clientActionCmd);
    }


    @NotNull
    static State getState(String sourceId, @NotNull P4CommandRunner.ClientAction<?> action) {
        State ret = new State();
        ret.sourceId = sourceId;
        ret.actionId = action.getActionId();
        ret.serverActionCmd = null;
        ret.clientActionCmd = action.getCmd();
        ret.data = new PrimitiveMap();
        switch (action.getCmd()) {
            case MOVE_FILE: {
                MoveFileAction a = (MoveFileAction) action;
                ret.data
                        .putFilePath("source", a.getSourceFile())
                        .putFilePath("target", a.getTargetFile());
                break;
            }
            case ADD_EDIT_FILE: {
                AddEditAction a = (AddEditAction) action;
                ret.data
                        .putFilePath("file", a.getFile())
                        .putString("type", a.getFileType() == null
                            ? null
                            : a.getFileType().toString())
                        .putChangelistId("cl-id", a.getChangelistId())
                        .putString("charset", a.getCharset());
                break;
            }
            case DELETE_FILE: {
                DeleteFileAction a = (DeleteFileAction) action;
                ret.data
                        .putFilePath("file", a.getFile())
                        .putChangelistId("cl-id", a.getChangelistId());
                break;
            }
            case REVERT_FILE: {
                RevertFileAction a = (RevertFileAction) action;
                ret.data
                        .putFilePath("file", a.getFile())
                        .putBoolean("unchanged", a.isRevertOnlyIfUnchanged());
                break;
            }
            case MOVE_FILES_TO_CHANGELIST: {
                MoveFilesToChangelistAction a = (MoveFilesToChangelistAction) action;
                ret.data
                        .putChangelistId("cl-id", a.getChangelistId())
                        .putFilePathList("files", a.getFiles());
                break;
            }
            case EDIT_CHANGELIST_DESCRIPTION: {
                EditChangelistAction a = (EditChangelistAction) action;
                ret.data
                        .putChangelistId("cl-id", a.getChangelistId())
                        .putString("comment", a.getComment());
                break;
            }
            case ADD_JOB_TO_CHANGELIST: {
                AddJobToChangelistAction a = (AddJobToChangelistAction) action;
                ret.data
                        .putChangelistId("cl-id", a.getChangelistId())
                        .putP4Job("job", a.getJob());
                break;
            }
            case REMOVE_JOB_FROM_CHANGELIST: {
                RemoveJobFromChangelistAction a = (RemoveJobFromChangelistAction) action;
                ret.data
                        .putChangelistId("cl-id", a.getChangelistId())
                        .putP4Job("job", a.getJob());
                break;
            }
            case CREATE_CHANGELIST: {
                CreateChangelistAction a = (CreateChangelistAction) action;
                ret.data
                        .putClientServerRef("ref", a.getClientServerRef())
                        .putString("comment", a.getComment());
                break;
            }
            case DELETE_CHANGELIST:
                ret.data
                        .putChangelistId("cl-id", ((DeleteChangelistAction) action).getChangelistId());
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


    @NotNull
    private static P4CommandRunner.ServerAction<?> readServerAction(@NotNull State state)
            throws PrimitiveMap.UnmarshalException {
        if (state.serverActionCmd == null) {
            throw new IllegalArgumentException("not a server action");
        }
        switch (state.serverActionCmd) {
            case CREATE_JOB:
                return new CreateJobAction(
                        state.actionId,
                        state.data.getP4Job("job"));
            case LOGIN:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
        }
        throw new IllegalArgumentException("Unknown server cmd " + state.serverActionCmd);
    }


    @NotNull
    static State getState(String sourceId, @NotNull P4CommandRunner.ServerAction<?> action) {
        State ret = new State();
        ret.sourceId = sourceId;
        ret.actionId = action.getActionId();
        ret.serverActionCmd = action.getCmd();
        ret.clientActionCmd = null;
        ret.data = new PrimitiveMap();
        switch (action.getCmd()) {
            case CREATE_JOB:
                ret.data.putP4Job("job", ((CreateJobAction) action).getJob());
                break;
            case LOGIN:
                throw new IllegalArgumentException("Should not attempt to store a submit action");
            default:
                throw new IllegalArgumentException("Unknown server cmd " + action.getCmd());
        }
        return ret;
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

    public static String getSourceId(ClientConfig config) {
        return getSourceId(config.getClientServerRef());
    }


    public static String getSourceId(ClientServerRef config) {
        return "client:" + config.toString();
    }


    public static String getSourceId(ServerConfig config) {
        return getSourceId(config.getServerName());
    }


    public static String getSourceId(P4ServerName config) {
        return "server:" + config.getUrl();
    }

}
