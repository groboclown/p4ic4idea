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

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
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
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.async.AnswerSink;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistResult;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.util.FileTreeUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.messages.UserMessage;
import net.groboclown.p4plugin.util.ChangelistUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class P4ChangelistListener
        implements ChangeListListener {
    private final static Logger LOG = Logger.getInstance(P4ChangelistListener.class);

    private final Project myProject;

    P4ChangelistListener(@NotNull final Project project) {
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
        if (!(list instanceof LocalChangeList) || !UserProjectPreferences.getRemoveP4Changelist(myProject)) {
            return;
        }
        LocalChangeList local = (LocalChangeList) list;

        IdeChangelistMap changelistMap =
                CacheComponent.getInstance(myProject).getServerOpenedCache().first;
        try {
            final List<P4ChangelistId> changelists = new ArrayList<>(changelistMap.getP4ChangesFor(local));
            for (ClientConfigRoot clientConfigRoot : getClientConfigRoots()) {
                final Iterator<P4ChangelistId> iter = changelists.iterator();
                while (iter.hasNext()) {
                    final P4ChangelistId p4change = iter.next();
                    if (p4change.isIn(clientConfigRoot.getServerConfig())) {
                        iter.remove();
                        P4ServerComponent.perform(myProject, clientConfigRoot.getClientConfig(),
                                new DeleteChangelistAction(p4change))
                        .whenCompleted((r) -> {
                            UserMessage.showNotification(myProject, UserMessage.INFO,
                                    P4Bundle.message("changelist.removed.text", p4change.getClientname(), p4change),
                                    P4Bundle.message("changelist.removed.title", p4change.getChangelistId()),
                                    NotificationType.INFORMATION);
                        })
                        .whenServerError((err) -> {
                            UserMessage.showNotification(myProject, UserMessage.ERROR,
                                    err.getLocalizedMessage(),
                                    P4Bundle.message("error.remove-changelist.title", p4change.getChangelistId()),
                                    NotificationType.ERROR);
                        });
                    }
                }
            }
        } catch (InterruptedException e) {
            InternalErrorMessage.send(myProject).cacheLockTimeoutError(new ErrorEvent<>(
                    new VcsInterruptedException("Interrupted while performing changelist removal", e)));
        }
    }

    @Override
    public void changesRemoved(@NotNull final Collection<Change> changes, @NotNull final ChangeList fromList) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("changesRemoved: changes " + changes);
            LOG.debug("changesRemoved: changelist " + fromList.getName() + "; [" + fromList.getComment() + "]; "
                    + fromList.getClass().getSimpleName());
        }

        // This is called when a file change is removed from a changelist, not when a changelist is deleted.
        // A revert will move the file it out of the changelist.
        // Note that if a change is removed, it is usually added or
        // moved, so we can ignore this call.
    }

    @Override
    public void changesAdded(@NotNull final Collection<Change> changes, @NotNull final ChangeList toList) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("changesAdded: changes " + changes);
            LOG.debug("changesAdded: changelist " + toList.getName() + "; [" + toList.getComment() + "]");
        }

        if (! (toList instanceof LocalChangeList)) {
            return;
        }

        // TODO if a file in a "move" operation is included, but not the
        // other side, then ensure the other side is also moved along with this one.
        // That can be easily solved if the move operation is contained in a single Change object.


        LocalChangeList local = (LocalChangeList) toList;

        for (ClientConfigRoot clientConfigRoot : getClientConfigRoots()) {
            // First, see if there are any changes for this client config that require an
            // underlying P4 changelist move.

            Collection<FilePath> affectedFiles = getAffectedFiles(clientConfigRoot, changes);
            if (affectedFiles.isEmpty()) {
                continue;
            }

            try {
                // The file may already be associated with the correct change; this can happen on
                // the first invocation from the changelist view refresh.
                final Pair<IdeChangelistMap, IdeFileMap> cache =
                        CacheComponent.getInstance(myProject).getServerOpenedCache();
                final P4ChangelistId p4changeSrc = cache.first.getP4ChangeFor(
                        clientConfigRoot.getClientConfig().getClientServerRef(),
                        local);
                if (p4changeSrc != null) {
                    final Iterator<FilePath> iter = affectedFiles.iterator();
                    while (iter.hasNext()) {
                        final P4LocalFile p4file = cache.second.forIdeFile(iter.next());
                        if (p4file != null && p4file.getChangelistId() != null &&
                                p4changeSrc.getChangelistId() == p4file.getChangelistId().getChangelistId()) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Skipping " + p4file + " from changelist move; it's already in changelist " +
                                        p4changeSrc);
                            }
                            iter.remove();
                        }
                    }
                }
                if (affectedFiles.isEmpty()) {
                    continue;
                }

                Answer.resolve(p4changeSrc)
                .futureMap((BiConsumer<P4ChangelistId, AnswerSink<P4ChangelistId>>) (p4change, sink) -> {
                    if (p4change != null) {
                        sink.resolve(p4change);
                        return;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Forcing the creation of a changelist due to moving p4 file into IDE change");
                    }
                    // No server changelist associated with this ide change.  Create one.
                    CreateChangelistAction action =
                            new CreateChangelistAction(clientConfigRoot.getClientConfig().getClientServerRef(),
                                    toDescription(local), local.getId());
                    P4ServerComponent.perform(myProject, clientConfigRoot.getClientConfig(), action)
                            .whenCompleted((res) -> {
                                sink.resolve(new P4ChangelistIdImpl(
                                        res.getChangelistId(),
                                        clientConfigRoot.getClientConfig().getClientServerRef()));
                            })
                            .whenServerError(sink::reject)
                            .whenOffline(() -> {
                                try {
                                    CacheComponent.getInstance(myProject).getServerOpenedCache().first
                                                    .setMapping(action, local);
                                    P4LocalChangelist v = CacheComponent.getInstance(myProject).getServerOpenedCache()
                                            .first.getMappedChangelist(action);
                                    sink.resolve(v == null ? null : v.getChangelistId());
                                } catch (InterruptedException e) {
                                    sink.reject(AnswerUtil.createFor(e));
                                }
                            });
                })
                .futureMap((cl, sink) -> {
                    if (cl == null) {
                        UserMessage.showNotification(myProject, UserMessage.ERROR,
                                P4Bundle.message("error.create-changelist", local.getName()),
                                P4Bundle.message("error.create-changelist.title"),
                                NotificationType.ERROR);
                        sink.resolve(null);
                        return;
                    }
                    UserMessage.showNotification(myProject, UserMessage.VERBOSE,
                            P4Bundle.message("changelist.created", cl.getChangelistId()),
                            P4Bundle.message("changelist.created.title"),
                            NotificationType.INFORMATION);
                    P4ServerComponent
                            .perform(myProject, clientConfigRoot.getClientConfig(),
                                    new MoveFilesToChangelistAction(cl, affectedFiles))
                    .whenCompleted(sink::resolve)
                    .whenServerError(sink::reject)

                    // Offline is not an error for this particular request.
                    .whenOffline(() -> sink.resolve(null));
                })
                .whenCompleted((r) -> {
                    if (r != null) {
                        MoveFilesToChangelistResult res = (MoveFilesToChangelistResult) r;
                        UserMessage.showNotification(myProject, UserMessage.VERBOSE,
                                P4Bundle.message("changelist.file.moved", res.getFiles().size(), res.getMessage()),
                                P4Bundle.message("changelist.file.moved.title",
                                        res.getChangelistId().getChangelistId()),
                                NotificationType.INFORMATION);
                    }
                })
                .whenFailed(err -> {
                    UserMessage.showNotification(myProject, UserMessage.ERROR,
                            P4Bundle.message("error.changelist-file-move", err.getLocalizedMessage()),
                            P4Bundle.message("error.changelist-file-move.title"),
                            NotificationType.ERROR);
                })
                .blockingWait(UserProjectPreferences.getLockWaitTimeoutMillis(myProject), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                InternalErrorMessage.send(myProject).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException(e)));
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
                InternalErrorMessage.send(myProject).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException(e)));
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
        return ChangelistUtil.createP4ChangelistDescription(myProject, changeList);
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
