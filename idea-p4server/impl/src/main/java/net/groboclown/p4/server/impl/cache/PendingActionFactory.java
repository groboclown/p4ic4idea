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

package net.groboclown.p4.server.impl.cache;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class PendingActionFactory {
    @NotNull
    public static <R extends P4CommandRunner.ClientResult> PendingAction create(
            @NotNull ClientServerRef config, @NotNull P4CommandRunner.ClientAction<R> action) {
        switch (action.getCmd()) {
            case MOVE_FILE:
                break;
            case ADD_EDIT_FILE:
                break;
            case DELETE_FILE:
                break;
            case REVERT_FILE:
                break;
            case MOVE_FILES_TO_CHANGELIST:
                break;
            case EDIT_CHANGELIST_DESCRIPTION:
                break;
            case ADD_JOB_TO_CHANGELIST:
                break;
            case REMOVE_JOB_FROM_CHANGELIST:
                break;
            case CREATE_CHANGELIST:
                break;
            case DELETE_CHANGELIST:
                break;
            case FETCH_FILES:
                break;
            case SUBMIT_CHANGELIST:
                break;
        }
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    public static <R extends P4CommandRunner.ServerResult> PendingAction create(
            @NotNull P4ServerName config, @NotNull P4CommandRunner.ServerAction<R> action) {
        switch (action.getCmd()) {
            case CREATE_JOB:
                break;
            case LOGIN:
                break;
        }
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    public static PendingAction read(@NotNull PendingAction.State pendingActionState) {
        // FIXME implement
        throw new IllegalStateException("not implemented");
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
