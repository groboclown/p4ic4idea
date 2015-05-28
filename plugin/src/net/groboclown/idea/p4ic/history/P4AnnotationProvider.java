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
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4RevisionNumber;
import net.groboclown.idea.p4ic.extension.P4RevisionNumber.RevType;
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
            throw new P4InvalidConfigException(P4Bundle.message("error.filespec.no-client", file));
        }
        ServerExecutor exec = client.getServer();
        List<P4FileInfo> p4files = exec.getVirtualFileInfo(Collections.singletonList(file));
        if (p4files.size() != 1) {
            throw new P4FileException(P4Bundle.message("error.filespec.incorrect", file, p4files));
        }
        P4RevisionNumber rev = new P4RevisionNumber(p4files.get(0).getDepotPath(), p4files.get(0), RevType.HAVE);
        String contents = rev.loadContentAsString(client, p4files.get(0));
        if (contents == null) {
            // TODO right way to handle this?
            contents = "";
        }
        return createAnnotation(client, file, exec.getAnnotationsFor(file, p4files.get(0).getHaveRev()),
                rev, contents);
    }

    @Override
    public FileAnnotation annotate(VirtualFile file, VcsFileRevision revision) throws VcsException {
        FilePath filePath = VcsUtil.getFilePath(file);
        Client client = vcs.getClientFor(filePath);
        if (client == null) {
            throw new P4InvalidConfigException(P4Bundle.message("error.filespec.no-client", file));
        }
        VcsRevisionNumber rev = revision.getRevisionNumber();
        if (!(rev instanceof P4RevisionNumber)) {
            throw new P4Exception(P4Bundle.message("error.diff.bad-revision", rev));
        }
        ServerExecutor exec = client.getServer();

        // TODO look at how to crawl down into branching.
        String contents = exec.loadFileAsString(filePath,
                ((P4RevisionNumber) rev).getRev());
        return createAnnotation(client, file, exec.getAnnotationsFor(file,
                        ((P4RevisionNumber) rev).getRev()),
                (P4RevisionNumber) rev, contents);
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
        if (!(rev instanceof P4FileRevision)) {
            return false;
        }
        VcsRevisionNumber revNum = rev.getRevisionNumber();
        if (!(revNum instanceof P4RevisionNumber)) {
            return false;
        }
        return ((P4RevisionNumber) revNum).getRev() > 0;
    }


    private P4FileAnnotation createAnnotation(@NotNull Client client, @NotNull VirtualFile file,
            @NotNull List<P4AnnotatedLine> annList, @NotNull P4RevisionNumber fileRev, @NotNull String content)
            throws VcsException {
        return new P4FileAnnotation(vcs.getProject(), client, file, fileRev, annList, content);
    }
}
