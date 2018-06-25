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
package net.groboclown.p4plugin.extension;

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
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class P4AnnotationProvider
        implements AnnotationProvider {
    private final P4Vcs vcs;

    public P4AnnotationProvider(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }

    @NotNull
    @Override
    public FileAnnotation annotate(@NotNull VirtualFile file) throws VcsException {
        // Use the "have" revision, not the "head" revision
        // FIXME implement annotation
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public FileAnnotation annotate(@NotNull VirtualFile file, VcsFileRevision revision) throws VcsException {
        // FIXME implement annotation
        throw new IllegalStateException("not implemented");
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
        // FIXME implement is annotation valid.
        throw new IllegalStateException("not implemented");
    }
}
