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
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddEditResult implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final FilePath file;
    private final boolean wasAdded;
    private final P4FileType fileType;
    private final P4ChangelistId changelistId;
    private final P4RemoteFile depotPath;

    public AddEditResult(ClientConfig config, @NotNull FilePath file, boolean wasAdded,
            @Nullable P4FileType fileType,
            @NotNull P4ChangelistId changelistId, @NotNull P4RemoteFile depotPath) {
        this.config = config;
        this.file = file;
        this.wasAdded = wasAdded;
        this.fileType = fileType;
        this.changelistId = changelistId;
        this.depotPath = depotPath;
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    @NotNull
    public FilePath getFile() {
        return file;
    }

    public boolean isAdd() {
        return wasAdded;
    }

    public boolean isEdit() {
        return !wasAdded;
    }

    @Nullable
    public P4FileType getFileType() {
        return fileType;
    }

    @NotNull
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @NotNull
    public P4RemoteFile getDepotPath() {
        return depotPath;
    }
}
