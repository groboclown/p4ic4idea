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

package net.groboclown.p4.server.api.commands.file;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.AbstractAction;
import net.groboclown.p4.server.api.commands.ActionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class RevertFileAction extends AbstractAction implements P4CommandRunner.ClientAction<RevertFileResult> {
    private final String actionId;
    private final FilePath file;
    private final boolean ifUnchanged;

    public RevertFileAction(@NotNull FilePath file, boolean ifUnchanged) {
        this(createActionId(RevertFileAction.class), file, ifUnchanged);
    }

    public RevertFileAction(@NotNull String actionId, @NotNull FilePath file, boolean ifUnchanged) {
        this.actionId = actionId;
        this.file = file;
        this.ifUnchanged = ifUnchanged;
    }

    @NotNull
    @Override
    public Class<? extends RevertFileResult> getResultType() {
        return RevertFileResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.REVERT_FILE;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public FilePath getFile() {
        return file;
    }

    public boolean isRevertOnlyIfUnchanged() {
        return ifUnchanged;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        if (isRevertOnlyIfUnchanged()) {
            // TODO message catalog
            return new String[] { "unchanged" };
        }
        return EMPTY;
    }

    @NotNull
    @Override
    public List<FilePath> getAffectedFiles() {
        return Collections.singletonList(file);
    }
}
