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

package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class P4ChangeListCache implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(P4ChangeListCache.class);

    public static final int P4_DEFAULT = IChangelist.DEFAULT;
    public static final int P4_UNKNOWN = IChangelist.UNKNOWN;

    /** numbered changelists, by server */
    private final Map<String, List<P4ChangeList>> numberedCache = new HashMap<String, List<P4ChangeList>>();

    /** default changelists, by server + '@' + client */
    private final Map<String, P4ChangeList> defaultCache = new HashMap<String, P4ChangeList>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static P4ChangeListCache getInstance() {
        return ApplicationManager.getApplication().getComponent(P4ChangeListCache.class);
    }


    @NotNull
    public List<P4ChangeList> getChanges(@NotNull Client client) throws VcsException {
        lock.readLock().lock();
        try {
            List<P4ChangeList> res = numberedCache.get(getClientServerId(client));
            if (res == null) {
                reloadCache(client);
                res = numberedCache.get(getClientServerId(client));
            }
            List<P4ChangeList> ret = new ArrayList<P4ChangeList>(res);
            final P4ChangeList change = defaultCache.get(getClientServerId(client));
            if (change != null) {
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
            res = reloadCache(client);
        }
        return res;
    }


    @Nullable
    public P4ChangeList getChangeListForFile(@NotNull Client client, @NotNull FilePath file) throws VcsException {
        final List<P4ChangeList> res = getCachedChangeListsFor(client);
        if (res != null) {
            for (P4ChangeList p4cl: res) {
                for (P4FileInfo fileInfo: p4cl.getFiles()) {
                    if (file.equals(fileInfo.getPath())) {
                        return p4cl;
                    }
                }
            }
        }
        return null;
    }


    @Nullable
    public P4FileInfo getFileInChangelist(@NotNull Client client, @NotNull FilePath file) {
        final List<P4ChangeList> res = getCachedChangeListsFor(client);
        if (res != null) {
            for (P4ChangeList p4cl : res) {
                for (P4FileInfo fileInfo : p4cl.getFiles()) {
                    if (file.equals(fileInfo.getPath())) {
                        return fileInfo;
                    }
                }
            }
        }
        return null;
    }


    @NotNull
    public Map<Client, List<P4ChangeList>> reloadCachesFor(@NotNull final List<Client> clients) throws VcsException {
        Map<Client, List<P4ChangeList>> ret = new HashMap<Client, List<P4ChangeList>>();
        for (Client client: clients) {
            ret.put(client, reloadCache(client));
        }
        return ret;
    }


    @NotNull
    public List<P4ChangeList> reloadCache(final Client client) throws VcsException {
        // Reloading the changelists will mean reloading the cached server objects.
        client.getServer().invalidateCache();

        final List<P4ChangeList> serverCache = new ArrayList<P4ChangeList>();
        final Set<P4FileInfo> files = new HashSet<P4FileInfo>(
                client.getServer().loadOpenFiles(client.getRoots().toArray(new VirtualFile[client.getRoots().size()])));
        final List<IChangelistSummary> summaries = client.getServer().getPendingClientChangelists();

        // For each changelist, create the P4ChangeList object and remove the
        // found files.
        for (IChangelistSummary summary : summaries) {
            if (summary.getId() > 0) {
                Set<P4FileInfo> changelistFiles = new HashSet<P4FileInfo>();
                Iterator<P4FileInfo> iter = files.iterator();
                while (iter.hasNext()) {
                    final P4FileInfo file = iter.next();
                    if (file.getChangelist() == summary.getId()) {
                        LOG.debug("Adding " + file + " to changelist " + file.getChangelist());
                        changelistFiles.add(file);
                        iter.remove();
                    }
                }
                P4ChangeList change = new P4ChangeList(new P4ChangeListIdImpl(client, summary), changelistFiles,
                        summary.getDescription(), summary.getUsername(),
                        client.getServer().getJobsForChangelist(summary.getId()));
                serverCache.add(change);
                LOG.debug("change " + change.getId() + " contains files " + change.getFiles());
            }
        }

        // The remaining files are expected to be in default changelists.
        P4ChangeList defaultChange;
        Set<P4FileInfo> defaultChangeFiles = new HashSet<P4FileInfo>();
        final Iterator<P4FileInfo> iter = files.iterator();
        while (iter.hasNext()) {
            final P4FileInfo next = iter.next();
            if (next.getChangelist() <= P4_DEFAULT) {
                iter.remove();
                defaultChangeFiles.add(next);
                LOG.debug("file " + next + " added to default changelist");
            } else {
                LOG.warn("File " + next + " not added to changelist (should be in " + next.getChangelist() + ")");
            }
        }
        defaultChange = new P4ChangeList(new P4ChangeListIdImpl(client, P4_DEFAULT),
                defaultChangeFiles, null, null, null);
        LOG.debug("default change contains files " + defaultChange.getFiles());

        // If there are left over files, then that could be an error.
        if (! files.isEmpty()) {
            LOG.error("`p4 opened` returned files that are not in opened changelists: " +
                files);
        }


        lock.writeLock().lock();
        try {
            numberedCache.put(getClientServerId(client), serverCache);
            defaultCache.put(getClientServerId(client), defaultChange);
        } finally {
            lock.writeLock().unlock();
        }
        final ArrayList<P4ChangeList> ret = new ArrayList<P4ChangeList>(serverCache);
        ret.add(defaultChange);
        return ret;
    }


    @NotNull
    public P4ChangeListId createChangeList(@NotNull Client client, @NotNull String description) throws VcsException {
        final IChangelist p4cl = client.getServer().createChangelist(description);
        final P4ChangeList changeList = new P4ChangeList(new P4ChangeListIdImpl(client, p4cl),
                Collections.<P4FileInfo>emptyList(), p4cl.getDescription(), p4cl.getUsername(),
                client.getServer().getJobsForChangelist(p4cl.getId()));
        lock.writeLock().lock();
        try {
            // Can only ever be a numbered changelist
            List<P4ChangeList> changes = numberedCache.get(getClientServerId(client));
            if (changes == null) {
                changes = new ArrayList<P4ChangeList>();
            }
            // And it's only ever a new changelist, so no need to check if it's already added
            changes.add(changeList);
            numberedCache.put(getClientServerId(client), changes);
        } finally {
            lock.writeLock().unlock();
        }
        return changeList.getId();
    }


    /**
     * Add files to a changelist.
     *
     * @param client client
     * @param changeList the change to add the files to, or {@code null} for the default changelist.
     * @param affected affected files
     * @return messages for the operation
     * @throws VcsException
     */
    public List<P4StatusMessage> addFilesToChangelist(@NotNull Client client, @Nullable P4ChangeListId changeList,
            @NotNull List<FilePath> affected) throws VcsException {
        // Add in the files, then reload the cache for the changelist.
        final List<P4StatusMessage> messages =
                client.getServer().moveFilesToChangelist(
                        changeList == null ? P4_DEFAULT : changeList.getChangeListId(), affected);
        // We can't run just this one reload, because the from-changelist needs
        // to be reloaded as well.

        Set<P4ChangeListId> updatedChangelists = new HashSet<P4ChangeListId>();
        updatedChangelists.add(changeList);
        for (FilePath fp: affected) {
            P4ChangeList change = getChangeListForFile(client, fp);
            if (change != null) {
                updatedChangelists.add(change.getId());
            }
        }
        for (P4ChangeListId p4id: updatedChangelists) {
            reloadChangeList(client, p4id);
        }

        return messages;
    }


    /**
     * Called when the server is removed.
     *
     * @param serverConfig server config that was removed
     */
    public void invalidateServerCache(final @NotNull ServerConfig serverConfig) {
        lock.writeLock().lock();
        try {
            // None of this touches the Perforce server, so put it inside the lock
            // without an issue.
            Set<String> clientServerIds = new HashSet<String>();
            for (String key: numberedCache.keySet()) {
                if (isClientServerIdForServer(key, serverConfig)) {
                    clientServerIds.add(key);
                }
            }
            for (String key: defaultCache.keySet()) {
                if (isClientServerIdForServer(key, serverConfig)) {
                    clientServerIds.add(key);
                }
            }

            for (String key: clientServerIds) {
                numberedCache.remove(key);
                defaultCache.remove(key);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     *
     * @param client client
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
                    summary == null ? null : summary.getUsername(),
                    client.getServer().getJobsForChangelist(changeListId.getChangeListId()));
        }

        lock.writeLock().lock();
        try {
            if (changeListId.isDefaultChangelist()) {
                if (list != null) {
                    defaultCache.put(getClientServerId(client), list);
                }
            } else {
                List<P4ChangeList> changes = numberedCache.get(getClientServerId(client));
                if (changes == null) {
                    if (list == null) {
                        // early exit - nothing to remove
                        return false;
                    }
                    changes = new ArrayList<P4ChangeList>();
                    numberedCache.put(getClientServerId(client), changes);
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


    @NotNull
    public Collection<P4FileInfo> getOpenedFiles(@NotNull Client client) throws VcsException {
        final List<P4ChangeList> changeLists = getChangeListsFor(client);
        final Set<P4FileInfo> ret = new HashSet<P4FileInfo>();
        for (P4ChangeList changeList : changeLists) {
            ret.addAll(changeList.getFiles());
        }
        return ret;
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


    /**
     *
     *
     * @param client
     * @deprecated ClientServerId
     * @return
     */
    @NotNull
    private static String getClientServerId(@NotNull Client client)
    {
        return getClientServerId(client.getConfig().getServiceName(), client.getClientName());
    }


    /**
     *
     * @param serverConfigId
     * @param clientName
     * @deprecated ClientServerId
     * @return
     */
    @NotNull
    private static String getClientServerId(@NotNull String serverConfigId, @NotNull String clientName)
    {
        return serverConfigId + ((char) 1) + clientName;
    }


    private static boolean isClientServerIdForServer(@NotNull String clientServerId, @NotNull ServerConfig server) {
        return clientServerId.startsWith(server.getServiceName() + ((char) 1));
    }


    @Nullable
    private List<P4ChangeList> getCachedChangeListsFor(@NotNull Client client) {
        final String serviceName = getClientServerId(client);
        final P4ChangeList defaultChange;
        List<P4ChangeList> res;
        lock.readLock().lock();
        try {
            res = numberedCache.get(serviceName);
            if (res != null) {
                res = new ArrayList<P4ChangeList>(res);
            }
            defaultChange = defaultCache.get(serviceName);
        } finally {
            lock.readLock().unlock();
        }

        if (defaultChange != null) {
            if (res == null) {
                // This can definitely happen on first load, or if there were
                // no changes in the changelist.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Default change exists for " + client + " when no numbered cache exists");
                }
                res = new ArrayList<P4ChangeList>();
            }
            res.add(defaultChange);
            if (LOG.isDebugEnabled()) {
                LOG.debug(client + ": default change has " + defaultChange.getFiles().size() + " files");
            }
        } else if (res != null) {
            LOG.error("No default changelist for " + client);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("cached changes for " + client + " are " + res + " (" + (res == null ? -1 : res.size()) + ")");
        }

        return res;
    }
}
