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

package net.groboclown.p4.server.api.commands.changelist;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.AbstractAction;
import org.jetbrains.annotations.NotNull;

public class CreateChangelistAction extends AbstractAction
        implements P4CommandRunner.ClientAction<CreateChangelistResult> {
    private final String actionId;
    private final ClientServerRef ref;
    private final String comment;
    private final String localChangelistId;

    public CreateChangelistAction(@NotNull ClientServerRef ref, @NotNull String comment,
            @NotNull String localChangelistId) {
        this(createActionId(CreateChangelistAction.class), ref, comment, localChangelistId);
    }

    public CreateChangelistAction(@NotNull String actionId, @NotNull ClientServerRef ref, @NotNull String comment,
            @NotNull String localChangelistId) {
        this.actionId = actionId;
        this.ref = ref;
        this.comment = comment;
        this.localChangelistId = localChangelistId;
    }

    @NotNull
    @Override
    public Class<? extends CreateChangelistResult> getResultType() {
        return CreateChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.CREATE_CHANGELIST;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    @NotNull
    public String getComment() {
        return comment;
    }

    @NotNull
    public ClientServerRef getClientServerRef() {
        return ref;
    }

    @NotNull
    public String getLocalChangelistId() {
        return localChangelistId;
    }
    @NotNull
    @Override
    public String[] getDisplayParameters() {
        String p = comment;
        if (comment.length() > 25) {
            p = comment.substring(0, 22) + "...";
        }
        return new String[] { p };
    }

}
