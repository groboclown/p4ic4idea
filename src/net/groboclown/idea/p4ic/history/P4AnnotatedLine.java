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
package net.groboclown.idea.p4ic.history;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class P4AnnotatedLine {
    private final int lineNumber;
    private final P4FileInfo file;
    private final IFileAnnotation ann;

    @Nullable
    private final IFileRevisionData revisionData;

    public P4AnnotatedLine(int lineNumber, @NotNull P4FileInfo file, @NotNull IFileAnnotation ann, @Nullable IFileRevisionData data) {
        this.lineNumber = lineNumber;
        this.file = file;
        this.ann = ann;
        this.revisionData = data;
    }

    public VcsRevisionNumber.Int getRev() {
        return new VcsRevisionNumber.Int(ann.getUpper());
    }

    public String getAuthor() {
        return revisionData == null ? null : revisionData.getUserName();
    }

    public Date getDate() {
        return revisionData == null ? null : revisionData.getDate();
    }

    public String getComment() {
        return revisionData == null ? null : revisionData.getDescription();
    }

    public P4FileInfo getFile() {
        return file;
    }

    public IFileRevisionData getRevisionData() {
        return revisionData;
    }
}
