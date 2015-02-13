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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class P4AnnotationProvider implements AnnotationProvider {
    private final P4Vcs vcs;

    public P4AnnotationProvider(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public FileAnnotation annotate(VirtualFile file) throws VcsException {
        // Use the "have" revision, not the "head" revision
        Client client = vcs.getClientFor(VcsUtil.getFilePath(file));
        if (client == null) {
            throw new P4InvalidConfigException("no valid p4 config for " + file);
        }
        ServerExecutor exec = client.getServer();
        List<P4FileInfo> p4files = exec.getVirtualFileInfo(Collections.singletonList(file));
        if (p4files.size() != 1) {
            throw new P4FileException("incorrect file info for " + file + ": " + p4files);
        }
        String contents = exec.loadFileAsString(p4files.get(0), p4files.get(0).getHaveRev());
        return createAnnotation(file, exec.getAnnotationsFor(file, p4files.get(0).getHaveRev()),
                new VcsRevisionNumber.Int(p4files.get(0).getHaveRev()), contents);
    }

    @Override
    public FileAnnotation annotate(VirtualFile file, VcsFileRevision revision) throws VcsException {
        FilePath filePath = VcsUtil.getFilePath(file);
        Client client = vcs.getClientFor(filePath);
        if (client == null) {
            throw new P4InvalidConfigException("no valid p4 config for " + file);
        }
        VcsRevisionNumber rev = revision.getRevisionNumber();
        if (! (rev instanceof VcsRevisionNumber.Int)) {
            throw new P4Exception("Bad revision: " + rev);
        }
        ServerExecutor exec = client.getServer();
        String contents = exec.loadFileAsString(filePath,
                ((VcsRevisionNumber.Int) rev).getValue());
        return createAnnotation(file, exec.getAnnotationsFor(file,
                ((VcsRevisionNumber.Int) rev).getValue()),
                (VcsRevisionNumber.Int) rev, contents);
    }

    /**
     * Check whether the annotation retrieval is valid (or possible) for the
     * particular file revision (or version in the repository).
     *
     * @param rev File revision to be checked.
     * @return true if annotation it valid for the given revision.
     */
    @Override
    public boolean isAnnotationValid(VcsFileRevision rev) {
        if (! (rev instanceof P4FileRevision)) {
            return false;
        }
        VcsRevisionNumber revNum = rev.getRevisionNumber();
        if (! (revNum instanceof VcsRevisionNumber.Int)) {
            return false;
        }
        return ((VcsRevisionNumber.Int) revNum).getValue() > 0;
    }


    private P4FileAnnotation createAnnotation(@NotNull VirtualFile file, @NotNull List<P4AnnotatedLine> annList,
            @NotNull VcsRevisionNumber.Int fileRev, @NotNull String content)
            throws VcsException {
        return new P4FileAnnotation(vcs.getProject(), file, fileRev, annList, content);
    }
}
