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

package net.groboclown.p4.server.impl.values;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileAnnotation;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.api.values.P4Revision;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class P4FileRevisionImpl
        implements P4FileRevision {
    private final P4RemoteFile remoteFile;

    public P4FileRevisionImpl(FilePath baseFile, P4RemoteFile depotPath, IFileAnnotation ann) {
        this.remoteFile = depotPath;
    }

    @NotNull
    @Override
    public P4RemoteFile getFile() {
        return null;
    }

    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return null;
    }

    @NotNull
    @Override
    public P4Revision getRevision() {
        return null;
    }

    @NotNull
    @Override
    public P4FileAction getFileAction() {
        return null;
    }

    @NotNull
    @Override
    public P4FileType getFileType() {
        return null;
    }

    @Nullable
    @Override
    public P4RemoteFile getIntegratedFrom() {
        return null;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return null;
    }
}
