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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ColumnInfo;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.compat.HistoryCompat;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.ui.history.ChangelistDescriptionAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// TODO add support for VcsHistoryProviderEx, but that means making this conditional as part of the version
// dependency.
public class P4HistoryProvider implements VcsHistoryProvider {
    private static final Logger LOG = Logger.getInstance(P4HistoryProvider.class);

    private final Project project;
    private final P4Vcs vcs;
    private final DiffFromHistoryHandler diffHandler;

    public P4HistoryProvider(@NotNull Project project, @NotNull P4Vcs vcs) {
        this.project = project;
        this.vcs = vcs;
        this.diffHandler = HistoryCompat.getInstance().createDiffFromHistoryHandler();
    }

    @Override
    public VcsDependentHistoryComponents getUICustomization(VcsHistorySession session, JComponent forShortcutRegistration) {
        return VcsDependentHistoryComponents.createOnlyColumns(new ColumnInfo[0]);
    }

    @Override
    public AnAction[] getAdditionalActions(Runnable refresher) {
        return new AnAction[] {
                new ChangelistDescriptionAction()
        };
    }

    @Override
    public boolean isDateOmittable() {
        return false;
    }

    @Nullable
    @Override
    public String getHelpId() {
        return null;
    }

    @Nullable
    @Override
    public VcsHistorySession createSessionFor(FilePath filePath) throws VcsException {
        final List<VcsFileRevision> revisions = new ArrayList<VcsFileRevision>();
        revisions.addAll(getHistory(filePath, vcs));
        return createAppendableSession(filePath, revisions, null);
    }

    @Override
    public void reportAppendableHistory(FilePath path, VcsAppendableHistorySessionPartner partner) throws VcsException {
        final List<P4FileRevision> history = getHistory(path, vcs);
        if (history.size() == 0) return;

        final VcsAbstractHistorySession emptySession = createAppendableSession(path, Collections.<VcsFileRevision>emptyList(), null);
        partner.reportCreatedEmptySession(emptySession);

        for (P4FileRevision fileRevision : history) {
            partner.acceptRevision(fileRevision);
        }
        partner.finished();
    }

    @Override
    public boolean supportsHistoryForDirectories() {
        return false;
    }

    @Nullable
    @Override
    public DiffFromHistoryHandler getHistoryDiffHandler() {
        return diffHandler;
    }

    @Override
    public boolean canShowHistoryFor(@NotNull VirtualFile file) {
        return ! file.isDirectory() && file.isValid();
    }

    private VcsAbstractHistorySession createAppendableSession(final FilePath path, List<VcsFileRevision> revisions, @Nullable final VcsRevisionNumber number) {
        return new VcsAbstractHistorySession(revisions, number) {
            /**
             * This method should return actual value for current revision (it can be changed after submit for example)
             *
             * @return current file revision, null if file does not exist anymore
             */
            @Nullable
            protected VcsRevisionNumber calcCurrentRevisionNumber() {
                if (project.isDisposed()) {
                    return null;
                }
                try {
                    P4Server server = vcs.getP4ServerFor(path);
                    if (server != null) {
                        final Map<FilePath, IExtendedFileSpec> status =
                                server.getFileStatus(Collections.singletonList(path));
                        if (status == null || status.get(path) == null) {
                            LOG.info("No information for " + path);
                            return null;
                        }
                        final IExtendedFileSpec spec = status.get(path);
                        return new P4RevisionNumber(path, spec.getDepotPathString(), spec,
                                P4RevisionNumber.RevType.HEAD);
                    }
                    return null;
                } catch (InterruptedException e) {
                    LOG.warn(e);
                    return null;
                }
            }

            public HistoryAsTreeProvider getHistoryAsTreeProvider() {
                // All providers seem to just return null here
                return null;
            }

            @Override
            public VcsHistorySession copy() {
                return createAppendableSession(path, getRevisionList(), getCurrentRevisionNumber());
            }
        };
    }

    @NotNull
    private static List<P4FileRevision> getHistory(@Nullable FilePath filePath, @NotNull P4Vcs vcs) throws VcsException {
        if (filePath == null || !vcs.fileIsUnderVcs(filePath)) {
            return Collections.emptyList();
        }

        VcsConfiguration vcsConfiguration = VcsConfiguration.getInstance(vcs.getProject());
        int limit = vcsConfiguration.LIMIT_HISTORY ? vcsConfiguration.MAXIMUM_HISTORY_ROWS : -1;
        return getHistory(filePath, vcs, limit);
    }

    @NotNull
    private static List<P4FileRevision> getHistory(@NotNull FilePath filePath, @NotNull P4Vcs vcs, int limit)
            throws VcsException {
        try {
            P4Server server = vcs.getP4ServerFor(filePath);
            if (server != null) {
                final Map<FilePath, IExtendedFileSpec> specs =
                        server.getFileStatus(Collections.singletonList(filePath));
                if (specs == null || specs.get(filePath) == null) {
                    LOG.info("No file information for " + filePath);
                    return Collections.emptyList();
                }
                List<P4FileRevision> ret = server.getRevisionHistoryOnline(specs.get(filePath), limit);
                if (ret != null) {
                    return ret;
                }
                // fall through
            }
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
        return Collections.emptyList();
    }


    // Later than v136
    //@Override
    @Nullable
    public VcsFileRevision getLastRevision(FilePath filePath)
            throws VcsException {
        if (filePath == null || ! vcs.fileIsUnderVcs(filePath)) {
            return null;
        }

        List<P4FileRevision> history = getHistory(filePath, vcs, 1);
        if (history.isEmpty()) {
            return null;
        }
        return history.get(0);
    }

    // Later than v136
    // @Override
    public void reportAppendableHistory(@NotNull FilePath path, @Nullable VcsRevisionNumber startingRevision,
            @NotNull VcsAppendableHistorySessionPartner partner)
            throws VcsException {
        if (vcs.fileIsUnderVcs(path)) {
            try {
                List<P4FileRevision> history = getHistory(path, vcs);
                for (P4FileRevision p4FileRevision : history) {
                    if (isSameOrAfter(p4FileRevision, startingRevision)) {
                        partner.acceptRevision(p4FileRevision);
                    }
                }
            } catch (VcsException e) {
                partner.reportException(e);
            }
        }
        partner.finished();

    }

    private static boolean isSameOrAfter(@Nullable P4FileRevision p4Revision,
            @Nullable VcsRevisionNumber startingRevision) {
        if (p4Revision == null) {
            return false;
        }
        if (startingRevision == null) {
            return true;
        }
        if (startingRevision instanceof P4RevisionNumber) {
            P4RevisionNumber rn = (P4RevisionNumber) startingRevision;
            return p4Revision.getChangeListId() >= rn.getChangelist();
        }
        return p4Revision.getRevisionNumber().compareTo(startingRevision) >= 0;
    }
}
