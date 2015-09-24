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
package net.groboclown.idea.p4ic.v2.history;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-deleted, submitted version of the file.
 */
public class P4ContentRevision implements ContentRevision {
    private final Project project;
    private final FilePath filePath;
    private final P4RevisionNumber rev;
    private final P4FileAction fileAction;

    // FIXME needs a better implementation than this, to better record the revision number.
    @Deprecated
    public P4ContentRevision(@NotNull Project project, @NotNull P4FileAction file) {
        this.project = project;
        this.fileAction = file;
        this.filePath = file.getFile();
        if (this.filePath == null) {
            throw new IllegalArgumentException(file + " has null file path");
        }
        this.rev = new P4RevisionNumber(file);
    }

    @Nullable
    @Override
    public String getContent() throws VcsException {
        // This can run in the EDT!

        // FIXME implement
        throw new IllegalStateException("not implemented");
        // return rev.loadContentAsString(client, p4file);
    }

    @NotNull
    @Override
    public FilePath getFile() {
        // FIXME ensure in constructor that the file is not null
        return filePath;
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        // FIXME instantiate a correct object
        return rev;
    }
}
