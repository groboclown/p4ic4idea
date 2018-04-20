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

package net.groboclown.p4.server.config.server;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.FileAction;
import net.groboclown.idea.p4ic.v2.history.P4RevisionNumber;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import org.jetbrains.annotations.NotNull;

public class FileSyncResult {
    private final P4ClientFileMapping file;
    private final FileAction fileAction;
    private final P4RevisionNumber rev;

    public FileSyncResult(@NotNull P4ClientFileMapping file,
            @NotNull FileAction clientAction, final int revision) {
        assert file.getLocalFilePath() != null;
        this.file = file;
        this.fileAction = clientAction;
        this.rev = new P4RevisionNumber(file.getLocalFilePath(),
                file.getDepotPath(), file.getDepotPath(), revision);
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public FilePath getFilePath() {
        return file.getLocalFilePath();
    }



    @NotNull
    public FileAction getFileAction() {
        return fileAction;
    }


    @NotNull
    public P4RevisionNumber getRevisionNumber() {
        return rev;
    }
}
