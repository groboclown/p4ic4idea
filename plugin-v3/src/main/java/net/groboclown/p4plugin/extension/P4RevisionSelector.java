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

package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.diff.RevisionSelector;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.ui.revision.RevisionDialog;
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;
import org.jetbrains.annotations.Nullable;

public class P4RevisionSelector implements RevisionSelector {
    private static final Logger LOG = Logger.getInstance(P4RevisionSelector.class);
    private final P4Vcs vcs;

    public P4RevisionSelector(final P4Vcs vcs) {
        this.vcs = vcs;
    }

    @Nullable
    @Override
    public VcsRevisionNumber selectNumber(final VirtualFile file) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Selecting version for file " + file);
        }
        final P4FileRevision rev = RevisionDialog.requestRevision(vcs, file);
        if (rev == null) {
            return null;
        }
        return rev.getRevisionNumber();
    }

}
