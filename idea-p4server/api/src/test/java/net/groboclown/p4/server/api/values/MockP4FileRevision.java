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

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// supposed to be immutable, but it's for testing...
public class MockP4FileRevision implements P4FileRevision {
    private P4RemoteFile file;
    private P4ChangelistId changelist;
    private P4Revision revision;
    private P4FileAction action;
    private P4FileType fileType;
    private P4RemoteFile integratedFrom;

    @NotNull
    @Override
    public P4RemoteFile getFile() {
        return file;
    }

    public MockP4FileRevision withFile(String s) {
        file = new MockP4RemoteFile(s);
        return this;
    }

    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return changelist;
    }

    public MockP4FileRevision withChangelistId(ClientServerRef ref, int id) {
        changelist = new MockP4ChangelistId(ref, id);
        return this;
    }

    public MockP4FileRevision withChangelistId(P4ChangelistId id) {
        changelist = id;
        return this;
    }

    @NotNull
    @Override
    public P4Revision getRevision() {
        return revision;
    }

    public MockP4FileRevision withRevision(int rev) {
        revision = new P4Revision(rev);
        return this;
    }

    @NotNull
    @Override
    public P4FileAction getFileAction() {
        return action;
    }

    public MockP4FileRevision withFileAction(P4FileAction action) {
        this.action = action;
        return this;
    }

    @NotNull
    @Override
    public P4FileType getFileType() {
        return fileType;
    }

    public MockP4FileRevision withFileType(P4FileType f) {
        fileType = f;
        return this;
    }

    @Nullable
    @Override
    public P4RemoteFile getIntegratedFrom() {
        return integratedFrom;
    }

    public MockP4FileRevision withIntegratedFrom(String s) {
        if (s == null) {
            integratedFrom = null;
        } else {
            integratedFrom = new MockP4RemoteFile(s);
        }
        return this;
    }

    @Nullable
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return revision;
    }
}
