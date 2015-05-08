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
package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.history.P4AnnotatedLine;
import net.groboclown.idea.p4ic.server.FileInfoCache;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CancellationException;

public class AnnotateFileTask extends ServerTask<List<P4AnnotatedLine>> {
    private static final Logger LOG = Logger.getInstance(AnnotateFileTask.class);

    private final Project project;
    private final VirtualFile file;
    private final String revStr;
    private final FileInfoCache fileInfoCache;

    public AnnotateFileTask(@NotNull Project project, @NotNull VirtualFile file, @NotNull String revStr, @NotNull FileInfoCache fileInfoCache) {
        this.project = project;
        this.file = file;
        this.revStr = revStr;
        this.fileInfoCache = fileInfoCache;
    }


    @Override
    public List<P4AnnotatedLine> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        List<IFileSpec> specs = FileSpecUtil.getFromVirtualFilesAt(Collections.singletonList(file), revStr, false);
        if (specs.isEmpty()) {
            return Collections.emptyList();
        }

        // This is supposed to return a list of line-by-line annotations for
        // the input file.  We need to then cross-reference the actual file
        // revision.
        List<IFileAnnotation> annotations = exec.getAnnotationsFor(project, specs);
        Map<String, IFileRevisionData> revisions = new HashMap<String, IFileRevisionData>();
        List<P4AnnotatedLine> ret = new ArrayList<P4AnnotatedLine>(annotations.size());
        Map<String, P4FileInfo> fileInfo = new HashMap<String, P4FileInfo>();

        int lineNumber = 0;
        for (IFileAnnotation ann: annotations) {
            // It will most always be different
            //if (ann.getUpper() != ann.getLower()) {
            //    LOG.info("upper/lower response: " + ann.getUpper() + "/" + ann.getLower());
            //}
            if (ann.getDepotPath() == null) {
                LOG.info("Annotation encountered null depot path for line " + lineNumber);
                continue;
            }
            P4FileInfo p4file = fileInfo.get(ann.getDepotPath());
            if (p4file == null) {
                // depot path, so it's already escaped
                List<P4FileInfo> p4files = exec.loadFileInfo(project,
                        FileSpecBuilder.makeFileSpecList(ann.getDepotPath()), fileInfoCache);
                if (p4files.size() != 1) {
                    throw new P4FileException(P4Bundle.message("error.annotate.multiple-files", ann.getDepotPath(), p4files));
                }
                p4file = p4files.get(0);
                fileInfo.put(ann.getDepotPath(), p4file);
            }

            if (ann.getUpper() > 0) {
                String depotRev = ann.getDepotPath() + '#' + ann.getUpper();
                IFileRevisionData data = revisions.get(depotRev);
                if (ann.getUpper() > 0 && data == null) {
                    data = getHistoryFor(exec, depotRev);
                    revisions.put(depotRev, data);
                }
                ret.add(new P4AnnotatedLine(lineNumber++, p4file, ann, data));
            } else if (ann.getUpper() == 0) {
                ret.add(new P4AnnotatedLine(lineNumber++, p4file, ann, null));
                //LOG.info("deleted file");
            } else {
                ret.add(new P4AnnotatedLine(lineNumber++, p4file, ann, null));
                //LOG.info("current revision");
            }
        }
        return ret;
    }


    private IFileRevisionData getHistoryFor(@NotNull P4Exec exec, @NotNull String depotRev) throws VcsException {
        // The "depotRev" came from a Perforce named depot file,
        // so it is already escaped.  Therefore it's okay to use
        // FileSpecBuilder.
        List<IFileSpec> depotFiles = FileSpecBuilder.makeFileSpecList(depotRev);
        Map<IFileSpec, List<IFileRevisionData>> history =
                exec.getRevisionHistory(project, depotFiles, 1);
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> en: history.entrySet()) {
            List<IFileRevisionData> ret = en.getValue();
            // it can return empty values for a server message
            if (ret != null && ! ret.isEmpty()) {
                if (ret.size() != 1) {
                    throw new P4FileException(P4Bundle.message("error.annotate.revision", depotRev, ret));
                }
                return ret.get(0);
            }
        }
        throw new P4FileException(P4Bundle.message("error.annotate.no-revision", depotRev));
    }
}
