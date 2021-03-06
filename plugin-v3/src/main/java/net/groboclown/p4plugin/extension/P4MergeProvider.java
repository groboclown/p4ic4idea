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

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.merge.MergeData;
import com.intellij.openapi.vcs.merge.MergeProvider2;
import com.intellij.openapi.vcs.merge.MergeSession;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class P4MergeProvider
        implements MergeProvider2 {
    @NotNull
    @Override
    public MergeSession createMergeSession(@NotNull List<VirtualFile> list) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public MergeData loadRevisions(@NotNull VirtualFile virtualFile)
            throws VcsException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void conflictResolvedForFile(@NotNull VirtualFile virtualFile) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean isBinary(@NotNull VirtualFile virtualFile) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }
}
