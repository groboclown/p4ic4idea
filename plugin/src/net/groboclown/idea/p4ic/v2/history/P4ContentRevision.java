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
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * Represents a non-deleted, submitted version of the file.
 */
public class P4ContentRevision implements ContentRevision {
    private final Project myProject;
    private final P4RevisionNumber rev;
    private final FilePath file;
    private Reference<String> previous = null;

    public P4ContentRevision(@NotNull Project project, @NotNull FilePath file, @NotNull P4RevisionNumber rev) {
        this.myProject = project;
        this.file = file;
        this.rev = rev;
    }

    public P4ContentRevision(@NotNull Project project, @NotNull FilePath file, @NotNull IExtendedFileSpec p4file) {
        this.myProject = project;
        this.file = file;
        this.rev = new P4RevisionNumber(p4file.getDepotPathString(), p4file, P4RevisionNumber.RevType.HEAD);
    }

    public P4ContentRevision(@NotNull Project project, @NotNull FilePath file, @NotNull IExtendedFileSpec p4file, int rev) {
        this(project, file, new P4RevisionNumber(p4file.getDepotPathString(), p4file.getDepotPathString(), rev));
    }

    @Nullable
    @Override
    public String getContent() throws VcsException {
        // This can run in the EDT!
        if (myProject.isDisposed()) {
            throw new P4Exception(P4Bundle.message("exception.disposed"));
        }

        if (previous != null) {
            String ret = previous.get();
            if (ret != null) {
                return ret;
            }
            previous = null;
        }

        P4Server server;
        try {
            server = P4Vcs.getInstance(myProject).getP4ServerFor(file);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
        if (server == null) {
            throw new P4FileException(P4Bundle.message("error.filespec.no-client", file));
        }
        String ret = rev.loadContentAsString(server, file);
        if (ret == null) {
            // cache the null value as an empty string, so we
            // don't need to go through this again.
            ret = "";
        }
        previous = new SoftReference<String>(ret);
        return ret;
    }

    @NotNull
    @Override
    public FilePath getFile() {
        return file;
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return rev;
    }
}
