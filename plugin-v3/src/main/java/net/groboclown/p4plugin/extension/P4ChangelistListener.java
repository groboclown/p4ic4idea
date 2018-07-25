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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListListener;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.util.FileTreeUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.util.ChangelistDescriptionGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class P4ChangelistListener
        implements ChangeListListener {
    private final static Logger LOG = Logger.getInstance(P4ChangelistListener.class);

    private final Project myProject;

    P4ChangelistListener(@NotNull final Project project, @NotNull final P4Vcs vcs) {
        myProject = project;
    }

    @Override
    public void changeListAdded(@NotNull final ChangeList list) {
        // Adding a changelist does not automatically create a corresponding
        // Perforce changelist.  It must have files added to it that are
        // Perforce-backed in order for it to become one.
        LOG.debug("changeListAdded: " + list.getName() + "; [" + list.getComment() + "]; " +
                list.getClass().getSimpleName());
    }

    @Override
    public void changeListRemoved(@NotNull final ChangeList list) {
        LOG.debug("changeListRemoved: " + list.getName() + "; [" + list.getComment() + "]; " + list.getClass()
                .getSimpleName());
        // TODO will removing a changelist force the pending changelist delete?
    }

    @Override
    public void changesRemoved(@NotNull final Collection<Change> changes, @NotNull final ChangeList fromList) {
        LOG.debug("changesRemoved: changes " + changes);
        LOG.debug("changesRemoved: changelist " + fromList.getName() + "; [" + fromList.getComment() + "]; " + fromList
                .getClass().getSimpleName());

        // This method doesn't do what it seems to say it does.
        // It is called when part of a change is removed.  Only
        // changeListRemoved will perform the move to default
        // changelist.  A revert will move it out of the changelist.
        // Note that if a change is removed, it is usually added or
        // moved, so we can ignore this call.
    }


    @Override
    public void changesAdded(@NotNull final Collection<Change> changes, @NotNull final ChangeList toList) {
        LOG.debug("changesAdded: changes " + changes);
        LOG.debug("changesAdded: changelist " + toList.getName() + "; [" + toList.getComment() + "]");

        if (! (toList instanceof LocalChangeList)) {
            return;
        }

        // TODO if a file in a "move" operation is included, but not the
        // other side, then ensure the other side is also moved along with this one.


        LocalChangeList local = (LocalChangeList) toList;

        for (ClientConfigRoot clientConfigRoot : getClientConfigRoots()) {
            // First, see if there are any changes for this client config that require an
            // underlying P4 changelist move.

            Collection<FilePath> affectedFiles = getAffectedFiles(clientConfigRoot, changes);
            if (affectedFiles.isEmpty()) {
                continue;
            }

            try {
                P4ChangelistId p4change =
                        CacheComponent.getInstance(myProject).getServerOpenedCache().first.getP4ChangeFor(
                                clientConfigRoot.getClientConfig().getClientServerRef(),
                                local);
                if (p4change == null) {
                    // No server changelist associated with this ide change.  Create one.
                    CreateChangelistAction action =
                            new CreateChangelistAction(clientConfigRoot.getClientConfig().getClientServerRef(),
                                    toDescription(local));
                    P4LocalChangelist cl =
                            CacheComponent.getInstance(myProject).getServerOpenedCache().first
                                    .getMappedChangelist(action);
                    p4change = cl.getChangelistId();
                    P4ServerComponent
                            .perform(myProject, clientConfigRoot.getClientConfig(), action);
                }
                P4ServerComponent
                        .perform(myProject, clientConfigRoot.getClientConfig(),
                                new MoveFilesToChangelistAction(p4change, affectedFiles));
            } catch (InterruptedException e) {
                LOG.warn(e);
            }
        }
    }

    @Override
    public void changeListChanged(final ChangeList list) {
        LOG.debug("changeListChanged: " + list);
    }

    @Override
    public void changeListRenamed(final ChangeList list, final String oldName) {
        LOG.info("changeListRenamed: from " + oldName + " to " + list);

        if (! (list instanceof LocalChangeList)) {
            // ignore
            return;
        }

        // Don't check name equality, due to the reuse from
        // changeListCommentChanged

        LocalChangeList local = (LocalChangeList) list;

        for (ClientConfigRoot clientConfigRoot : getClientConfigRoots()) {
            try {
                P4ChangelistId change =
                        CacheComponent.getInstance(myProject).getServerOpenedCache().first.getP4ChangeFor(
                                clientConfigRoot.getClientConfig().getClientServerRef(),
                                local);
                if (change != null) {
                    P4ServerComponent
                            .perform(myProject, clientConfigRoot.getClientConfig(),
                                    new EditChangelistAction(change, toDescription(local)));
                }
            } catch (InterruptedException e) {
                LOG.warn(e);
            }
        }
    }

    @Override
    public void changeListCommentChanged(final ChangeList list, final String oldComment) {
        LOG.debug("changeListCommentChanged: " + list);

        // This is the same logic as with the name change.
        changeListRenamed(list, list.getName());
    }

    @Override
    public void changesMoved(final Collection<Change> changes, final ChangeList fromList, final ChangeList toList) {
        LOG.debug("changesMoved: " + fromList + " to " + toList);

        // This is just like a "changes added" command,
        // in the sense that the old list doesn't matter too much.
        changesAdded(changes, toList);
    }

    @Override
    public void defaultListChanged(final ChangeList oldDefaultList, final ChangeList newDefaultList) {
        LOG.debug("defaultListChanged: " + oldDefaultList + " to " + newDefaultList);

        // Don't change the internal default changelist id mapping to the IDE change list.
    }

    @Override
    public void unchangedFileStatusChanged() {
        LOG.debug("unchangedFileStatusChanged");
    }

    @Override
    public void changeListUpdateDone() {
        LOG.debug("changeListUpdateDone");
    }

    private boolean isUnderVcs(final FilePath path) {
        // Only files can be under VCS control.
        if (path.isDirectory()) {
            return false;
        }
        final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(path);
        return ((vcs != null) && (P4Vcs.VCS_NAME.equals(vcs.getName())));
    }

    private List<FilePath> getPathsFromChanges(final Collection<Change> changes) {
        final List<FilePath> paths = new ArrayList<>();
        for (Change change : changes) {
            if ((change.getBeforeRevision() != null) && (isUnderVcs(change.getBeforeRevision().getFile()))) {
                FilePath path = change.getBeforeRevision().getFile();
                if (!paths.contains(path)) {
                    paths.add(path);
                }
            }
            if ((change.getAfterRevision() != null) && (isUnderVcs(change.getAfterRevision().getFile()))) {
                final FilePath path = change.getAfterRevision().getFile();
                if (!paths.contains(path)) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }


    private String toDescription(@NotNull ChangeList changeList) {
        return ChangelistDescriptionGenerator.getDescription(myProject, changeList);
    }

    private Collection<FilePath> getAffectedFiles(ClientConfigRoot clientConfigRoot, Collection<Change> changes) {
        Set<FilePath> ret = new HashSet<>();
        for (Change change : changes) {
            for (ContentRevision cr : Arrays.asList(change.getBeforeRevision(), change.getAfterRevision())) {
                if (cr != null) {
                    FilePath file = cr.getFile();
                    if (!ret.contains(file) && FileTreeUtil.isSameOrUnder(clientConfigRoot.getClientRootDir(), file)) {
                        ret.add(cr.getFile());
                    }
                }
            }
        }
        return ret;
    }

    @NotNull
    private Collection<ClientConfigRoot> getClientConfigRoots() {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(myProject);
        return reg == null ? Collections.emptyList() : reg.getClientConfigRoots();
    }
}
