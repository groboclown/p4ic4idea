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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.AddEditResult;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileResult;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// TODO look at switching to a CommitExecutor and CommitSession, CommitSessionContextAware
// to fix the long standing issue where changelists must have comments.
public class P4CheckinEnvironment implements CheckinEnvironment {
    private static final Logger LOG = Logger.getInstance(P4CheckinEnvironment.class);

    private final Project project;

    P4CheckinEnvironment(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
    }

    @Nullable
    @Override
    public RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer) {
        // #52 - we could be able to monitor panel.getCommitMessage(); to ensure
        // that there's a message, and when there isn't, disable the submit
        // button.
        // We can monitor the message (with a thread, disposing of it is
        // a bit of a bother, but it's doable).  However, disabling the button
        // currently requires:
        //  1. Grabbing the owning dialog object of the panel.
        //  2. Using reflection to make the protected "disableOKButton" method
        //     accessible and callable.
        // All of this means that this isn't a very good idea.
        // The panel has a "setWarning" method, but that doesn't do anything.

        // FIXME
        throw new IllegalStateException("not implemented");
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
        return commit(changes, preparedComment, NullableFunction.NULL, new HashSet<>());
    }

    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, final String preparedComment,
            @NotNull NullableFunction<Object, Object> parametersHolder, Set<String> feedback) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("scheduleMissingFileForDeletion: " + files);
        }
        List<VcsException> ret = new ArrayList<>();
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            ret.add(new VcsException("Project not configured"));
            return ret;
        }
        Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
        for (FilePath file : files) {
            ClientConfigRoot root = registry.getClientFor(file);
            if (root == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipped adding file not in VCS root: " + file);
                }
            } else {
                P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for add/edit: " + file + " (@" + id + ")");
                }
                P4CommandRunner.ActionAnswer<DeleteFileResult> answer =
                        P4ServerComponent.getInstance(project).getCommandRunner()
                                .perform(root.getClientConfig(), new DeleteFileAction(file, id))
                                .whenCompleted((res) -> {
                                    P4ChangesViewRefresher.refreshLater(project);
                                });
                if (ApplicationManager.getApplication().isDispatchThread()) {
                    LOG.info("Running delete file command in EDT; will not wait for server errors.");
                } else {
                    try {
                        answer.blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project),
                                TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
                        ret.add(new VcsException(e));
                    }
                }
            }
        }
        return ret;
    }

    @Nullable
    @Override
    public List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files) {
        if (LOG.isDebugEnabled()) {
            LOG.info("scheduleUnversionedFilesForAddition: " + files);
        }
        List<VcsException> ret = new ArrayList<>();
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            ret.add(new VcsException("Project not configured"));
            return ret;
        }
        Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
        for (VirtualFile file : files) {
            ClientConfigRoot root = registry.getClientFor(file);
            if (root == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipped adding file not in VCS root: " + file);
                }
            } else {
                FilePath fp = VcsUtil.getFilePath(file);
                P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for add/edit: " + fp + " (@" + id + ")");
                }
                P4CommandRunner.ActionAnswer<AddEditResult> answer =
                        P4ServerComponent.getInstance(project).getCommandRunner()
                                .perform(root.getClientConfig(), new AddEditAction(fp, getFileType(fp), id, null))
                                .whenCompleted((res) -> {
                                    P4ChangesViewRefresher.refreshLater(project);
                                })
                                //.whenServerError(asdf)
                                //.whenOffline(asdf)
                                ;
                if (ApplicationManager.getApplication().isDispatchThread()) {
                    LOG.info("Running add/edit command in EDT; will not wait for server errors.");
                } else {
                    try {
                        answer.blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project),
                                TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
                        ret.add(new VcsException(e));
                    }
                }
            }
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


    // TODO this is shared with P4VFSListener.  Look at making shared utility.
    private Map<ClientServerRef, P4ChangelistId> getActiveChangelistIds() {
        LocalChangeList defaultIdeChangeList =
                ChangeListManager.getInstance(project).getDefaultChangeList();
        Map<ClientServerRef, P4ChangelistId> ret = new HashMap<>();
        try {
            CacheComponent.getInstance(project).getServerOpenedCache().first
                    .getP4ChangesFor(defaultIdeChangeList)
                    .forEach((id) -> ret.put(id.getClientServerRef(), id));
        } catch (InterruptedException e) {
            LOG.warn(e);
        }
        return ret;
    }

    // TODO this is shared with P4VFSListener.  Look at making shared utility.
    private P4ChangelistId getActiveChangelistFor(ClientConfigRoot root, Map<ClientServerRef, P4ChangelistId> ids) {
        ClientServerRef ref = root.getClientConfig().getClientServerRef();
        P4ChangelistId ret = ids.get(ref);
        if (ret == null) {
            ret = P4ChangelistIdImpl.createDefaultChangelistId(ref);
            ids.put(ref, ret);
        }
        return ret;
    }

    // TODO this is shared with P4VFSListener.  Look at making shared utility.
    private P4FileType getFileType(FilePath fp) {
        FileType ft = fp.getFileType();
        if (ft.isBinary()) {
            return P4FileType.convert("binary");
        }
        return P4FileType.convert("text");
    }
}
