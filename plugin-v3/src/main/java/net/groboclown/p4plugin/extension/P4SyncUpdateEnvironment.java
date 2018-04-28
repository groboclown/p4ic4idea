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

package net.groboclown.idea.p4ic.v2.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.*;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.ui.sync.SyncOptionConfigurable;
import net.groboclown.idea.p4ic.v2.server.FileSyncResult;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.connection.MessageResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class P4SyncUpdateEnvironment implements UpdateEnvironment {
    private static final Logger LOG = Logger.getInstance(P4SyncUpdateEnvironment.class);

    private final P4Vcs vcs;

    private final SyncOptionConfigurable syncOptions = new SyncOptionConfigurable();

    public P4SyncUpdateEnvironment(final P4Vcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public void fillGroups(final UpdatedFiles updatedFiles) {
        // No non-standard file status, so ignored.
    }

    @NotNull
    @Override
    public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots, final UpdatedFiles updatedFiles,
            final ProgressIndicator progressIndicator, @NotNull final Ref<SequentialUpdatesContext> context)
            throws ProcessCanceledException {
        // Run the Perforce operation in the current thread, because that's the context in which this operation
        // is expected to run.

        if (LOG.isDebugEnabled()) {
            LOG.debug("updateDirectories: sync options are " + syncOptions.getCurrentOptions());
        }

        final SyncUpdateSession session = new SyncUpdateSession();
        final Map<String, FileGroup> groups = sortByFileGroupId(updatedFiles.getTopLevelGroups(), null);
        final Map<P4Server, List<FilePath>> clientRoots = findClientRoots(contentRoots, session);

        for (Entry<P4Server, List<FilePath>> entry : clientRoots.entrySet()) {
            P4Server server = entry.getKey();
            // Get the revision or changelist from the Configurable that the user wants to sync to.

            final MessageResult<Collection<FileSyncResult>> results;
            try {
                results = server.synchronizeFilesOnline(
                        entry.getValue(),
                        syncOptions.getRevision(),
                        syncOptions.getChangelist(),
                        syncOptions.isForceSync());
                for (FileSyncResult file : results.getResult()) {
                    updateFileInfo(file);
                    addToGroup(file, groups);
                }
                session.exceptions.addAll(results.messagesAsExceptions());
            } catch (InterruptedException e) {
                throw new ProcessCanceledException(e);
            } catch (P4DisconnectedException e) {
                session.exceptions.add(e);
            }
        }

        return session;
    }


    /**
     * File was synchronized, so we need to inform Idea that the file state
     * needs to be refreshed.
     *
     * @param file p4 file information
     */
    private void updateFileInfo(@Nullable final FileSyncResult file) {
        if (file != null) {
            final FilePath path = file.getFilePath();
            path.hardRefresh();
        }
    }

    private void addToGroup(@Nullable final FileSyncResult file,
            @NotNull final Map<String, FileGroup> groups) {
        final String groupId = getGroupIdFor(file);
        if (groupId != null) {
            final FileGroup group = groups.get(groupId);
            if (group != null) {
                group.add(file.getFilePath().getIOFile().getAbsolutePath(),
                        P4Vcs.getKey(), file.getRevisionNumber());
            } else {
                LOG.warn("Unknown group " + groupId + " for action " + file.getFileAction() +
                        "; caused by synchronizing " + file.getFilePath());
            }
        }
    }


    @Nullable
    private String getGroupIdFor(@Nullable final FileSyncResult file) {
        if (file == null) {
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("sync: " + file.getFileAction() + " / " + file.getFilePath());
        }
        return P4StatusUpdateEnvironment.getGroupId(file.getFileAction());
    }

    @Nullable
    @Override
    public Configurable createConfigurable(final Collection<FilePath> files) {
        // Allow for the user to select the right revision for synchronizing
        return syncOptions;
    }

    @Override
    public boolean validateOptions(final Collection<FilePath> roots) {
        // This checks to make sure the selected files allow for this option to be shown.
        // We allow update on any file or directory that's under a client root.

        // To make this option easy and fast, just return true.

        return true;
    }


    private Map<String, FileGroup> sortByFileGroupId(final List<FileGroup> groups, Map<String, FileGroup> sorted) {
        if (sorted == null) {
            sorted = new HashMap<String, FileGroup>();
        }

        for (FileGroup group : groups) {
            sorted.put(group.getId(), group);
            sorted = sortByFileGroupId(group.getChildren(), sorted);
        }

        return sorted;
    }


    /**
     * Find the lowest client roots for each content root.  This is necessary, because each content root
     * might map to multiple clients.
     *
     * @param contentRoots input context roots
     * @param session      session
     * @return clients mapped to roots
     */
    private Map<P4Server, List<FilePath>> findClientRoots(final FilePath[] contentRoots,
            final SyncUpdateSession session) {
        Map<P4Server, List<FilePath>> ret = new HashMap<P4Server, List<FilePath>>();

        for (FilePath root: contentRoots) {
            try {
                final P4Server server = vcs.getP4ServerFor(root);
                if (server != null) {
                    List<FilePath> paths = ret.get(server);
                    if (paths == null) {
                        paths = new ArrayList<FilePath>();
                        ret.put(server, paths);
                    }
                    paths.add(root);
                }
            } catch (InterruptedException e) {
                session.exceptions.add(new VcsInterruptedException(e));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("P4ServerName to file mapping for sync: " + ret);
        }

        return ret;
    }

    static class SyncUpdateSession implements UpdateSession {
        private boolean cancelled = false;
        private List<VcsException> exceptions = new ArrayList<VcsException>();

        @NotNull
        @Override
        public List<VcsException> getExceptions() {
            return exceptions;
        }

        @Override
        public void onRefreshFilesCompleted() {
            // TODO if any cache needs update, call it from here.
        }

        @Override
        public boolean isCanceled() {
            return cancelled;
        }
    }


}
