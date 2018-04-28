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
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class P4AnnotationProvider implements AnnotationProvider {
    private final P4Vcs vcs;

    public P4AnnotationProvider(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }

    @NotNull
    @Override
    public FileAnnotation annotate(@NotNull VirtualFile file) throws VcsException {
        // Use the "have" revision, not the "head" revision
        try {
            final P4Server server = vcs.getP4ServerFor(file);
            if (server == null) {
                throw new P4InvalidConfigException(P4Bundle.message("error.filespec.no-client", file));
            }
            final FilePath filePath = FilePathUtil.getFilePath(file);
            final Map<FilePath, IExtendedFileSpec> statusMap =
                    server.getFileStatus(Collections.singletonList(filePath));
            if (statusMap == null) {
                throw new P4DisconnectedException(P4Bundle.message("exception.working-offline"));
            }
            final IExtendedFileSpec spec = statusMap.get(filePath);
            if (spec == null) {
                throw new P4FileException(file);
            }
            P4RevisionNumber rev = new P4RevisionNumber(filePath, spec.getDepotPathString(), spec,
                    P4RevisionNumber.RevType.HAVE);
            String contents = rev.loadContentAsString(server, filePath);
            if (contents == null) {
                // TODO right way to handle this?
                contents = "";
            }
            return createAnnotation(server, file, server.getAnnotationsForOnline(filePath, spec, spec.getHaveRev()),
                    rev, contents);

        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    @NotNull
    @Override
    public FileAnnotation annotate(@NotNull VirtualFile file, VcsFileRevision revision) throws VcsException {
        FilePath filePath = VcsUtil.getFilePath(file);
        P4Server server;
        try {
            server = vcs.getP4ServerFor(file);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
        if (server == null) {
            throw new P4InvalidConfigException(P4Bundle.message("error.filespec.no-client", file));
        }
        VcsRevisionNumber vcsRev = revision.getRevisionNumber();
        if (!(vcsRev instanceof P4RevisionNumber)) {
            throw new P4Exception(P4Bundle.message("error.diff.bad-revision", vcsRev));
        }
        P4RevisionNumber p4rev = (P4RevisionNumber) vcsRev;
        int revNumber = p4rev.getRev();
        String contents;
        try {
            contents = server.loadFileAsStringOnline(filePath, revNumber);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
        if (contents == null) {
            // TODO right way to handle this?
            contents = "";
        }
        IFileSpec annotatedSpec = FileSpecUtil.getOneSpecWithRev(filePath, revNumber);
        try {
            return createAnnotation(server, file, server.getAnnotationsForOnline(filePath, annotatedSpec, revNumber),
                    p4rev, contents);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    /**
     * Check whether the annotation retrieval is valid (or possible) for the
     * particular file revision (or version in the repository).
     *
     * @param rev File revision to be checked.
     * @return true if annotation it valid for the given revision.
     */
    @Override
    public boolean isAnnotationValid(@NotNull VcsFileRevision rev) {
        if (!(rev instanceof P4FileRevision)) {
            return false;
        }
        VcsRevisionNumber revNum = rev.getRevisionNumber();
        if (!(revNum instanceof P4RevisionNumber)) {
            return false;
        }
        return ((P4RevisionNumber) revNum).getRev() > 0;
    }


    private P4FileAnnotation createAnnotation(@NotNull P4Server server, @NotNull VirtualFile file,
            @NotNull List<P4AnnotatedLine> annList, @NotNull P4RevisionNumber fileRev, @NotNull String content)
            throws VcsException {
        return new P4FileAnnotation(vcs.getProject(), server.getClientServerId(), file, fileRev, annList, content);
    }
}
