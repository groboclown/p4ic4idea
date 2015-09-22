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
package net.groboclown.idea.p4ic.v2.server.history;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-deleted, submitted version of the file.
 */
public class P4ContentRevision implements ContentRevision {
    private final Project myProject;
    private final FilePath filePath;
    private final P4ClientFileMapping p4file;
    private final P4RevisionNumber rev;
    private final P4FileAction fileAction;

    public P4ContentRevision(@NotNull Project project, @NotNull P4FileAction file) {
        myProject = project;
        fileAction = file;
        filePath = file.getFile();
        // FIXME ensure filePath is not null

        // FIXME needs correct implementation.
        rev = null;
        //p4file = file.getP4File();
        p4file = null;
    }

    public P4ContentRevision(@NotNull Project project, @NotNull FilePath fp) {
        myProject = project;
        fileAction = null;
        filePath = fp;

        // FIXME needs correct implementation
        rev = null;
        p4file = null;
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
