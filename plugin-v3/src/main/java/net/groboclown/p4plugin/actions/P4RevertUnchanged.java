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
import com.intellij.openapi.vcs.actions.AbstractVcsAction;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.ui.RevertedFilesDialog;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.connection.MessageResult;
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
        final List<P4StatusMessage> errors = new ArrayList<P4StatusMessage>();
        if (changes != null) {
            for (ChangeList change: changes) {
                final Map<P4Server, P4ChangeListId> serverChanges = mapClientsToChangelistsFor(vcs, change);
                for (Entry<P4Server, P4ChangeListId> en: serverChanges.entrySet()) {
                    final P4Server server = en.getKey();
                    final Set<FilePath> files = new HashSet<FilePath>();
                    try {
                        for (P4FileAction action: server.getOpenFiles()) {
                            if (action.getChangeList() == en.getValue().getChangeListId()) {
                                files.add(action.getFile());
                            }
                        }
                        final MessageResult<List<FilePath>> messages =
                                server.revertUnchangedFilesOnline(files, en.getValue().getChangeListId());
                        reverted.addAll(messages.getResult());
                        errors.addAll(messages.getMessages());
                    } catch (InterruptedException ex) {
                        LOG.warn(ex);
                    } catch (P4DisconnectedException ex) {
                        // TODO ensure that it's already handled by looking at the calls that were made.
                        LOG.warn(ex);
                    }
                }
            }
        }

        if (! reverted.isEmpty()) {
            P4ChangesViewRefresher.refreshLater(project);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    RevertedFilesDialog.show(project, reverted, errors);
                }
            });
        }

    }

    @Override
    protected void update(@NotNull final VcsContext vcsContext, @NotNull final Presentation presentation) {
        Project project = vcsContext.getProject();
        if (project == null) {
            presentation.setVisible(false);
            return;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);
        presentation.setText(P4Bundle.getString("action.revert-unchanged"));

        final ChangeList[] changes = vcsContext.getSelectedChangeLists();
        if (changes != null && changes.length > 0) {
            for (ChangeList cl: changes) {
                if (cl instanceof LocalChangeList
                        && (P4ChangeListMapping.getInstance(project).hasPerforceChangelist((LocalChangeList) cl)
                        || ((LocalChangeList) cl).isDefault())) {
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

        presentation.setVisible(true);
        presentation.setEnabled(false);
    }


    /**
     * Maps online clients to the p4 changelist that matches the idea changelist
     * @param vcs vcs
     * @param ideaChange IDEA changelist
     * @return online clients to P4 changelists
     */
    @NotNull
    private Map<P4Server, P4ChangeListId> mapClientsToChangelistsFor(@NotNull P4Vcs vcs, @Nullable ChangeList ideaChange) {
        if (ideaChange == null || ! (ideaChange instanceof LocalChangeList)) {
            return Collections.emptyMap();
        }

        final LocalChangeList localChange = (LocalChangeList) ideaChange;
        final P4ChangeListMapping changeListMapping = P4ChangeListMapping.getInstance(vcs.getProject());
        Map<P4Server, P4ChangeListId> ret = new HashMap<P4Server, P4ChangeListId>();
        for (P4Server server: vcs.getP4Servers()) {
            final P4ChangeListId change = changeListMapping.getPerforceChangelistFor(server, localChange);
            if (change != null) {
                ret.put(server, change);
            }
        }
        return ret;
    }

    // Async update is not supported for actions that change their visibility.
    @Override
    protected boolean forceSyncUpdate(AnActionEvent e) {
        return true;
    }
}
