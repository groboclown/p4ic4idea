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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import net.groboclown.p4.server.impl.repository.P4HistoryVcsFileRevision;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.ui.history.ChangelistDetails;

import java.util.Arrays;
import java.util.Collection;

public class ChangelistDescriptionAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ChangelistDescriptionAction.class);

    public ChangelistDescriptionAction() {
        super(
                P4Bundle.getString("history.describe-change"),
                P4Bundle.getString("history.describe-change.description"),
                AllIcons.General.BalloonInformation);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = getEventProject(e);
        if (project == null || project.isDisposed()) {
            LOG.info("Skipping because project is disposed");
            return;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            LOG.info("Skipping because no config registry known");
            return;
        }

        Pair<OptionalClientServerConfig, P4ChangelistId> setup = getReferencedChangelistId(project, registry, e);
        if (setup == null) {
            LOG.info("Skipping because no changelist associated to context item could be found");
            return;
        }

        P4ServerComponent
                .query(project, setup.first, new DescribeChangelistQuery(setup.second))
                .whenCompleted((r) -> {
                    if (r.getRemoteChangelist() != null) {
                        ChangelistDetails.showDocked(project, r.getRemoteChangelist());
                    }
                });
    }


    private Pair<OptionalClientServerConfig, P4ChangelistId> getReferencedChangelistId(Project project,
            ProjectConfigRegistry registry, AnActionEvent e) {
        Pair<OptionalClientServerConfig, P4ChangelistId> ret;
        ret = findAttachedChangelist(project, registry, e);
        if (ret != null) {
            return ret;
        }
        ret = findAttachedFileRevision(e);

        return ret;
    }


    private Pair<OptionalClientServerConfig, P4ChangelistId> findAttachedChangelist(Project project, ProjectConfigRegistry registry,
            AnActionEvent e) {
        ChangeList[] changeLists = VcsDataKeys.CHANGE_LISTS.getData(e.getDataContext());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found changelists on context item: " + Arrays.toString(changeLists));
        }
        if (changeLists == null || changeLists.length <= 0) {
            return null;
        }

        // Check for easy changelists first
        for (ChangeList changeList : changeLists) {
            if (changeList instanceof P4CommittedChangelist) {
                P4CommittedChangelist p4cl = (P4CommittedChangelist) changeList;
                ClientConfig clientConfig = getConfigForChangelistId(registry,
                        p4cl.getSummary().getChangelistId());
                if (clientConfig != null) {
                    return Pair.create(new OptionalClientServerConfig(clientConfig),
                            p4cl.getSummary().getChangelistId());
                } else {
                    LOG.warn("Skipped " + p4cl + " / " + p4cl.getSummary().getChangelistId() +
                            " because no server registration known to exist for " +
                            p4cl.getSummary().getChangelistId().getClientServerRef());
                }
            }
        }

        // Check for harder ones second
        for (ChangeList changeList : changeLists) {
            if (changeList instanceof LocalChangeList) {
                LocalChangeList ide = (LocalChangeList) changeList;
                try {
                    Collection<P4ChangelistId> p4ChangeLists = CacheComponent.getInstance(project).getServerOpenedCache()
                            .first.getP4ChangesFor(ide);
                    if (p4ChangeLists.isEmpty()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("No cached P4 changelist known for IDE change list " + ide);
                        }
                        continue;
                    }

                    VirtualFile file = e.getData(VcsDataKeys.VCS_VIRTUAL_FILE);
                    if (file != null) {
                        ClientConfigRoot root = registry.getClientFor(file);
                        if (root != null) {
                            for (P4ChangelistId p4ChangeList : p4ChangeLists) {
                                if (p4ChangeList.isIn(root.getServerConfig())) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Using changelist " + p4ChangeList);
                                    }
                                    return Pair.create(new OptionalClientServerConfig(root.getClientConfig()), p4ChangeList);
                                }
                            }
                        }
                    }

                    // Pick one
                    P4ChangelistId first = p4ChangeLists.iterator().next();
                    ClientConfig clientConfig = getConfigForChangelistId(registry, first);
                    if (clientConfig != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Using changelist " + first + " out of " + p4ChangeLists);
                        }
                        return Pair.create(new OptionalClientServerConfig(clientConfig), first);
                    }

                    // Unknown...
                    LOG.warn("No known client associated with p4 changelist " + first + "; probably a caching issue?");
                } catch (InterruptedException ex) {
                    InternalErrorMessage.send(project).cacheLockTimeoutError(
                            new ErrorEvent<>(new VcsInterruptedException(ex)));
                }
            }
        }
        return null;
    }

    private ClientConfig getConfigForChangelistId(ProjectConfigRegistry registry, P4ChangelistId p4cl) {
        return registry.getRegisteredClientConfigState(p4cl.getClientServerRef());
    }



    private Pair<OptionalClientServerConfig, P4ChangelistId> findAttachedFileRevision(AnActionEvent e) {
        final VirtualFile file;
        {
            final Boolean nonLocal = e.getData(VcsDataKeys.VCS_NON_LOCAL_HISTORY_SESSION);
            if (Boolean.TRUE.equals(nonLocal)) {
                LOG.info("non-local VCS history session; ignoring changelist description action");
                return null;
            }
            file = e.getData(VcsDataKeys.VCS_VIRTUAL_FILE);
            if (file == null || file.isDirectory()) {
                LOG.info("No VCS virtual file associated with changelist description action; ignoring request.");
                return null;
            }
        }

        final VcsFileRevision revision = e.getData(VcsDataKeys.VCS_FILE_REVISION);
        if (!(revision instanceof P4HistoryVcsFileRevision)) {
            LOG.info("No file revision associated with file " + file);
            return null;
        }
        P4HistoryVcsFileRevision history = (P4HistoryVcsFileRevision) revision;
        return Pair.create(new OptionalClientServerConfig(history.getClientConfig()), history.getChangelistId());
    }
}
