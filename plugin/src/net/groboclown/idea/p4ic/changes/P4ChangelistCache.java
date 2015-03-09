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

package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4ApiException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class P4ChangeListCache implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(P4ChangeListMapping.class);

    public static final int P4_DEFAULT = IChangelist.DEFAULT;
    public static final int P4_UNKNOWN = IChangelist.UNKNOWN;

    /** numbered changelists, by server */
    private final Map<String, List<P4ChangeList>> numberedCache = new HashMap<String, List<P4ChangeList>>();

    /** default changelists, by server + '@' + client */
    private final Map<String, P4ChangeList> defaultCache = new HashMap<String, P4ChangeList>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static P4ChangeListCache getInstance() {
        return ServiceManager.getService(P4ChangeListCache.class);
    }


    @NotNull
    public List<P4ChangeList> getChanges(@NotNull Client client) throws VcsException {
        lock.readLock().lock();
        try {
            List<P4ChangeList> res = numberedCache.get(client.getConfig().getServiceName());
            if (res == null)
            {
                reloadCache(client);
                res = numberedCache.get(client.getConfig().getServiceName());
            }
            List<P4ChangeList> ret = new ArrayList<P4ChangeList>(res);
            final P4ChangeList change = defaultCache.get(getClientServerId(client));
            if (change != null)
            {
                ret.add(change);
            }
            return ret;
        } finally {
            lock.readLock().unlock();
        }
    }


    @NotNull
    public Map<Client, List<P4ChangeList>> getChangeListsForAll(@NotNull Collection<Client> clients) throws VcsException {
        final Map<Client, List<P4ChangeList>> ret = new HashMap<Client, List<P4ChangeList>>();
        for (Client client: clients) {
            ret.put(client, getChangeListsFor(client));
        }
        return ret;
    }


    @NotNull
    public List<P4ChangeList> getChangeListsFor(@NotNull Client client) throws VcsException {
        List<P4ChangeList> res = getCachedChangeListsFor(client);
        if (res == null) {
            // small window for a duplicate call into the cache reload, because this is outside of the lock.
            // However, no real way to do this and keep from locking over a p4 operation.
            reloadCache(client);
            res = getCachedChangeListsFor(client);
            if (res == null) {
                throw new P4ApiException("could not load changelist cache for client " +
                        client.getClientName() + '@' + client.getConfig().getServiceName());
            }
        }
        return res;
    }


    @Nullable
    public P4ChangeList getChangeListFor(@NotNull Client client, P4ChangeListId p4id) throws VcsException {
        List<P4ChangeList> res = getCachedChangeListsFor(client);
        for (P4ChangeList p4cl: res) {
            if (p4cl.getId().equals(p4id)) {
                return p4cl;
            }
        }
        return null;
    }


    public void reloadCachesFor(@NotNull final List<Client> clients) throws VcsException {
        for (Client client: clients) {
            reloadCache(client);
        }
    }


    public void reloadCache(final Client client) throws VcsException {
        final List<P4ChangeList> serverCache = new ArrayList<P4ChangeList>();
        final Set<P4FileInfo> files = new HashSet<P4FileInfo>(
                client.getServer().loadOpenFiles(client.getRoots().toArray(new VirtualFile[client.getRoots().size()])));
        final List<IChangelistSummary> summaries = client.getServer().getPendingClientChangelists();
        P4ChangeList defaultChange = null;

        // For each changelist, create the P4ChangeList object and remove the
        // found files.
        for (IChangelistSummary summary : summaries) {
            if (summary.getId() < 0) {
                LOG.error("`p4 changes` returned a negative changelist ID");
            } else {
                Set<P4FileInfo> changelistFiles = new HashSet<P4FileInfo>();
                Iterator<P4FileInfo> iter = files.iterator();
                while (iter.hasNext()) {
                    final P4FileInfo file = iter.next();
                    if (file.getChangelist() == summary.getId()) {
                        changelistFiles.add(file);
                        iter.remove();
                    }
                }
                P4ChangeList change = new P4ChangeList(new P4ChangeListIdImpl(client, summary), changelistFiles,
                        summary.getDescription(), summary.getUsername());
                if (change.getId().isDefaultChangelist()) {
                    if (defaultChange != null) {
                        LOG.error("`p4 changes` returned multiple default changelists");
                    } else {
                        defaultChange = change;
                    }
                } else {
                    serverCache.add(change);
                }
            }
        }

        // If there are left over files, then that could be an error.
        if (! files.isEmpty()) {
            LOG.error("`p4 opeend` returned files that are not in opened changelists: " +
                files);
        }


        lock.writeLock().lock();
        try {
            numberedCache.put(client.getConfig().getServiceName(), serverCache);
            if (defaultChange == null) {
                defaultCache.remove(getClientServerId(client));
            } else {
                defaultCache.put(getClientServerId(client), defaultChange);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    @NotNull
    public P4ChangeListId createChangeList(@NotNull Client client, @NotNull String description) throws VcsException {
        final IChangelist p4cl = client.getServer().createChangelist(description);
        final P4ChangeList changeList = new P4ChangeList(new P4ChangeListIdImpl(client, p4cl),
                Collections.<P4FileInfo>emptyList(), p4cl.getDescription(), p4cl.getUsername());
        lock.writeLock().lock();
        try {
            // Can only ever be a numbered changelist
            List<P4ChangeList> changes = numberedCache.get(client.getConfig().getServiceName());
            if (changes == null) {
                changes = new ArrayList<P4ChangeList>();
            }
            // And it's only ever a new changelist, so no need to check if it's already added
            changes.add(changeList);
            numberedCache.put(client.getConfig().getServiceName(), changes);
        } finally {
            lock.writeLock().unlock();
        }
        return changeList.getId();
    }


    /**
     * Add files to a changelist.
     *
     * @param client
     * @param changeList the change to add the files to, or {@code null} for the default changelist.
     * @param affected
     * @return
     * @throws VcsException
     */
    public List<P4StatusMessage> addFilesToChangelist(@NotNull Client client, @Nullable P4ChangeListId changeList,
            @NotNull List<FilePath> affected) throws VcsException {
        // Add in the files, then reload the cache for the changelist.
        final List<P4StatusMessage> messages =
                client.getServer().moveFilesToChangelist(
                        changeList == null ? P4_DEFAULT : changeList.getChangeListId(), affected);

        reloadChangeList(client, changeList);

        return messages;
    }


    /**
     *
     * @param client
     * @param changeListId    the ID for the changelist, or {@code null} for the default changelist on the client.
     * @return true if the changelist was updated; false if it doesn't exist or was submitted / deleted
     * @throws VcsException
     */
    public boolean reloadChangeList(@NotNull Client client, @Nullable P4ChangeListId changeListId) throws VcsException {
        // Never have the Perforce actions in a lock

        boolean removed = false;

        final IChangelist summary;
        if (changeListId == null) {
            changeListId = new P4ChangeListIdImpl(client, P4_DEFAULT);
        }
        if (changeListId.isDefaultChangelist()) {
            summary = null;
        } else {
            summary = client.getServer().getChangelist(changeListId.getChangeListId());
            if (summary == null || summary.getStatus() == ChangelistStatus.SUBMITTED) {
                removed = true;
            }
        }
        final List<P4FileInfo> currentFiles = client.getServer().getFilesInChangelist(changeListId.getChangeListId());
        if (currentFiles == null) {
            removed = true;
        }

        final P4ChangeList list;
        if (removed) {
            list = null;
        } else {
            list = new P4ChangeList(changeListId, currentFiles,
                    summary == null ? null : summary.getDescription(),
                    summary == null ? null : summary.getUsername());
        }

        lock.writeLock().lock();
        try {
            if (changeListId.isDefaultChangelist()) {
                if (list != null) {
                    defaultCache.put(getClientServerId(client), list);
                }
            } else {
                List<P4ChangeList> changes = numberedCache.get(client.getConfig().getServiceName());
                if (changes == null) {
                    if (list == null) {
                        // early exit - nothing to remove
                        return false;
                    }
                    changes = new ArrayList<P4ChangeList>();
                    numberedCache.put(client.getConfig().getServiceName(), changes);
                }
                final Iterator<P4ChangeList> iter = changes.iterator();
                while (iter.hasNext()) {
                    final P4ChangeList next = iter.next();
                    if (next.getId().equals(changeListId)) {
                        iter.remove();
                    }
                }
                if (list != null) {
                    changes.add(list);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return list != null;
    }


    public void updateComment(@NotNull Client client,
                              @NotNull final P4ChangeListId p4id, @NotNull final String comment) throws VcsException {
        client.getServer().updateChangelistComment(p4id.getChangeListId(), comment);
        // Note: we don't reload the changelist.
        // Primarily this is because the only time we check the changelist comment in this tool is when we
        // are checking changelists against the IDEA changelists.  That only happens AFTER a cache reload.
        // Not only that, but updating a comment means that we already have a changelist match between the
        // two systems.
    }


    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        // do nothing
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "P4ChangeListCache";
    }


    @NotNull
    private static String getClientServerId(@NotNull Client client)
    {
        return getClientServerId(client.getConfig().getServiceName(), client.getClientName());
    }


    @NotNull
    private static String getClientServerId(@NotNull String serverConfigId, @NotNull String clientName)
    {
        return serverConfigId + ((char) 1) + clientName;
    }


    @Nullable
    private List<P4ChangeList> getCachedChangeListsFor(@NotNull Client client) {
        List<P4ChangeList> res;
        lock.readLock().lock();
        try {
            res = numberedCache.get(client.getConfig().getServiceName());
            if (res != null) {
                res = new ArrayList<P4ChangeList>(res);
            }
        } finally {
            lock.readLock().unlock();
        }
        return res;
    }
}
