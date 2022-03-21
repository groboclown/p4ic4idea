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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.DiffFromHistoryHandler;
import com.intellij.openapi.vcs.history.HistoryAsTreeProvider;
import com.intellij.openapi.vcs.history.VcsAbstractHistorySession;
import com.intellij.openapi.vcs.history.VcsAppendableHistorySessionPartner;
import com.intellij.openapi.vcs.history.VcsDependentHistoryComponents;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsHistoryUtil;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.dualView.DualViewColumnInfo;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.vcs.history.VcsHistoryProviderEx;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.HistoryMessageFormatter;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.impl.repository.P4HistoryVcsFileRevision;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.actions.ChangelistDescriptionAction;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.messages.HistoryMessageFormatterImpl;
import net.groboclown.p4plugin.util.HistoryContentLoaderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class P4HistoryProvider
        implements VcsHistoryProviderEx {
    private static final Logger LOG = Logger.getInstance(P4HistoryProvider.class);

    private final Project project;
    private final DiffFromHistoryHandler diffHandler = new P4DiffFromHistoryHandler();
    private final HistoryMessageFormatter formatter = new HistoryMessageFormatterImpl();
    private final HistoryContentLoader loader;

    P4HistoryProvider(@NotNull Project project) {
        this.project = project;
        this.loader = new HistoryContentLoaderImpl(project);
    }

    @Override
    public VcsDependentHistoryComponents getUICustomization(VcsHistorySession session, JComponent forShortcutRegistration) {
        return VcsDependentHistoryComponents.createOnlyColumns(ADDITIONAL_HISTORY_COLUMNS);
    }

    @Override
    public AnAction[] getAdditionalActions(Runnable refresher) {
        return new AnAction[] {
                new ChangelistDescriptionAction()
        };
    }

    @Override
    public boolean isDateOmittable() {
        // Show the date column.
        return false;
    }

    @Nullable
    @Override
    public String getHelpId() {
        // TODO add help
        return null;
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
        if (file.isDirectory() || !file.isValid()) {
            return false;
        }
        FilePath fp = VcsUtil.getFilePath(file);
        return fp != null && getRootFor(fp) != null;
    }

    @Nullable
    @Override
    public VcsHistorySession createSessionFor(FilePath filePath) throws VcsException {
        RootedClientConfig root = getRootFor(filePath);
        if (root == null) {
            LOG.info("Not in P4 project: " + filePath);
            return null;
        }

        try {
            List<VcsFileRevision> revisions = P4ServerComponent
                    .query(project, root.getClientConfig(),
                            new ListFileHistoryQuery(filePath, -1))
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)
                    .getRevisions(formatter, loader);
            return createAppendableSession(filePath, revisions, null);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    @Override
    public void reportAppendableHistory(FilePath path, VcsAppendableHistorySessionPartner partner) {
        partner.reportCreatedEmptySession(createAppendableSession(
                path, Collections.emptyList(), null));
        RootedClientConfig root = getRootFor(path);
        if (root == null) {
            LOG.info("File not under VCS: " + path);
            // TODO bundle message
            partner.reportException(new VcsException("File not under VCS: " + path));
            return;
        }

        // Async operation.
        getHistory(root, path, -1)
                .whenCompleted((r) ->
                    r.getRevisions(formatter, loader).forEach(partner::acceptRevision))
                .whenServerError((e) -> {
                    LOG.warn(e);
                    partner.reportException(e);
                });
    }

    @Override
    @Nullable
    public VcsFileRevision getLastRevision(FilePath filePath)
            throws VcsException {
        RootedClientConfig root = getRootFor(filePath);
        if (root == null) {
            LOG.info("File not under vcs: " + filePath);
            return null;
        }

        try {
            List<VcsFileRevision> revisions = getHistory(root, filePath, 1)
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)
                    .getRevisions(formatter, loader);
            if (revisions.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("no revisions found for " + filePath);
                }
                return null;
            }
            return revisions.get(0);
        } catch (InterruptedException e) {
            InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(e)));
            return null;
        }
    }

    @Override
    public void reportAppendableHistory(@NotNull FilePath path, @Nullable VcsRevisionNumber startingRevision,
            @NotNull VcsAppendableHistorySessionPartner partner) {
        partner.reportCreatedEmptySession(createAppendableSession(
                path, Collections.emptyList(), null));
        RootedClientConfig root = getRootFor(path);
        if (root == null) {
            LOG.warn("File not under vcs: " + path);
            // TODO bundle message
            partner.reportException(new VcsException("File not under VCS: " + path));
            return;
        }
        final int startingRev;
        if (startingRevision instanceof VcsRevisionNumber.Int) {
            startingRev = ((VcsRevisionNumber.Int) startingRevision).getValue();
        } else {
            startingRev = 0;
            if (startingRevision != null) {
                LOG.warn("Requested reportAppendableHistory with unexpected type " + startingRevision +
                        " (" + startingRevision.getClass() + ")");
            }
        }

        // Async operation
        getHistory(root, path, -1)
                .whenCompleted((r) ->
                    r.getRevisions(formatter, loader).forEach((rev) -> {
                        VcsRevisionNumber rn = rev.getRevisionNumber();
                        if (rn instanceof VcsRevisionNumber.Int) {
                            VcsRevisionNumber.Int rni = (VcsRevisionNumber.Int) rn;
                            if (rni.getValue() >= startingRev) {
                                partner.acceptRevision(rev);
                            }
                        } else {
                            LOG.warn("VcsFileRevision returned unexpected revision number " + rn +
                                    " (" + rn.getClass() + ")");
                        }
                    })
                )
                .whenServerError(partner::reportException);
    }

    @Nullable
    private RootedClientConfig getRootFor(FilePath fp) {
        if (fp == null || project.isDisposed()) {
            return null;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return null;
        }
        return registry.getClientConfigFor(fp);
    }

    private VcsAbstractHistorySession createAppendableSession(final FilePath path, List<VcsFileRevision> revisions, @Nullable final VcsRevisionNumber number) {
        return new VcsAbstractHistorySession(revisions, number) {
            /**
             * This method should return actual value for current revision (it can be changed after submit for example)
             *
             * @return current file revision, null if file does not exist anymore
             */
            @Nullable
            @Override
            protected VcsRevisionNumber calcCurrentRevisionNumber() {
                RootedClientConfig root = getRootFor(path);
                if (root == null) {
                    return null;
                }
                try {
                    ListFilesDetailsResult result = P4ServerComponent
                            .query(project, root.getClientConfig(), new ListFilesDetailsQuery(
                                    Collections.singletonList(path),
                                    ListFilesDetailsQuery.RevState.HEAD,  1))
                            .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project),
                                    TimeUnit.MILLISECONDS);
                    if (result.getFiles().isEmpty()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("No file details found for " + path);
                        }
                        return null;
                    }
                    return result.getFiles().get(0).getRevisionNumber();
                } catch (InterruptedException e) {
                    InternalErrorMessage.send(project).cacheLockTimeoutError(
                            new ErrorEvent<>(new VcsInterruptedException(e)));
                    return null;
                } catch (P4CommandRunner.ServerResultException e) {
                    // Already reported elsewhere
                    LOG.debug(e);
                    return null;
                }
            }

            @Override
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

    private P4CommandRunner.QueryAnswer<ListFileHistoryResult> getHistory(
            @NotNull RootedClientConfig root, FilePath file, int revisionCount) {
        return P4ServerComponent
                .query(project, root.getClientConfig(), new ListFileHistoryQuery(file, revisionCount));
    }

    private static class P4DiffFromHistoryHandler implements DiffFromHistoryHandler {
        @Override
        public void showDiffForOne(@NotNull AnActionEvent e, @NotNull Project project, @NotNull FilePath filePath,
                @NotNull VcsFileRevision previousRevision, @NotNull VcsFileRevision revision) {
            VcsHistoryUtil.showDifferencesInBackground(project, filePath, previousRevision, revision);
        }

        @Override
        public void showDiffForTwo(@NotNull Project project,
                @NotNull FilePath filePath,
                @NotNull VcsFileRevision revision1,
                @NotNull VcsFileRevision revision2) {
            VcsHistoryUtil.showDifferencesInBackground(project, filePath, revision1, revision2);
        }
    }


    private static class ChangelistColumnInfo extends DualViewColumnInfo<VcsFileRevision, String>
            implements Comparator<VcsFileRevision> {
        @NotNull
        private final ColoredTableCellRenderer myRenderer = new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean selected, boolean hasFocus,
                    int row, int column) {
                this.setOpaque(selected);
                this.append(value == null ? "?" : value.toString());
                SpeedSearchUtil.applySpeedSearchHighlighting(table, this, false, selected);
            }
        };

        public ChangelistColumnInfo() {
            super(P4Bundle.getString("history.columns.changelist"));
        }

        Integer getDataOf(VcsFileRevision rev) {
            if (rev instanceof P4FileRevision) {
                return ((P4FileRevision) rev).getChangelistId().getChangelistId();
            }
            if (rev instanceof P4HistoryVcsFileRevision) {
                return ((P4HistoryVcsFileRevision) rev).getChangelistId().getChangelistId();
            }
            return null;
        }

        @Override
        public Comparator<VcsFileRevision> getComparator() {
            return this;
        }

        @Override
        public String valueOf(VcsFileRevision object) {
            Integer result = this.getDataOf(object);
            return result == null ? "" : result.toString();
        }

        @Override
        public int compare(VcsFileRevision o1, VcsFileRevision o2) {
            return Comparing.compare(this.getDataOf(o1), this.getDataOf(o2));
        }

        @Override
        public boolean shouldBeShownIsTheTree() {
            return true;
        }

        @Override
        public boolean shouldBeShownIsTheTable() {
            return true;
        }

        @Override
        @Nullable
        public TableCellRenderer getRenderer(VcsFileRevision revision) {
            return this.myRenderer;
        }
    }

    private static final ChangelistColumnInfo CHANGELIST_COLUMN_INFO = new ChangelistColumnInfo();
    private static final ColumnInfo<?, ?>[] ADDITIONAL_HISTORY_COLUMNS = new ColumnInfo[] { CHANGELIST_COLUMN_INFO };
}
