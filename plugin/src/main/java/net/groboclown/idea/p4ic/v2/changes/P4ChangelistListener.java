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
package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListListener;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.util.ChangelistDescriptionGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class P4ChangelistListener implements ChangeListListener {
    private final static Logger LOG = Logger.getInstance(P4ChangelistListener.class);

    private final Project myProject;
    private final P4Vcs myVcs;
    private final P4ChangeListMapping changeListMapping;
    private final AlertManager alerts;

    public P4ChangelistListener(@NotNull final Project project, @NotNull final P4Vcs vcs) {
        myProject = project;
        myVcs = vcs;
        alerts = AlertManager.getInstance();
        changeListMapping = P4ChangeListMapping.getInstance(project);
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

        if (list instanceof LocalChangeList && !P4ChangeListMapping.isDefaultChangelist(list)) {
            final List<P4ChangeListId> changelists = new ArrayList<P4ChangeListId>(
                    changeListMapping.getAllPerforceChangelistsFor((LocalChangeList) list));
            for (P4Server server: myVcs.getP4Servers()) {
                final Iterator<P4ChangeListId> iter = changelists.iterator();
                while (iter.hasNext()) {
                    final P4ChangeListId next = iter.next();
                    if (next.isDefaultChangelist()) {
                        iter.remove();
                    } else if (next.isIn(server)) {
                        iter.remove();
                        server.deleteChangelist(next.getChangeListId());
                    }
                }
            }
        }
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
    }


    @Override
    public void changesAdded(@NotNull final Collection<Change> changes, @NotNull final ChangeList toList) {
        LOG.debug("changesAdded: changes " + changes);
        LOG.debug("changesAdded: changelist " + toList.getName() + "; [" + toList.getComment() + "]");


        // TODO if a file in a "move" operation is included, but not the
        // other side, then ensure the other side is also moved along with this one.


        if (toList instanceof LocalChangeList) {
            final List<FilePath> paths = getPathsFromChanges(changes);
            if (paths.isEmpty()) {
                return;
            }

            final Map<P4Server, List<FilePath>> perServer;
            try {
                perServer = myVcs.mapFilePathsToP4Server(paths);
            } catch (InterruptedException e) {
                alerts.addWarning(myProject,
                        P4Bundle.message("error.cancelled-timeout.changes-added"),
                        P4Bundle.message("error.cancelled-timeout"),
                        e, paths);
                return;
            }
            for (Entry<P4Server, List<FilePath>> entry : perServer.entrySet()) {
                P4Server server = entry.getKey();
                // Do not find the actual Perforce related changelist here, because
                // it could potentially not exist yet.  Instead, pass in the IDEA
                // changelist so that, if it is created, it can be bound to the
                // IDEA changelist correctly.
                server.moveFilesToChange(entry.getValue(), (LocalChangeList) toList, changeListMapping);
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

        if (Comparing.equal(list.getName(), oldName)) {
            return;
        }

        if (P4ChangeListMapping.isDefaultChangelist(list) &&
                P4ChangeListMapping.isIdeaDefaultChangelistName(oldName)) {
            LOG.info("Cannot set a comment to the default changelist in Perforce; ignoring.");
            return;
        }

        if (P4ChangeListMapping.isDefaultChangelist(list)) {
            // Rename a changelist to the default.
            // So move the files into the default changelist, and delete
            // the changelist.

            final Map<ClientServerRef, P4ChangeListId> moved =
                    changeListMapping.rebindChangelistAsDefault((LocalChangeList) list);
            for (P4Server server: myVcs.getP4Servers()) {
                final P4ChangeListId p4cl = moved.remove(server.getClientServerId());
                if (p4cl != null) {
                    final Collection<P4FileAction> opened;
                    try {
                        opened = server.getOpenFiles();
                    } catch (InterruptedException e) {
                        LOG.error(e);
                        continue;
                    }
                    List<FilePath> files = new ArrayList<FilePath>(opened.size());
                    for (P4FileAction p4FileAction : opened) {
                        if (p4FileAction.getChangeList() == p4cl.getChangeListId()) {
                            files.add(p4FileAction.getFile());
                        }
                    }
                    server.moveFilesToChange(files, (LocalChangeList) list, changeListMapping);
                }
            }
            return;
        }


        if (P4ChangeListId.DEFAULT_CHANGE_NAME.equals(oldName)) {
            // Move files out of the default changelist.

            for (P4Server server : myVcs.getP4Servers()) {
                final Collection<P4FileAction> opened;
                try {
                    opened = server.getOpenFiles();
                } catch (InterruptedException e) {
                    LOG.error(e);
                    continue;
                }
                List<FilePath> files = new ArrayList<FilePath>(opened.size());
                for (P4FileAction p4FileAction : opened) {
                    if (p4FileAction.getChangeList() == P4ChangeListId.P4_DEFAULT) {
                        files.add(p4FileAction.getFile());
                    }
                }
                server.moveFilesToChange(files, (LocalChangeList) list, changeListMapping);
            }
            return;
        }

        for (P4Server server : myVcs.getP4Servers()) {
            final P4ChangeListId p4cl = changeListMapping.getPerforceChangelistFor(server, (LocalChangeList) list);
            if (p4cl != null) {
                server.renameChangelist(p4cl.getChangeListId(), toDescription(server.getProject(), list));
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
        final List<FilePath> paths = new ArrayList<FilePath>();
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


    private static String toDescription(@Nullable Project project, @NotNull ChangeList changeList) {
        return ChangelistDescriptionGenerator.getDescription(project, changeList);
    }
}
