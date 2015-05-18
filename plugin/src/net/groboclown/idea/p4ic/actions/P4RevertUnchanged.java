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

package net.groboclown.idea.p4ic.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.actions.AbstractVcsAction;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.RevertedFilesDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class P4RevertUnchanged extends AbstractVcsAction {
    private static final Logger LOG = Logger.getInstance(P4RevertUnchanged.class);


    @Override
    protected void actionPerformed(@NotNull final VcsContext e) {
        final Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        P4Vcs vcs = P4Vcs.getInstance(project);

        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().saveAll();
        }

        // Revert changes

        final ChangeList[] changes = e.getSelectedChangeLists();
        final Set<FilePath> reverted = new HashSet<FilePath>();
        final List<VcsException> errors = new ArrayList<VcsException>();
        if (changes != null) {
            for (ChangeList change: changes) {
                final Map<Client, P4ChangeListId> clientChanges = mapClientsToChangelistsFor(vcs, change);
                for (Entry<Client, P4ChangeListId> en: clientChanges.entrySet()) {
                    try {
                        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                        final Collection<P4FileInfo> revertedInfo = en.getKey().getServer().revertUnchangedFilesInChangelist(
                                en.getValue().getChangeListId(), messages);
                        for (P4FileInfo fileInfo: revertedInfo) {
                            reverted.add(fileInfo.getPath());
                            LOG.debug("Revert unchanged from changelist: " + fileInfo.getPath());
                        }
                        errors.addAll(P4StatusMessage.messagesAsErrors(messages, true));
                    } catch (VcsException ex) {
                        LOG.warn("Revert caused error", ex);
                        errors.add(ex);
                    }
                }
            }
        }


        // Revert files
        final FilePath[] files = e.getSelectedFilePaths();
        if (files != null) {
            Map<Client, List<FilePath>> clientFileMap;
            try {
                clientFileMap = vcs.mapFilePathToClient(Arrays.asList(files));
            } catch (P4InvalidConfigException ex) {
                errors.add(ex);
                clientFileMap = Collections.emptyMap();
            }

            for (Entry<Client, List<FilePath>> entry: clientFileMap.entrySet()) {
                List<FilePath> toRevert = getFilesToRevert(entry.getValue(), reverted);
                if (! toRevert.isEmpty()) {
                    try {
                        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                        final Collection<P4FileInfo> revertedInfo = entry.getKey().getServer().revertUnchangedFiles(toRevert, messages);
                        for (P4FileInfo fileInfo : revertedInfo) {
                            reverted.add(fileInfo.getPath());
                            LOG.debug("Revert unchanged from filespec: " + fileInfo.getPath());
                        }
                        errors.addAll(P4StatusMessage.messagesAsErrors(messages, true));
                    } catch (VcsException ex) {
                        LOG.warn("Revert caused error", ex);
                        errors.add(ex);
                    }
                }
            }
        }

        P4ChangesViewRefresher.refreshLater(project);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                RevertedFilesDialog.show(project, reverted, errors);
            }
        });
    }

    @Override
    protected void update(@NotNull final VcsContext vcsContext, @NotNull final Presentation presentation) {
        Project project = vcsContext.getProject();
        if (project == null) {
            presentation.setVisible(false);
            return;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);

        final ChangeList[] changes = vcsContext.getSelectedChangeLists();
        if (changes != null && changes.length > 0) {
            for (ChangeList cl: changes) {
                if (cl instanceof LocalChangeList && vcs.getChangeListMapping().hasPerforceChangelist((LocalChangeList) cl)) {
                    presentation.setVisible(true);
                    presentation.setEnabled(true);
                    return;
                }
            }
        }

        final FilePath[] files = vcsContext.getSelectedFilePaths();
        if (files == null || files.length <= 0) {
            presentation.setVisible(false);
            return;
        }

        for (FilePath file: files) {
            if (vcs.fileIsUnderVcs(file)) {
                presentation.setVisible(true);
                presentation.setEnabled(true);
                return;
            }
        }

        presentation.setVisible(false);
    }


    /**
     * Maps online clients to the p4 changelist that matches the idea changelist
     * @param vcs vcs
     * @param ideaChange IDEA changelist
     * @return online clients to P4 changelists
     */
    @NotNull
    private Map<Client, P4ChangeListId> mapClientsToChangelistsFor(@NotNull P4Vcs vcs, @Nullable ChangeList ideaChange) {
        if (ideaChange == null || ! (ideaChange instanceof LocalChangeList)) {
            return Collections.emptyMap();
        }
        Map<Client, P4ChangeListId> ret = new HashMap<Client, P4ChangeListId>();
        for (Client client: vcs.getClients()) {
            final P4ChangeListId change = vcs.getChangeListMapping().getPerforceChangelistFor(client, (LocalChangeList) ideaChange);
            if (client.isWorkingOnline() && change != null) {
                ret.put(client, change);
            }
        }
        return ret;
    }

    @NotNull
    private List<FilePath> getFilesToRevert(@NotNull final List<FilePath> files, @NotNull final Set<FilePath> reverted) {
        List<FilePath> ret = new ArrayList<FilePath>(files);
        ret.removeAll(reverted);
        return ret;
    }

    // Async update is not supported for actions that change their visibility.
    @Override
    protected boolean forceSyncUpdate(AnActionEvent e) {
        return true;
    }
}
