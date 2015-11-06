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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinChangeListSpecificComponent;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.ui.checkin.P4SubmitPanel;
import net.groboclown.idea.p4ic.ui.checkin.SubmitContext;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.Map.Entry;

public class P4CheckinEnvironment implements CheckinEnvironment {
    private static final Logger LOG = Logger.getInstance(P4CheckinEnvironment.class);

    private final P4Vcs vcs;
    private final P4ChangeListMapping changeListMapping;

    public P4CheckinEnvironment(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
        this.changeListMapping = P4ChangeListMapping.getInstance(vcs.getProject());
    }

    @Nullable
    @Override
    public RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer) {
        return new P4OnCheckinPanel(vcs, panel, additionalDataConsumer);
    }

    @Nullable
    @Override
    public String getDefaultMessageFor(FilePath[] filesToCheckin) {
        return null;
    }

    @Nullable
    @Override
    public String getHelpId() {
        return null;
    }

    @Override
    public String getCheckinOperationName() {
        return P4Bundle.message("commit.operation.name");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, String preparedComment) {
        return commit(changes, preparedComment, NullableFunction.NULL, new HashSet<String>());
    }

    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, final String preparedComment,
            @NotNull NullableFunction<Object, Object> parametersHolder, Set<String> feedback) {
        LOG.info("Submit to server: " + changes);
        final List<VcsException> errors = new ArrayList<VcsException>();

        // Find all the files and their respective P4 changelists.
        // This method deals with the problem of discovering the
        // changelists to submit, and their associated P4 client.
        // The server end deals with filtering out the files that
        // aren't requested to submit.
        final ChangeListManager clm = ChangeListManager.getInstance(vcs.getProject());
        final Map<P4Server, Map<P4ChangeListId, List<FilePath>>> pathsPerChangeList = new HashMap<P4Server, Map<P4ChangeListId, List<FilePath>>>();
        for (Change change: changes) {
            if (change != null) {
                LocalChangeList cl = clm.getChangeList(change);
                try {
                    splitChanges(change, cl, pathsPerChangeList);
                } catch (InterruptedException e) {
                    errors.add(new VcsInterruptedException(e));
                }
            }
        }

        // If there are files in the default changelist, they need to have their
        // own changelist.  This needs to happen first, because all files that
        // are not in the following changelists are moved into the default.

        // This just puts the defaults into a changelist.  They will be submitted
        // with the rest of the changelists below.

        LOG.info("changes in a changelist: " + pathsPerChangeList);
        for (Entry<P4Server, Map<P4ChangeListId, List<FilePath>>> en: pathsPerChangeList.entrySet()) {
            final P4Server server = en.getKey();
            for (Entry<P4ChangeListId, List<FilePath>> clEn: en.getValue().entrySet()) {
                LOG.info("Submit to " + server + " cl " + clEn.getValue() + " files " +
                    clEn.getValue());
                try {
                    Ref<VcsException> problem = new Ref<VcsException>();
                    Ref<List<P4StatusMessage>> results = new Ref<List<P4StatusMessage>>();
                    server.submitChangelistOnline(clEn.getValue(),
                            getJobs(parametersHolder),
                            getSubmitStatus(parametersHolder),
                            clEn.getKey().getChangeListId(),
                            preparedComment,
                            results, problem);
                    if (! problem.isNull()) {
                        errors.add(problem.get());
                    }
                    errors.addAll(P4StatusMessage.messagesAsErrors(results.get()));
                } catch (P4DisconnectedException e) {
                    LOG.warn(e);
                    errors.add(e);
                } catch (InterruptedException e) {
                    LOG.warn(e);
                    errors.add(new VcsInterruptedException(e));
                }
                //errors.addAll(P4StatusMessage.messagesAsErrors(messages));
            }
        }

        LOG.info("Errors: " + errors);

        // Mark the changes as needing an update
        P4ChangesViewRefresher.refreshLater(vcs.getProject());
        return errors;
    }

    private void splitChanges(@NotNull Change change, @Nullable LocalChangeList lcl,
            @NotNull Map<P4Server, Map<P4ChangeListId, List<FilePath>>> clientPathsPerChangeList)
            throws InterruptedException {
        final FilePath fp;
        if (change.getVirtualFile() == null) {
            // possibly deleted.
            if (change.getBeforeRevision() != null) {
                fp = change.getBeforeRevision().getFile();
            } else {
                LOG.info("Tried to submit a change (" + change + ") which has no file");
                return;
            }
        } else {
            fp = FilePathUtil.getFilePath(change.getVirtualFile());
        }

        final P4Server server = vcs.getP4ServerFor(fp);
        if (server == null) {
            // not under p4 control
            LOG.info("Tried to submit a change (" + change + " / " + fp + ") that is not under P4 control");
            return;
        }
        if (lcl == null) {
            // use the default changelist
            LOG.info("Not in a changelist: " + fp + "; putting in the default changelist");
            lcl = changeListMapping.getDefaultIdeaChangelist();
            if (lcl == null) {
                return;
            }
        }
        final P4ChangeListId p4cl = changeListMapping.getPerforceChangelistFor(server, lcl);
        if (p4cl != null) {
            // each IDEA changelist stores at most 1 p4 changelist per client.
            // so we can exit once it's a client match.
            Map<P4ChangeListId, List<FilePath>> pathsPerChangeList = clientPathsPerChangeList.get(server);
            if (pathsPerChangeList == null) {
                pathsPerChangeList = new HashMap<P4ChangeListId, List<FilePath>>();
                clientPathsPerChangeList.put(server, pathsPerChangeList);
            }
            List<FilePath> files = pathsPerChangeList.get(p4cl);
            if (files == null) {
                files = new ArrayList<FilePath>();
                pathsPerChangeList.put(p4cl, files);
            }
            files.add(fp);
        } else {
            // TODO do something smart
            LOG.error("No perforce changelist known for " + lcl + " at " +
                    server.getClientServerId());
        }
    }


    @Nullable
    @Override
    public List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files) {
        LOG.info("scheduleMissingFileForDeletion: " + files);
        final List<VcsException> ret = new ArrayList<VcsException>();
        try {
            final Map<P4Server, List<FilePath>> fileMapping = vcs.mapFilePathsToP4Server(files);
            for (Entry<P4Server, List<FilePath>> entry : fileMapping.entrySet()) {
                final P4Server server = entry.getKey();
                if (server != null) {
                    final int changeListId = changeListMapping.getProjectDefaultPerforceChangelist(server).
                            getChangeListId();
                    server.deleteFiles(entry.getValue(), changeListId);
                } else {
                    ret.add(new P4FileException(P4Bundle.message("error.add.no-local-file", entry.getValue())));
                }
            }
        } catch (InterruptedException e) {
            ret.add(new VcsInterruptedException(e));
        }

        return ret;
    }

    @Nullable
    @Override
    public List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files) {
        LOG.info("scheduleUnversionedFilesForAddition: " + files);
        final List<VcsException> ret = new ArrayList<VcsException>();

        try {
            final Map<P4Server, List<VirtualFile>> fileMapping = vcs.mapVirtualFilesToP4Server(files);
            for (Entry<P4Server, List<VirtualFile>> entry : fileMapping.entrySet()) {
                final P4Server server = entry.getKey();
                if (server != null) {
                    final int changeListId = changeListMapping.getProjectDefaultPerforceChangelist(server).
                            getChangeListId();
                    server.addOrEditFiles(entry.getValue(), changeListId);
                } else {
                    ret.add(new P4FileException(P4Bundle.message("error.add.no-local-file", entry.getValue())));
                }
            }
        } catch (InterruptedException e) {
            ret.add(new VcsInterruptedException(e));
        }

        return ret;
    }

    @Override
    public boolean keepChangeListAfterCommit(ChangeList changeList) {
        return false;
    }

    @Override
    public boolean isRefreshAfterCommitNeeded() {
        // File status (read-only state) may have changed, or CVS substitution
        // may have happened.
        return true;
    }


    private static void setJobs(@NotNull List<P4ChangeListJob> jobIds, @NotNull PairConsumer<Object, Object> dataConsumer) {
        dataConsumer.consume("jobIds", jobIds);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static List<P4ChangeListJob> getJobs(@NotNull NullableFunction<Object, Object> dataFunc) {
        final Object ret = dataFunc.fun("jobIds");
        if (ret == null) {
            return Collections.emptyList();
        }
        return List.class.cast(ret);
    }

    private static void setSubmitStatus(@NotNull String jobStatus, @NotNull PairConsumer<Object, Object> dataConsumer) {
        dataConsumer.consume("jobStatus", jobStatus);
    }

    @Nullable
    private static String getSubmitStatus(@NotNull NullableFunction<Object, Object> dataFunc) {
        final Object ret = dataFunc.fun("jobStatus");
        if (ret != null) {
            return ret.toString();
        }
        return null;
    }


    private class P4OnCheckinPanel implements CheckinChangeListSpecificComponent {
        private final CheckinProjectPanel parentPanel;
        private final PairConsumer<Object, Object> dataConsumer;
        private final SubmitContext context;
        private final P4SubmitPanel panel;
        private final JScrollPane root;


        P4OnCheckinPanel(@NotNull P4Vcs vcs, @NotNull CheckinProjectPanel panel, final PairConsumer<Object, Object> additionalDataConsumer) {
            this.parentPanel = panel;
            this.dataConsumer = additionalDataConsumer;
            this.context = new SubmitContext(vcs, panel.getSelectedChanges());
            this.panel = new P4SubmitPanel(context);
            this.root = new JBScrollPane(this.panel.getRootPanel());


            // TODO set the ok action as enabled/disabled depending upon
            // whether the comment is empty or not (bug #52).  Probably
            // should just set a warning, though, but that doesn't do anything.
            // We will also need a notification for when the comment is changed,
            // which will require a poll thread, which isn't ideal.
        }

        @Override
        public void onChangeListSelected(LocalChangeList list) {
            context.setSelectedCurrentChanges(list.getChanges());
            panel.updateStatus();
        }

        @Override
        public JComponent getComponent() {
            return root;
        }

        @Override
        public void refresh() {
            restoreState();
        }

        @Override
        public void saveState() {
            // load from context into the data consumer
            setJobs(context.getJobs(), dataConsumer);
            setSubmitStatus(context.getSubmitStatus(), dataConsumer);
        }

        @Override
        public void restoreState() {
            context.refresh(parentPanel.getSelectedChanges());
            panel.updateStatus();
        }
    }
}
