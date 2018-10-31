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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.repository.P4RepositoryLocation;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

class LocalRepositoryLocation implements P4RepositoryLocation {
    private final ClientServerRef ref;
    private final String clientName;
    private final FilePath path;

    LocalRepositoryLocation(@NotNull ClientServerRef ref, @NotNull String clientName, @NotNull FilePath path) {
        this.ref = ref;
        this.clientName = clientName;
        this.path = path;
    }

    @Override
    public String toPresentableString() {
        return path.getPath();
    }

    @Override
    public String getKey() {
        return clientName + '|' + path.getPath();
    }

    @Override
    public void onBeforeBatch()
            throws VcsException {

    }

    @Override
    public void onAfterBatch() {

    }

    @NotNull
    @Override
    public ClientServerRef getClientServerRef() {
        return ref;
    }

    @NotNull
    @Override
    public List<IFileSpec> getFileSpecs() {
        return FileSpecBuildUtil.escapedForFilePathsAnnotated(Collections.singleton(path), null, true);
    }
}
