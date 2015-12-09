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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-deleted, submitted version of the file.
 */
public class P4CurrentContentRevision implements ContentRevision {
    private static final Logger LOG = Logger.getInstance(P4CurrentContentRevision.class);

    private final P4Vcs vcs;
    private final FilePath filePath;

    // TODO Just as the P4CurrentRevisionNumber needs a better implementation,
    // this too will need to be improved.
    @Deprecated
    public P4CurrentContentRevision(@NotNull Project project, @NotNull P4FileAction file) {
        this.vcs = P4Vcs.getInstance(project);
        this.filePath = file.getFile();
        if (this.filePath == null) {
            throw new IllegalArgumentException(file + " has null file path");
        }
    }

    @Nullable
    @Override
    public String getContent() throws VcsException {
        // This can run in the EDT!

        if (vcs.getProject().isDisposed()) {
            throw new P4Exception(P4Bundle.message("exception.disposed"));
        }

        try {
            final P4Server server = vcs.getP4ServerFor(filePath);
            if (server != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Extracting content for " + filePath + " from " + server.getClientServerId());
                }
                // Note: have revision, rather than head revision
                return server.loadFileAsStringOnline(filePath, IFileSpec.HAVE_REVISION);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No server found for " + filePath + "; cannot load contents");
                }
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
        return HAVE_REV;
    }

    @Override
    public String toString() {
        return "P4CurrentContentRevision: " + filePath;
    }


    private static final CurrentRevisionNumber HAVE_REV = new CurrentRevisionNumber();

    public static final class CurrentRevisionNumber implements VcsRevisionNumber {

        @Override
        public String asString() {
            return "#" + IFileSpec.HAVE_REVISION_STRING;
        }

        @Override
        public int compareTo(final VcsRevisionNumber o) {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            return o.getClass().equals(getClass());
        }
    }
}
