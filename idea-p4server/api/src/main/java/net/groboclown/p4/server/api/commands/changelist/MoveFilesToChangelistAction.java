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

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.AbstractAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MoveFilesToChangelistAction extends AbstractAction implements P4CommandRunner.ClientAction<MoveFilesToChangelistResult> {
    private final String actionId;
    private final P4ChangelistId changelistId;
    private final List<FilePath> files;

    public MoveFilesToChangelistAction(@NotNull P4ChangelistId changelistId, @NotNull Collection<FilePath> files) {
        this(createActionId(MoveFilesToChangelistAction.class), changelistId, files);
    }

    public MoveFilesToChangelistAction(@NotNull String actionId, @NotNull P4ChangelistId changelistId,
            @NotNull Collection<FilePath> files) {
        assert !files.isEmpty(): "list of files to move is empty";
        this.actionId = actionId;
        this.changelistId = changelistId;
        this.files = new ArrayList<>(files);
    }

    @NotNull
    @Override
    public Class<? extends MoveFilesToChangelistResult> getResultType() {
        return MoveFilesToChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.MOVE_FILES_TO_CHANGELIST;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public List<FilePath> getFiles() {
        return files;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        return new String[] { changeId(changelistId) };
    }


    @NotNull
    @Override
    public List<FilePath> getAffectedFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Intended to find a directory to use as the "working directory" for the path.  The returned path
     * is just a parent directory to one of the files in the move request.  The primary idea is finding
     * a base directory for the server to use as the root of the client workspace; with an AltRoot definition,
     * the server needs this, or it will generate error messages if the given files are not under the client
     * root used by the current working directory.
     *
     * @return a directory for the files.
     */
    @NotNull
    public File getCommonDir() {
        for (FilePath file : files) {
            FilePath parent = file.getParentPath();
            if (parent != null) {
                return parent.getIOFile();
            }
        }
        throw new RuntimeException("Unable to find a parent directory for " + files);
    }
}
