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
package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-deleted, submitted version of the file.
 */
public class P4CurrentContentRevision implements ContentRevision {
    private final P4Vcs vcs;
    private final FilePath filePath;
    private final P4CurrentRevisionNumber rev;
    private final P4FileAction fileAction;

    @Deprecated
    public P4CurrentContentRevision(@NotNull Project project, @NotNull P4FileAction file) {
        this.vcs = P4Vcs.getInstance(project);
        this.fileAction = file;
        this.filePath = file.getFile();
        if (this.filePath == null) {
            throw new IllegalArgumentException(file + " has null file path");
        }
        this.rev = new P4CurrentRevisionNumber(file);
    }

    @Nullable
    @Override
    public String getContent() throws VcsException {
        // This can run in the EDT!

        try {
            final P4Server server = vcs.getP4ServerFor(filePath);
            if (server != null) {
                return rev.loadContentAsString(server, filePath);
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    @NotNull
    @Override
    public FilePath getFile() {
        return filePath;
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return rev;
    }
}
