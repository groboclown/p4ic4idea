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
import com.intellij.openapi.vcs.actions.AbstractVcsAction;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.messagebus.SwarmErrorMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.ui.P4Icons;
import net.groboclown.p4plugin.ui.swarm.SwarmReview;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CreateSwarmReviewAction extends AbstractVcsAction {
    private static final Logger LOG = Logger.getInstance(CreateSwarmReviewAction.class);


    public CreateSwarmReviewAction() {
        getTemplatePresentation().setText(P4Bundle.getString("swarm.review.create.title"));
        getTemplatePresentation().setIcon(P4Icons.SWARM);
    }


    @Override
    public void actionPerformed(@NotNull VcsContext event) {
        final Project project = event.getProject();
        if (project == null) {
            return;
        }
        final ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return;
        }
        final List<ChangeList> changeLists = getSelectedChangeLists(event);
        if (changeLists.isEmpty()) {
            return;
        }

        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().saveAll();
        }


        ApplicationManager.getApplication().executeOnPooledThread(() ->
                createSwarmReviews(project, registry, changeLists));
    }


    @Override
    protected void update(@NotNull final VcsContext event, @NotNull final Presentation presentation) {
        Project project = event.getProject();
        if (project == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        List<ChangeList> changeLists = getSelectedChangeLists(event);
        if (changeLists.isEmpty()) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        // Restrict to just 1 changelist to create a review.
        if (changeLists.size() != 1) {
            presentation.setEnabled(false);
            presentation.setVisible(true);
            return;
        }
        boolean hasChanges = false;
        for (ChangeList changeList : changeLists) {
            if (!changeList.getChanges().isEmpty()) {
                hasChanges = true;
                break;
            }
        }
        presentation.setEnabled(hasChanges);
        presentation.setVisible(true);
    }

    private void createSwarmReviews(@NotNull Project project, @NotNull ProjectConfigRegistry registry,
            @NotNull List<ChangeList> changeLists) {
        Answer<Integer> next = Answer.resolve(0);
        for (ChangeList ideChangeList : changeLists) {
            if (ideChangeList instanceof LocalChangeList) {
                try {
                    Collection<P4ChangelistId> p4Changelists =
                            CacheComponent.getInstance(project).getServerOpenedCache().first.
                                    getP4ChangesFor((LocalChangeList) ideChangeList);
                    if (!p4Changelists.isEmpty()) {
                        for (P4ChangelistId p4Changelist : p4Changelists) {
                            next = next.mapAsync(x ->
                                    SwarmReview.createOrEditSwarmReview(project, registry, p4Changelist));
                            // Failure reporting is handled by the SwarmReview class.
                        }
                        // Skip the notOnServer message.
                        continue;
                    }
                } catch (InterruptedException e) {
                    LOG.info("Timeout or interruption while reading list of changes", e);
                }
            }
            SwarmErrorMessage.send(project).notOnServer(new SwarmErrorMessage.SwarmEvent(null), ideChangeList);
        }
    }

    @NotNull
    private List<ChangeList> getSelectedChangeLists(@NotNull VcsContext event) {
        ChangeList[] ret = event.getSelectedChangeLists();
        if (ret == null || ret.length <= 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(ret);
    }
}
