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

package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.mock.VFFilePath;
import net.groboclown.p4.server.api.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Supposed to be immutable, but it's for testing...
public class MockP4LocalFile implements P4LocalFile {
    private P4RemoteFile depotPath;
    private FilePath filePath;
    private P4Revision have;
    private P4FileRevision head;
    private P4ChangelistId changelist;
    private P4FileAction action;
    private P4ResolveType resolveType;
    private P4FileType fileType;
    private P4RemoteFile integrateFrom;


    @Nullable
    @Override
    public P4RemoteFile getDepotPath() {
        return depotPath;
    }

    public MockP4LocalFile withDepotPath(String s) {
        depotPath = new MockP4RemoteFile(s);
        return this;
    }

    @NotNull
    @Override
    public FilePath getFilePath() {
        return filePath;
    }

    public MockP4LocalFile withFilePath(VirtualFile vf) {
        filePath = new VFFilePath(vf);
        return this;
    }

    @NotNull
    @Override
    public P4Revision getHaveRevision() {
        return have;
    }

    public MockP4LocalFile withHaveRevision(int rev) {
        have = new P4Revision(rev);
        return this;
    }

    @Nullable
    @Override
    public P4FileRevision getHeadFileRevision() {
        return head;
    }

    public MockP4LocalFile withDefaultHeadFileRevision() {
        head = new MockP4FileRevision()
                .withChangelistId(changelist)
                .withFile(depotPath.getDepotPath())
                .withFileAction(action)
                .withFileType(fileType)
                .withRevision(have.getValue());
        return this;
    }

    @Nullable
    @Override
    public P4ChangelistId getChangelistId() {
        return changelist;
    }

    public MockP4LocalFile withChangelistId(ClientServerRef ref, int id) {
        changelist = new MockP4ChangelistId(ref, id);
        return this;
    }

    @NotNull
    @Override
    public P4FileAction getFileAction() {
        return action;
    }

    public MockP4LocalFile withFileAction(P4FileAction a) {
        action = a;
        return this;
    }

    @NotNull
    @Override
    public P4ResolveType getResolveType() {
        return resolveType;
    }

    public MockP4LocalFile withResolveType(P4ResolveType t) {
        resolveType = t;
        return this;
    }

    @NotNull
    @Override
    public P4FileType getFileType() {
        return fileType;
    }

    public MockP4LocalFile withFileType(P4FileType t) {
        fileType = t;
        return this;
    }
    @Nullable
    @Override
    public P4RemoteFile getIntegrateFrom() {
        return integrateFrom;
    }

    public MockP4LocalFile withIntegrateFrom(P4RemoteFile f) {
        this.integrateFrom = f;
        return this;
    }
}
