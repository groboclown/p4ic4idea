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

    @Nullable
    private final IFileRevisionData revisionData;

    public P4AnnotatedLine(@NotNull FilePath baseFile, int lineNumber, @NotNull IFileAnnotation ann, @Nullable IFileRevisionData data) {
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

    @Nullable
    public String getAuthor() {
        return revisionData == null ? null : revisionData.getUserName();
    }

    @Nullable
    public Date getDate() {
        return revisionData == null ? null : revisionData.getDate();
    }

    @Nullable
    public String getComment() {
        return revisionData == null ? null : revisionData.getDescription();
    }

    @Nullable
    public IFileRevisionData getRevisionData() {
        return revisionData;
    }

    @NotNull
    public String getDepotPath() {
        return depotPath;
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
            // It will most always be different
            //if (ann.getUpper() != ann.getLower()) {
            //    LOG.info("upper/lower response: " + ann.getUpper() + "/" + ann.getLower());
            //}
            if (ann.getDepotPath() == null) {
                LOG.info("Annotation encountered null depot path for line " + lineNumber);
                continue;
            }

            IExtendedFileSpec spec = fileSpecs.get(ann.getDepotPath());
            if (spec == null) {
                throw new IllegalStateException("should have already found path " + ann.getDepotPath() +
                        "; but instead found paths " + fileSpecs.keySet());
            }

            if (ann.getUpper() > 0) {
                String depotRev = ann.getDepotPath() + '#' + ann.getUpper();
                IFileRevisionData data = revisions.get(depotRev);
                if (ann.getUpper() > 0 && data == null) {
                    data = getHistoryFor(exec, depotRev);
                    revisions.put(depotRev, data);
                }
                ret.add(new P4AnnotatedLine(baseFile, lineNumber++, ann, data));
            } else if (ann.getUpper() == 0) {
                // TODO this is the source of "null" rev data
                ret.add(new P4AnnotatedLine(baseFile, lineNumber++, ann, getUnknownHistoryFor(spec)));
                //LOG.info("deleted file");
            } else {
                ret.add(new P4AnnotatedLine(baseFile, lineNumber++, ann, getUnknownHistoryFor(spec)));
                //LOG.info("current revision");
            }
        }
        return ret;
    }


    @Nullable
    private static IFileRevisionData getUnknownHistoryFor(@NotNull IExtendedFileSpec depotRev) {
        // TODO this is the source of "null" rev data
        return null;
    }


    @NotNull
    private static IFileRevisionData getHistoryFor(@NotNull P4Exec2 exec, @NotNull String depotRev)
            throws VcsException {
        // The "depotRev" came from a Perforce named depot file,
        // so it is already escaped.  Therefore it's okay to use
        // FileSpecBuilder.
        List<IFileSpec> depotFiles = FileSpecBuilder.makeFileSpecList(depotRev);
        Map<IFileSpec, List<IFileRevisionData>> history = exec.getRevisionHistory(depotFiles, 1);
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> en : history.entrySet()) {
            List<IFileRevisionData> ret = en.getValue();
            // it can return empty values for a server message
            if (ret != null && !ret.isEmpty()) {
                if (ret.size() != 1) {
                    throw new P4FileException(P4Bundle.message("error.annotate.revision", depotRev, ret));
                }
                return ret.get(0);
            }
        }
        throw new P4FileException(P4Bundle.message("error.annotate.no-revision", depotRev));
    }
}
