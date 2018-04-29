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

package net.groboclown.p4plugin.actions;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.actions.AbstractVcsAction;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.swarm.SwarmConnectionComponent;
import net.groboclown.idea.p4ic.ui.P4Icons;
import net.groboclown.idea.p4ic.ui.swarm.CreateSwarmReviewDialog;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.util.ChangelistDescriptionGenerator;
import net.groboclown.p4.simpleswarm.SwarmClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CreateSwarmReviewAction extends AbstractVcsAction {
    private static final Logger LOG = Logger.getInstance(CreateSwarmReviewAction.class);


    @Override
    protected void actionPerformed(@NotNull VcsContext e) {
        final Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().saveAll();
        }

        final ChangeList[] changes = e.getSelectedChangeLists();
        if (changes != null && changes.length == 1 && changes[0] instanceof LocalChangeList &&
                P4ChangeListMapping.getInstance(project).hasPerforceChangelist((LocalChangeList) changes[0])) {
            final String description = ChangelistDescriptionGenerator.getDescription(project, changes[0]);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    Collection<P4ChangeListId> p4changes =
                            P4ChangeListMapping.getInstance(project).getAllPerforceChangelistsFor((LocalChangeList)
                                    changes[0]);
                    List<Pair<SwarmClient, P4ChangeListId>> swarmChanges = new ArrayList<Pair<SwarmClient,
                            P4ChangeListId>>();
                    for (P4ChangeListId p4change : p4changes) {
                        SwarmClient client = SwarmConnectionComponent.getInstance().getClientFor(p4change
                                .getClientServerRef());
                        if (client != null) {
                            swarmChanges.add(Pair.create(client, p4change));
                        }
                    }
                    if (swarmChanges.isEmpty()) {
                        AlertManager.getInstance().addWarning(project,
                                P4Bundle.message("swarm.review.no-swarm-client", changes[0].getName()),
                                P4Bundle.message("swarm.review.no-swarm-client", changes[0].getName()),
                                null, new FilePath[0]);
                    } else {
                        CreateSwarmReviewDialog.show(project, description, swarmChanges);
                    }
                }
            });
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Changes can't be shown in a swarm review");
            if (changes != null) {
                for (ChangeList change : changes) {
                    if (change instanceof LocalChangeList) {
                        if (P4ChangeListMapping.getInstance(project).hasPerforceChangelist((LocalChangeList) change)) {
                            LOG.debug(change.getName() + " (p4)");
                        } else {
                            LOG.debug(change.getName() + " (local " + ((LocalChangeList) change).getId() + ")");
                        }
                    } else {
                        LOG.debug(change.getName() + " (non-local)");
                    }
                }
            }
        }
    }

    /**
     * Can only work on a single selected changelist.
     *
     * @param vcsContext contains the selected changelist
     * @param presentation UI to update
     */
    @Override
    protected void update(@NotNull final VcsContext vcsContext, @NotNull final Presentation presentation) {
        final Project project = vcsContext.getProject();
        if (project == null) {
            presentation.setVisible(false);
            return;
        }

        Map<ClientServerRef, SwarmClient> clients = SwarmConnectionComponent.getInstance().getClientsFor(project);
        if (clients.isEmpty()) {
            LOG.debug("No swarm clients found");
            presentation.setVisible(false);
            return;
        }

        presentation.setText(P4Bundle.getString("actions.create-swarm-review"));
        presentation.setIcon(P4Icons.SWARM);

        final ChangeList[] changes = vcsContext.getSelectedChangeLists();
        if (changes != null && changes.length == 1 && changes[0] instanceof LocalChangeList) {
            if (P4ChangeListMapping.isDefaultChangelist(changes[0])) {
                // we know that the project has some swarm clients, and since all clients have
                // a default changelist, we can assume that this is a valid setup.
                presentation.setVisible(true);
                presentation.setEnabled(true);
                return;
            }

            Collection<P4ChangeListId> p4changes =
                    P4ChangeListMapping.getInstance(project).getAllPerforceChangelistsFor((LocalChangeList)
                            changes[0]);
            for (P4ChangeListId p4change : p4changes) {
                if (clients.containsKey(p4change.getClientServerRef())) {
                    presentation.setVisible(true);
                    presentation.setEnabled(true);
                    return;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Changes can't be shown in a swarm review");
            if (changes != null) {
                for (ChangeList change : changes) {
                    if (change instanceof LocalChangeList) {
                        if (P4ChangeListMapping.getInstance(project).hasPerforceChangelist((LocalChangeList) change)) {
                            LOG.debug(change.getName() + " (p4)");
                        } else if (P4ChangeListMapping.isDefaultChangelist(change)) {
                            LOG.debug(change.getName() + " (local default, not in p4)");
                        } else {
                            LOG.debug(change.getName() + " (local " + ((LocalChangeList) change).getId() + ")");
                        }
                    } else {
                        LOG.debug(change.getName() + " (non-local)");
                    }
                }
            }
            LOG.debug("Default IDEA changelist: " + ChangeListManager.getInstance(project).getDefaultChangeList().getId());
        }

        presentation.setVisible(true);
        presentation.setEnabled(false);
    }
}
