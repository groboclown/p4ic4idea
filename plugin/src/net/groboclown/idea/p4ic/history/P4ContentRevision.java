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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-deleted, submitted version of the file.
 */
public class P4ContentRevision implements ContentRevision {
    private final Project myProject;
    private final P4FileInfo p4file;
    private final VcsRevisionNumber.Int rev;

    @Nullable
    private final P4FileInfo.ClientAction action;

    public P4ContentRevision(@NotNull Project myProject, @NotNull P4FileInfo p4file, @NotNull VcsRevisionNumber.Int rev) {
        this.myProject = myProject;
        this.p4file = p4file;
        this.rev = rev;
        action = null;
    }

    public P4ContentRevision(@NotNull Project myProject, @NotNull P4FileInfo p4file) {
        this.myProject = myProject;
        this.p4file = p4file;
        this.rev = new VcsRevisionNumber.Int(p4file.getHeadRev());
        action = null;
    }

    public P4ContentRevision(@NotNull Project myProject, @NotNull P4FileInfo p4file, IFileRevisionData data) {
        this.myProject = myProject;
        this.p4file = p4file;
        this.action = P4FileInfo.fromAction(data.getAction());
        this.rev = new VcsRevisionNumber.Int(data.getRevision());
    }

    public P4ContentRevision(Project project, P4FileInfo p4file, int rev) {
        this(project, p4file, new VcsRevisionNumber.Int(rev));
    }

    @Nullable
    @Override
    public String getContent() throws VcsException {
        // This can run in the EDT!
        Client client = P4Vcs.getInstance(myProject).getClientFor(p4file.getPath());
        if (client == null) {
            throw new P4FileException(P4Bundle.message("error.filespec.no-client", p4file));
        }
        return client.getServer().loadFileAsString(p4file, rev.getValue());
    }

    @NotNull
    @Override
    public FilePath getFile() {
        return p4file.getPath();
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return rev;
    }
}
