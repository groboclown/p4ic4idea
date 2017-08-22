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

package net.groboclown.idea.p4ic.v2.file;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.openapi.vcs.changes.CommitSessionContextAware;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.changes.ShelveChange;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.ui.alerts.DistinctDialog;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class P4ShelveChangesCommitExecutor implements CommitExecutor {
    private static final Logger LOG = Logger.getInstance(P4ShelveChangesCommitExecutor.class);


    private final Project project;

    public P4ShelveChangesCommitExecutor(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getActionText() {
        return P4Bundle.getString("action.shelve");
    }

    @NotNull
    @Override
    public CommitSession createCommitSession() {
        return new ShelveCommitExecutor();
    }

    private class ShelveCommitExecutor implements CommitSession, CommitSessionContextAware {

        @Override
        public void execute(Collection<Change> changes, String commitMessage) {
            P4Vcs vcs = P4Vcs.getInstance(project);

            try {
                Map<P4ChangeListId, Pair<P4Server, ArrayList<FilePath>>> fileMap = getChangelistMapping(changes, vcs);
                for (Map.Entry<P4ChangeListId, Pair<P4Server, ArrayList<FilePath>>> entry : fileMap.entrySet()) {
                    P4ChangeListId changeList = entry.getKey();
                    P4Server server = entry.getValue().first;
                    ArrayList<FilePath> files = entry.getValue().second;
                    server.shelveFilesInChangelistForOnline(
                            changeList, commitMessage, files);
                }
            } catch (InterruptedException e) {
                DistinctDialog.showMessageDialog(project,
                        P4Bundle.message("interrupted_exception", e.getMessage()),
                        P4Bundle.message("shelve.problem.title"),
                        NotificationType.ERROR);
            } catch (P4DisconnectedException e) {
                DistinctDialog.showMessageDialog(project,
                        P4Bundle.message("error.shelve.offline"),
                        P4Bundle.message("error.offline.title"),
                        NotificationType.ERROR);
            }
        }

        @Override
        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            if (changes.isEmpty() || commitMessage == null || commitMessage.isEmpty()) {
                return false;
            }
            for (Change change : changes) {
                if (change instanceof ShelveChange) {
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings("deprecation")
        @Deprecated
        @Nullable
        @Override
        public JComponent getAdditionalConfigurationUI() {
            return null;
        }

        @Nullable
        @Override
        public JComponent getAdditionalConfigurationUI(Collection<Change> changes, String commitMessage) {
            return null;
        }

        @Override
        public void executionCanceled() {

        }

        @Nullable
        @Override
        public String getHelpId() {
            return null;
        }

        // @Override
        public ValidationInfo validateFields() {
            return null;
        }

        @Override
        public void setContext(CommitContext context) {

        }
    }


    @SuppressWarnings("ConstantConditions")
    private static Map<P4ChangeListId, Pair<P4Server, ArrayList<FilePath>>> getChangelistMapping(
            @NotNull Collection<Change> changes, @NotNull P4Vcs vcs)
            throws InterruptedException {
        Project project = vcs.getProject();
        Map<P4ChangeListId, Pair<P4Server, ArrayList<FilePath>>> ret =
                new HashMap<P4ChangeListId, Pair<P4Server, ArrayList<FilePath>>>();
        for (Change change : changes) {
            LocalChangeList changeList = ChangeListManager.getInstance(project).getChangeList(change);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Associated changelist " + changeList + " for change " + change);
            }
            final FilePath file;
            if (change.getAfterRevision() != null && change.getAfterRevision().getFile() != null) {
                file = change.getAfterRevision().getFile();
            } else if (change.getBeforeRevision() != null && change.getBeforeRevision().getFile() != null) {
                file = change.getBeforeRevision().getFile();
            } else {
                file = null;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No file associated with change " + change);
                }
            }
            if (file != null && changeList != null) {
                P4Server server = vcs.getP4ServerFor(file);
                if (server != null) {
                    P4ChangeListId p4change =
                            P4ChangeListMapping.getInstance(project).getPerforceChangelistFor(server, changeList);
                    if (p4change != null) {
                        Pair<P4Server, ArrayList<FilePath>> pair = ret.get(p4change);
                        if (pair == null) {
                            pair = Pair.create(server, new ArrayList<FilePath>());
                            ret.put(p4change, pair);
                        }
                        pair.second.add(file);
                    } else if (LOG.isDebugEnabled()) {
                        LOG.debug("No Perforce changelist associated with file " + file + " on server " + server);
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("No P4 server associated with file " + file);
                }
            }
        }
        return ret;
    }
}
