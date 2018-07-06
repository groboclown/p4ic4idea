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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.DiffFromHistoryHandler;
import com.intellij.openapi.vcs.history.HistoryAsTreeProvider;
import com.intellij.openapi.vcs.history.VcsAbstractHistorySession;
import com.intellij.openapi.vcs.history.VcsAppendableHistorySessionPartner;
import com.intellij.openapi.vcs.history.VcsDependentHistoryComponents;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.vcs.history.VcsHistoryProviderEx;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.compat.HistoryCompat;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class P4HistoryProvider
        implements VcsHistoryProviderEx {
    private static final Logger LOG = Logger.getInstance(P4HistoryProvider.class);

    private final Project project;
    private final DiffFromHistoryHandler diffHandler;

    P4HistoryProvider(@NotNull Project project) {
        this.project = project;
        this.diffHandler = HistoryCompat.getInstance().createDiffFromHistoryHandler();
    }

    @Override
    public VcsDependentHistoryComponents getUICustomization(VcsHistorySession session, JComponent forShortcutRegistration) {
        return VcsDependentHistoryComponents.createOnlyColumns(new ColumnInfo[0]);
    }

    @Override
    public AnAction[] getAdditionalActions(Runnable refresher) {
        LOG.warn("FIXME add an action to view the description of a changelist.");
        return new AnAction[] {
                // FIXME add an action to view the description of a changelist.
                //new ChangelistDescriptionAction()
        };
    }

    @Override
    public boolean isDateOmittable() {
        return true;
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
        ClientConfigRoot root = getRootFor(filePath);
        if (root == null) {
            LOG.info("Not in P4 project: " + filePath);
            return null;
        }

        try {
            List<VcsFileRevision> revisions = P4ServerComponent.getInstance(project)
                    .getCommandRunner()
                    .query(root.getClientConfig().getServerConfig(),
                            new ListFileHistoryQuery(root.getClientConfig().getClientServerRef(), filePath, -1))
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)
                    .getRevisions();
            return createAppendableSession(filePath, revisions, null);
        } catch (InterruptedException e) {
            // TODO better exception?
            throw new VcsException(e);
        }
    }

    @Override
    public void reportAppendableHistory(FilePath path, VcsAppendableHistorySessionPartner partner) {
        // TODO make async
        ClientConfigRoot root = getRootFor(path);
        if (root == null) {
            // TODO bundle message
            partner.reportException(new VcsException("File not under VCS: " + path));
            partner.finished();
            return;
        }

        // Async operation.
        getHistory(root, path, -1)
                .whenCompleted((r) -> {
                    final VcsAbstractHistorySession emptySession = createAppendableSession(
                            path, Collections.emptyList(), null);
                    partner.reportCreatedEmptySession(emptySession);
                    r.getRevisions().forEach(partner::acceptRevision);
                })
                .whenServerError(partner::reportException)
                .after(partner::finished);
    }

    @Override
    @Nullable
    public VcsFileRevision getLastRevision(FilePath filePath)
            throws VcsException {
        ClientConfigRoot root = getRootFor(filePath);
        if (root == null) {
            return null;
        }

        try {
            List<VcsFileRevision> revisions = getHistory(root, filePath, 1)
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)
                    .getRevisions();
            if (revisions.isEmpty()) {
                return null;
            }
            return revisions.get(0);
        } catch (InterruptedException e) {
            LOG.info(e);
            return null;
        }
    }

    @Override
    public void reportAppendableHistory(@NotNull FilePath path, @Nullable VcsRevisionNumber startingRevision,
            @NotNull VcsAppendableHistorySessionPartner partner) {
        // Async!
        ClientConfigRoot root = getRootFor(path);
        if (root == null) {
            // TODO bundle message
            partner.reportException(new VcsException("File not under VCS: " + path));
            partner.finished();
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
                    r.getRevisions().forEach((rev) -> {
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
                .whenServerError(partner::reportException)
                .after(partner::finished);
    }

    @Nullable
    private ClientConfigRoot getRootFor(FilePath fp) {
        if (fp == null || project.isDisposed()) {
            return null;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return null;
        }
        return registry.getClientFor(fp);
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
                ClientConfigRoot root = getRootFor(path);
                if (root == null) {
                    return null;
                }
                try {
                    ListFilesDetailsResult result = P4ServerComponent.getInstance(project)
                            .getCommandRunner()
                            .query(root.getClientConfig().getServerConfig(), new ListFilesDetailsQuery(
                                    root.getClientConfig().getClientServerRef(), Collections.singletonList(path), 1))
                            .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project),
                                    TimeUnit.MILLISECONDS);
                    if (result.getFiles().isEmpty()) {
                        return null;
                    }
                    return result.getFiles().get(0).getRevisionNumber();
                } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
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

    private P4CommandRunner.QueryAnswer<ListFileHistoryResult> getHistory(
            @NotNull ClientConfigRoot root, FilePath file, int revisionCount) {
        return P4ServerComponent.getInstance(project)
                .getCommandRunner()
                .query(root.getClientConfig().getServerConfig(), new ListFileHistoryQuery(
                        root.getClientConfig().getClientServerRef(), file, revisionCount));
    }
}
