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
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class ShelveFilesAction extends AbstractAction implements P4CommandRunner.ClientAction<ShelveFilesResult> {
    private final String actionId;
    private final P4ChangelistId changelistId;
    private final List<FilePath> files;

    public ShelveFilesAction(P4ChangelistId changelistId, List<FilePath> files) {
        this(createActionId(ShelveFilesAction.class), changelistId, files);
    }

    public ShelveFilesAction(String actionId, P4ChangelistId changelistId, List<FilePath> files) {
        this.actionId = actionId;
        this.changelistId = changelistId;
        this.files = files;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        return new String[] { changeId(changelistId) };
    }

    @NotNull
    @Override
    public Class<? extends ShelveFilesResult> getResultType() {
        return ShelveFilesResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.SHELVE_FILES;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public List<FilePath> getFiles() {
        return files;
    }

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
