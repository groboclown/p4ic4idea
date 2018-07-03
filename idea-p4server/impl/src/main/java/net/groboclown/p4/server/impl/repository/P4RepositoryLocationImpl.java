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

package net.groboclown.p4.server.impl.repository;

import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.repository.P4RepositoryLocation;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class P4RepositoryLocationImpl implements P4RepositoryLocation {
    private final ClientServerRef ref;
    private final P4RemoteFile depotPath;

    public P4RepositoryLocationImpl(@NotNull ClientServerRef ref, @NotNull P4RemoteFile depotPath) {
        this.ref = ref;
        this.depotPath = depotPath;
    }

    public P4RepositoryLocationImpl(@NotNull ClientServerRef ref, @NotNull P4FileRevision p4FileRevision) {
        this.ref = ref;
        this.depotPath = p4FileRevision.getFile();
    }

    @Override
    public String toPresentableString() {
        return depotPath.getDisplayName();
    }

    @Override
    public String getKey() {
        return depotPath.getDepotPath();
    }

    @Override
    public void onBeforeBatch()
            throws VcsException {
        // Do nothing
    }

    @Override
    public void onAfterBatch() {
        // Do nothing
    }

    @NotNull
    @Override
    public ClientServerRef getClientServerRef() {
        return ref;
    }

    @NotNull
    @Override
    public List<IFileSpec> getFileSpecs() {
        return FileSpecBuildUtil.escapedForRemoteFileRev(depotPath, -1);
    }
}
