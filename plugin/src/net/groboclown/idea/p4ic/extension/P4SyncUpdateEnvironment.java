/* *************************************************************************
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 * *************************************************************************
 *                                                                         *
 * THIS MATERIAL IS PROVIDED "AS IS." ZILLIANT INC. DISCLAIMS ALL          *
 * WARRANTIES OF ANY KIND WITH REGARD TO THIS MATERIAL, INCLUDING,         *
 * BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF NONINFRINGEMENT,           *
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.                   *
 *                                                                         *
 * Zilliant Inc. shall not be liable for errors contained herein           *
 * or for incidental or consequential damages in connection with the       *
 * furnishing, performance, or use of this material.                       *
 *                                                                         *
 * Zilliant Inc. assumes no responsibility for the use or reliability      *
 * of interconnected equipment that is not furnished by Zilliant Inc,      *
 * or the use of Zilliant software with such equipment.                    *
 *                                                                         *
 * This document or software contains trade secrets of Zilliant Inc. as    *
 * well as proprietary information which is protected by copyright.        *
 * All rights are reserved.  No part of this document or software may be   *
 * photocopied, reproduced, modified or translated to another language     *
 * prior written consent of Zilliant Inc.                                  *
 *                                                                         *
 * ANY USE OF THIS SOFTWARE IS SUBJECT TO THE TERMS AND CONDITIONS         *
 * OF A SEPARATE LICENSE AGREEMENT.                                        *
 *                                                                         *
 * The information contained herein has been prepared by Zilliant Inc.     *
 * solely for use by Zilliant Inc., its employees, agents and customers.   *
 * Dissemination of the information and/or concepts contained herein to    *
 * other parties is prohibited without the prior written consent of        *
 * Zilliant Inc..                                                          *
 *                                                                         *
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 *                                                                         *
 * *************************************************************************/

package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.update.*;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4FileInfo.ClientAction;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class P4SyncUpdateEnvironment implements UpdateEnvironment {
    private static final Logger LOG = Logger.getInstance(P4SyncUpdateEnvironment.class);

    private final P4Vcs vcs;

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

        final SyncUpdateSession session = new SyncUpdateSession();
        final Map<String, FileGroup> groups = sortByFileGroupId(updatedFiles.getTopLevelGroups(), null);
        final Map<Client, List<FilePath>> clientRoots = findClientRoots(contentRoots, session);

        for (Entry<Client, List<FilePath>> entry: clientRoots.entrySet()) {
            Client client = entry.getKey();
            try {
                // TODO get the revision or changelist from the Configurable that the user wants to sync to.
                final List<P4FileInfo> results = client.getServer().synchronizeFiles(entry.getValue(), -1, -1, session.exceptions);
                for (P4FileInfo file: results) {
                    addToGroup(file, groups);
                }
            } catch (VcsException e) {
                session.exceptions.add(e);
            }
        }

        return session;
    }

    private void addToGroup(@Nullable final P4FileInfo file,
            @NotNull final Map<String, FileGroup> groups) {
        final String groupId = getGroupIdFor(file);
        if (groupId != null) {
            final FileGroup group = groups.get(groupId);
            if (group != null) {
                final int rev = file.getHaveRev();
                group.add(file.getPath().getIOFile().getAbsolutePath(),
                        P4Vcs.getKey(), new VcsRevisionNumber.Int(rev));
            } else {
                LOG.warn("Unknown group " + groupId + " for action " + file.getClientAction() +
                        "; caused by synchronizing " + file.getPath());
            }
        }
    }


    @Nullable
    private String getGroupIdFor(@Nullable final P4FileInfo file) {
        if (file == null) {
            return null;
        }
        LOG.info("sync: " + file.getClientAction() + " / " + file.getPath());
        if (file.getClientAction() == ClientAction.NONE) {
            return FileGroup.UPDATED_ID;
        }
        return file.getClientAction().getFileGroupId();
    }

    @Nullable
    @Override
    public Configurable createConfigurable(final Collection<FilePath> files) {
        // Currently no UI for synchronizing changes.
        // TODO this will need to be created to allow synchronizing on things other than #head.

        return null;
    }

    @Override
    public boolean validateOptions(final Collection<FilePath> roots) {
        // TODO better understand what's required with this method.
        // It seems to want a check against the user options, server options, and the files.
        // Perhaps this is for read-only checks?

        return true;
    }



    private Map<String, FileGroup> sortByFileGroupId(final List<FileGroup> groups, Map<String, FileGroup> sorted) {
        if (sorted == null) {
            sorted = new HashMap<String, FileGroup>();
        }

        for (FileGroup group: groups) {
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
     * @param session session
     * @return clients mapped to roots
     */
    private Map<Client, List<FilePath>> findClientRoots(final FilePath[] contentRoots, final SyncUpdateSession session) {
        Map<Client, List<FilePath>> ret = new HashMap<Client, List<FilePath>>();

        Set<FilePath> discoveredRoots = new HashSet<FilePath>();

        final List<Client> clients = vcs.getClients();
        for (Client client: clients) {
            final List<FilePath> clientPaths = new ArrayList<FilePath>();
            final List<FilePath> clientRoots;
            try {
                clientRoots = client.getFilePathRoots();
            } catch (P4InvalidConfigException e) {
                session.exceptions.add(e);
                continue;
            }

            // Find the double mapping - if a content root is a child of the client root, then add the
            // content root.  If the client root is a child of the content root, then add the client root.
            for (FilePath clientRoot: clientRoots) {
                for (FilePath contentRoot : contentRoots) {
                    if (contentRoot.isUnder(clientRoot, false) && ! discoveredRoots.contains(contentRoot)) {
                        clientPaths.add(contentRoot);
                        discoveredRoots.add(contentRoot);
                    } else if (clientRoot.isUnder(contentRoot, false) && ! discoveredRoots.contains(clientRoot)) {
                        clientPaths.add(clientRoot);
                        discoveredRoots.add(clientRoot);
                    }
                }
            }

            // We could shrink the contents of the list - we don't want both a/b/c AND a/b in the list.
            // However, the p4 command will shrink it for us.

            if (! clientPaths.isEmpty()) {
                ret.put(client, clientPaths);
            }
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
