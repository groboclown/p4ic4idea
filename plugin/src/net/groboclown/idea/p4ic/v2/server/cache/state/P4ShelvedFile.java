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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.vcs.FileStatus;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

public class P4ShelvedFile {
    private final String depotPath;
    private final String localPath;
    private final FileStatus status;

    public P4ShelvedFile(@NotNull String depotPath, @NotNull String localPath, @NotNull FileStatus status) {
        this.depotPath = depotPath;
        this.localPath = localPath;
        this.status = status;
    }

    @NotNull
    public String getDepotPath() {
        return depotPath;
    }

    @NotNull
    public FileStatus getStatus() {
        return status;
    }

    public boolean isDeleted() {
        return P4Vcs.SHELVED_DELETED.getId().equals(status.getId());
    }

    public boolean isAdded() {
        return P4Vcs.SHELVED_ADDED.getId().equals(status.getId());
    }

    public boolean isEdited() {
        return P4Vcs.SHELVED_MODIFIED.getId().equals(status.getId());
    }

    @Override
    public int hashCode() {
        return depotPath.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (that == null || !(that.getClass().equals(P4ShelvedFile.class))) {
            return false;
        }
        return ((P4ShelvedFile) that).depotPath.equals(depotPath);
    }

    public String getLocalPath() {
        return localPath;
    }
}
