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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ColumnInfo;
import net.groboclown.idea.p4ic.compat.HistoryCompat;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class P4HistoryProvider implements VcsHistoryProvider {
    private static final Logger LOG = Logger.getInstance(P4HistoryProvider.class);

    private final Project project;
    private final DiffFromHistoryHandler diffHandler;

    public P4HistoryProvider(Project project) {
        this.project = project;
        diffHandler = HistoryCompat.getInstance().createDiffFromHistoryHandler();
    }

    @Override
    public VcsDependentHistoryComponents getUICustomization(VcsHistorySession session, JComponent forShortcutRegistration) {
        // TODO give better history.
        return VcsDependentHistoryComponents.createOnlyColumns(new ColumnInfo[0]);
    }

    @Override
    public AnAction[] getAdditionalActions(Runnable refresher) {
        return new AnAction[0];
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
        revisions.addAll(getHistory(filePath, project));
        return createAppendableSession(filePath, revisions, null);
    }

    @Override
    public void reportAppendableHistory(FilePath path, VcsAppendableHistorySessionPartner partner) throws VcsException {
        final List<P4FileRevision> history = getHistory(path, project);
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
                try {
                    Client client = P4Vcs.getInstance(project).getClientFor(path);
                    if (client != null) {
                        List<P4FileInfo> infoList = client.getServer().getFilePathInfo(Collections.singletonList(path));
                        if (! infoList.isEmpty() && infoList.get(0).getHeadRev() > 0) {
                            return new VcsRevisionNumber.Int(infoList.get(0).getHeadRev());
                        }
                    }
                    return null;
                } catch (VcsException e) {
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
    private static List<P4FileRevision> getHistory(@Nullable FilePath filePath, @NotNull Project project) throws VcsException {
        P4Vcs vcs = P4Vcs.getInstance(project);
        if (filePath == null || ! vcs.fileIsUnderVcs(filePath)) {
            return Collections.emptyList();
        }

        VcsConfiguration vcsConfiguration = VcsConfiguration.getInstance(project);
        int limit = vcsConfiguration.LIMIT_HISTORY ? vcsConfiguration.MAXIMUM_HISTORY_ROWS : -1;

        Client client = vcs.getClientFor(filePath);
        if (client != null) {
            ServerExecutor server = client.getServer();
            List<P4FileInfo> p4files = server.getFilePathInfo(Collections.singletonList(filePath));
            if (p4files.isEmpty()) {
                LOG.info("No file information for " + filePath);
                return Collections.emptyList();
            }
            if (!p4files.get(0).isInDepot()) {
                return Collections.emptyList();
            }
            return server.getRevisionHistory(p4files.get(0), limit);
        }
        return Collections.emptyList();
    }
}
