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
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.values.P4AnnotatedLine;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class P4AnnotatedLineImpl implements P4AnnotatedLine {
    private final FilePath baseFile;
    private final P4RemoteFile depotPath;
    private final IFileAnnotation ann;
    private final int lineNumber;
    private final IFileRevisionData revisionData;
    private final P4ChangelistId changelistId;
    private final P4FileRevision rev;

    public P4AnnotatedLineImpl(@NotNull ClientServerRef ref, @NotNull FilePath baseFile, int lineNumber,
            @NotNull IFileAnnotation ann,
            @NotNull IFileRevisionData data) {
        this.baseFile = baseFile;
        this.depotPath = new P4RemoteFileImpl(ann);
        this.ann = ann;
        this.revisionData = data;
        this.lineNumber = lineNumber;
        this.changelistId = new P4ChangelistIdImpl(revisionData.getChangelistId(), ref);
        this.rev = new P4FileRevisionImpl(ref, data);
    }

    @Override
    @NotNull
    public P4FileRevision getRev() {
        return rev;
    }

    @Override
    public P4ChangelistId getChangelist() {
        return changelistId;
    }

    @Override
    @Nullable
    public String getAuthor() {
        return revisionData.getUserName();
    }

    @Override
    @Nullable
    public Date getDate() {
        return revisionData.getDate();
    }

    @Override
    @Nullable
    public String getComment() {
        return revisionData.getDescription();
    }

    @Override
    @NotNull
    public IFileRevisionData getRevisionData() {
        return revisionData;
    }

    @Override
    @NotNull
    public P4RemoteFile getDepotPath() {
        return depotPath;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getRevNumber() {
        return revisionData.getRevision();
    }
}
