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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An extension to the VcsRevisionNumber that allows for the revisions to
 * spread into the past history of the file across branches, rather than
 * limited to the current file depot location.
 */
public class P4RevisionNumber implements VcsRevisionNumber {

    public enum RevType {
        HEAD() {
            public int getRev(IExtendedFileSpec info) {
                return info.getHeadRev();
            }
        },
        HAVE() {
            public int getRev(IExtendedFileSpec info) {
                return info.getHaveRev();
            }
        };

        public abstract int getRev(IExtendedFileSpec info);
    }


    private final FilePath baseFile;
    private final String depotPath;
    private final int rev;
    private final int changelist;
    private final boolean showDepotPath;

    public P4RevisionNumber(@NotNull FilePath baseFile, @Nullable String requestedDepotPath, @Nullable final String depotPath, final int rev) {
        if (rev < 0) {
            throw new IllegalArgumentException(P4Bundle.message("exception.revision.number.bad", rev));
        }
        this.baseFile = baseFile;
        this.depotPath = depotPath;
        this.rev = rev;
        this.changelist = -1;
        this.showDepotPath = doShowDepotPath(requestedDepotPath, this.depotPath);
    }

    public P4RevisionNumber(@NotNull FilePath baseFile, @Nullable String requestedDepotPath, @NotNull final IExtendedFileSpec info,
            @NotNull RevType revType) {
        this.baseFile = baseFile;
        this.depotPath = info.getDepotPathString();
        this.rev = revType.getRev(info);

        // There is no way to really tell the changelist this belongs to without performing a file history search.
        this.changelist = -1;

        this.showDepotPath = doShowDepotPath(requestedDepotPath, this.depotPath);
    }

    public P4RevisionNumber(@NotNull FilePath baseFile, @Nullable String requestedDepotPath, @NotNull final IFileRevisionData rev) {
        this.baseFile = baseFile;
        this.depotPath = rev.getDepotFileName();
        this.rev = rev.getRevision();
        this.changelist = rev.getChangelistId();
        this.showDepotPath = doShowDepotPath(requestedDepotPath, this.depotPath);
    }

    public P4RevisionNumber(@NotNull FilePath baseFile, @Nullable String requestedDepotPath, @NotNull final IFileAnnotation ann) {
        this.baseFile = baseFile;
        this.depotPath = ann.getDepotPath();
        this.rev = ann.getUpper();
        this.changelist = -1;
        this.showDepotPath = doShowDepotPath(requestedDepotPath, this.depotPath);
    }

    private static boolean doShowDepotPath(@Nullable String requested, @Nullable String actual) {
        return requested != null && ! requested.equals(actual);
    }


    @Nullable
    public String getDepotPath() {
        return depotPath;
    }


    public int getRev() {
        return rev;
    }


    public int getChangelist() {
        return changelist;
    }


    @Nullable
    public String loadContentAsString(@NotNull P4Server server, @NotNull FilePath alternate)
            throws VcsException, InterruptedException {
        if (rev < 0) {
            return null;
        }
        if (depotPath == null) {
            return server.loadFileAsStringOnline(alternate, rev);
        }
        try {
            return server.loadFileAsStringOnline(baseFile, getFileSpec());
        } catch (P4FileException e) {
            // something went wrong with the spec conversion.  Try again.
            return server.loadFileAsStringOnline(alternate, rev);
        }
    }


    @Nullable
    public byte[] loadContentAsBytes(@NotNull P4Server server, @Nullable FilePath alternate)
            throws VcsException, InterruptedException {
        if (rev < 0) {
            return null;
        }
        if (depotPath == null) {
            if (alternate == null) {
                return null;
            }
            return server.loadFileAsBytesOnline(alternate, rev);
        }
        try {
            return server.loadFileAsBytesOnline(baseFile, getFileSpec());
        } catch (P4FileException e) {
            // something went wrong with the spec conversion.  Try again.

            if (alternate == null) {
                return null;
            }
            return server.loadFileAsBytesOnline(alternate, rev);
        }
    }


    @NotNull
    private IFileSpec getFileSpec() throws P4Exception {
        if (depotPath == null) {
            throw new P4FileException(P4Bundle.getString("exception.no-depot-path"));
        }
        return FileSpecUtil.getFromDepotPath(depotPath, rev);
    }


    @Override
    public String asString() {
        return (depotPath == null || ! showDepotPath)
                ? ('#' + Integer.toString(rev))
                : (depotPath + '#' + rev);
    }

    @Override
    public int compareTo(@NotNull final VcsRevisionNumber o) {
        if (o instanceof P4RevisionNumber) {
            P4RevisionNumber that = (P4RevisionNumber) o;
            if (this.changelist > 0 && that.changelist > 0) {
                return this.changelist - that.changelist;
            }
            if (this.changelist > 0) {
                // that.changelist <= 0
                // which means "that" is from the raw file history.
                if (this.depotPath != null && that.depotPath != null) {
                    return this.rev - that.rev;
                }
                return 1;
            }
            return this.rev - that.rev;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj != null && (obj instanceof VcsRevisionNumber) && compareTo((VcsRevisionNumber) obj) == 0);
    }

    @Override
    public int hashCode() {
        return rev;
    }
}
