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

package net.groboclown.p4.server.impl.connection.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileRevisionData;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.values.P4AnnotatedLine;
import net.groboclown.p4.server.api.values.P4FileAnnotation;
import net.groboclown.p4.server.impl.util.HandleFileSpecUtil;
import net.groboclown.p4.server.impl.values.P4AnnotatedLineImpl;
import net.groboclown.p4.server.impl.values.P4FileAnnotationImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileAnnotationParser {
    private static final Logger LOG = Logger.getInstance(FileAnnotationParser.class);

    // See bug #86
    // ann.getUpper() - return the most recent version of the file to
    //      contain the line.  If the line is still present in the file,
    //      it will return the highest revision number.  In most cases,
    //      this is NOT what we want.
    // ann.getLower() - return the first revision of the file that contained
    //      the line's value.


    public static List<IFileSpec> getRequiredHistorySpecs(@NotNull List<IFileAnnotation> annotations) {
        Set<String> revs = new HashSet<>();
        for (IFileAnnotation ann: annotations) {
            if (ann != null && ann.getDepotPath() != null) {
                int rev = ann.getLower();
                if (rev > 0) {
                    revs.add(ann.getDepotPath() + '#' + rev);
                }
            }
        }
        return FileSpecBuilder.makeFileSpecList(new ArrayList<>(revs));
    }


    public static P4FileAnnotation getFileAnnotation(
            @NotNull ClientServerRef ref, @NotNull String username,
            IExtendedFileSpec headSpec, @NotNull FilePath baseFile,
            @NotNull List<IFileAnnotation> annotations,
            @NotNull List<Pair<IFileSpec, IFileRevisionData>> revs) {
        List<P4AnnotatedLine> ret = new ArrayList<>(annotations.size());

        // Note: line numbers start at 0 for the IDE, and P4AnnotatedFileImpl requires the line number to match
        // the index in the array.
        for (IFileAnnotation ann : annotations) {
            int lineNumber = ret.size();
            if (ann.getDepotPath() == null) {
                LOG.info("Annotation encountered null depot path for line " + lineNumber);
                continue;
            }
            int blameRev = ann.getLower();
            IFileRevisionData data = getHistoryFor(ann, revs);
            // TODO adding deleted revs to this.  Is this right?
            if (data != null && blameRev >= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Annotated line for " + ann.getDepotPath() + '@' + lineNumber + " :: " +
                            data.getDepotFileName() + '#' + data.getRevision());
                }
                ret.add(new P4AnnotatedLineImpl(ref, baseFile, lineNumber, ann, data));
            } else {
                // local revision; this may happen relatively frequently
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Annotating line " + (lineNumber + 1) + " from local change");
                }
                ret.add(new P4AnnotatedLineImpl(ref, baseFile, lineNumber, ann,
                        createLocalHistoryFor(ref, username, headSpec)));
            }
        }
        return new P4FileAnnotationImpl(ret);
    }

    @Nullable
    private static IFileRevisionData getHistoryFor(@NotNull IFileAnnotation annotation,
            @NotNull List<Pair<IFileSpec, IFileRevisionData>> revs) {
        String depot = annotation.getDepotPath();
        int rev = annotation.getLower();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding match for " + depot + '#' + rev);
        }
        if (depot == null || rev < 0) {
            return null;
        }
        for (Pair<IFileSpec, IFileRevisionData> pair: revs) {
            // The FileSpec will not have revision # set.  Instead, check for the revision number in the revision data.
            if (LOG.isDebugEnabled()) {
                LOG.debug("- Comparing to " + HandleFileSpecUtil.getRawDepot(pair.first, true) + '#' + pair.second.getRevision());
            }
            if (depot.equals(HandleFileSpecUtil.getRawDepot(pair.first, true)) && rev == pair.second.getRevision()) {
                return pair.second;
            }
        }
        return null;
    }

    @NotNull
    private static IFileRevisionData createLocalHistoryFor(ClientServerRef ref,
            String username, @NotNull IExtendedFileSpec depotRev) {
        return new FileRevisionData(
                -1, -1,
                FileAction.EDIT,
                new Date(), // IntelliJ probably has a better date value in its local history
                username,
                depotRev.getFileType(),
                depotRev.getDesc(), // Should use active Idea changelist name
                depotRev.getDepotPathString(),
                ref.getClientName());
    }
}
