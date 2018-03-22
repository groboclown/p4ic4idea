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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.impl.generic.core.file.FileRevisionData;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class P4AnnotatedLine {
    private static final Logger LOG = Logger.getInstance(P4AnnotatedLine.class);


    private final FilePath baseFile;
    private final String depotPath;
    private final IFileAnnotation ann;
    private final int lineNumber;
    private final IFileRevisionData revisionData;

    public P4AnnotatedLine(@NotNull FilePath baseFile, int lineNumber, @NotNull IFileAnnotation ann, @NotNull IFileRevisionData data) {
        this.baseFile = baseFile;
        this.depotPath = ann.getDepotPath();
        this.ann = ann;
        this.revisionData = data;
        this.lineNumber = lineNumber;
    }

    @NotNull
    public P4RevisionNumber getRev() {
        return new P4RevisionNumber(baseFile, depotPath, ann);
    }

    public int getChangelist() {
        return revisionData.getChangelistId();
    }

    @Nullable
    public String getAuthor() {
        return revisionData.getUserName();
    }

    @Nullable
    public Date getDate() {
        return revisionData.getDate();
    }

    @Nullable
    public String getComment() {
        return revisionData.getDescription();
    }

    @Nullable
    public IFileRevisionData getRevisionData() {
        return revisionData;
    }

    @NotNull
    public String getDepotPath() {
        return depotPath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getRevNumber() {
        return revisionData.getRevision();
    }

    public static List<P4AnnotatedLine> loadAnnotatedLines(@NotNull P4Exec2 exec,
            @NotNull FilePath baseFile, @NotNull List<IFileAnnotation> annotations) throws VcsException {
        Map<String, IFileRevisionData> revisions = new HashMap<String, IFileRevisionData>();
        List<P4AnnotatedLine> ret = new ArrayList<P4AnnotatedLine>(annotations.size());
        Map<String, IExtendedFileSpec> fileSpecs = new HashMap<String, IExtendedFileSpec>();

        // First pass: get all the depot paths and load them up into specs.
        // This way, we only have a single call to get their status.
        Set<IFileSpec> allDepotPaths = new HashSet<IFileSpec>();
        for (IFileAnnotation ann : annotations) {
            // Depot path is already escaped, so keep it that way.
            allDepotPaths.add(FileSpecUtil.getAlreadyEscapedSpec(ann.getDepotPath()));
        }
        final List<IExtendedFileSpec> allSpecs = exec.getFileStatus(new ArrayList<IFileSpec>(allDepotPaths));
        for (IExtendedFileSpec spec : allSpecs) {
            fileSpecs.put(spec.getDepotPathString(), spec);
        }

        int lineNumber = 0;
        for (IFileAnnotation ann : annotations) {
            if (ann.getDepotPath() == null) {
                LOG.info("Annotation encountered null depot path for line " + lineNumber);
                continue;
            }

            IExtendedFileSpec spec = fileSpecs.get(ann.getDepotPath());
            if (spec == null) {
                LOG.error("should have already found path " + ann.getDepotPath() +
                        "; but instead found paths " + fileSpecs.keySet());
                return Collections.emptyList();
            }

            // See bug #86
            // ann.getUpper() - return the most recent version of the file to
            //      contain the line.  If the line is still present in the file,
            //      it will return the highest revision number.  In most cases,
            //      this is NOT what we want.
            // ann.getLower() - return the first revision of the file that contained
            //      the line's value.

            int blameRev = ann.getLower();
            if (blameRev > 0) {
                String depotRev = ann.getDepotPath() + '#' + blameRev;
                IFileRevisionData data = revisions.get(depotRev);
                if (blameRev > 0 && data == null) {
                    data = getHistoryFor(exec, depotRev);
                    revisions.put(depotRev, data);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Annotating line " + (lineNumber + 1) + " from " + depotRev + " || " +
                            baseFile + " with " + data.getRevision() + "//" + data.getDate());
                }
                ret.add(new P4AnnotatedLine(baseFile, lineNumber++, ann, data));
            } else if (blameRev == 0) {
                LOG.info("Annotated line for " + ann.getDepotPath() + '@' + lineNumber + " [" +
                        ann.getLine() + "] has a 0 revision number");
                // deleted file; this should never happen
                ret.add(new P4AnnotatedLine(baseFile, lineNumber++, ann, getDeletedHistoryFor(spec)));
            } else {
                // local revision; this may happen relatively frequently
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Annotating line " + (lineNumber + 1) + " from local change");
                }
                ret.add(new P4AnnotatedLine(baseFile, lineNumber++, ann, getLocalHistoryFor(exec, spec)));
            }
        }
        return ret;
    }

    @Nullable
    private static IFileRevisionData getDeletedHistoryFor(@NotNull IExtendedFileSpec depotRev) {
        return new FileRevisionData(
                0, 0,
                depotRev.getAction(),
                depotRev.getDate(),
                depotRev.getUserName(),
                depotRev.getFileType(),
                depotRev.getDesc(),
                depotRev.getDepotPathString(),
                depotRev.getClientName());
    }


    @Nullable
    private static IFileRevisionData getLocalHistoryFor(final P4Exec2 exec, @NotNull IExtendedFileSpec depotRev) {
        return new FileRevisionData(
                -1, -1,
                FileAction.EDIT,
                new Date(), // IntelliJ probably has a better date value in its local history
                exec.getUsername(),
                depotRev.getFileType(),
                depotRev.getDesc(), // Should use active Idea changelist name
                depotRev.getDepotPathString(),
                exec.getClientName());
    }


    @NotNull
    private static IFileRevisionData getHistoryFor(@NotNull P4Exec2 exec, @NotNull String depotRev)
            throws VcsException {
        // The "depotRev" came from a Perforce named depot file,
        // so it is already escaped.  Therefore it's okay to use
        // getAlreadyEscapedSpec.
        IFileSpec depotFile = FileSpecUtil.getAlreadyEscapedSpec(depotRev);
        Map<IFileSpec, List<IFileRevisionData>> history = exec.getRevisionHistory(
                Collections.singletonList(depotFile), 1);
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> en : history.entrySet()) {
            List<IFileRevisionData> ret = en.getValue();
            // it can return empty values for a server message
            if (ret != null && !ret.isEmpty()) {
                if (ret.size() != 1) {
                    LOG.warn("unexpected revision data for " + depotRev + ": " + ret);
                    throw new P4FileException(P4Bundle.message("error.annotate.revision", depotRev, ret));
                }
                return ret.get(0);
            }
        }
        LOG.warn("No revision for " + depotRev);
        throw new P4FileException(P4Bundle.message("error.annotate.no-revision", depotRev));
    }

}
