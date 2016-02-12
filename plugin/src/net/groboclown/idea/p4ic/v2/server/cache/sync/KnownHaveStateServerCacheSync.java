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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateGroup;
import net.groboclown.idea.p4ic.v2.server.cache.state.FileMappingRepo;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileSyncState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.groboclown.idea.p4ic.v2.server.cache.state.CachedState.NEVER_LOADED;

/**
 * Keeps track of all the job status states.  It does not have any ability to have
 * the client change those; that's the pervue of the admin, and must be done
 * meticulously, not just willy-nilly in the IDE.
 */
class KnownHaveStateServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(KnownHaveStateServerCacheSync.class);
    private final Set<P4FileSyncState> cachedServerState;
    private final FileMappingRepo fileMappingRepo;
    private final Lock serverStateLock = new ReentrantLock();
    private Date lastRefresh = NEVER_LOADED;


    KnownHaveStateServerCacheSync(@NotNull final Set<P4FileSyncState> cachedServerState,
            final FileMappingRepo fileMappingRepo) {
        this.cachedServerState = cachedServerState;
        this.fileMappingRepo = fileMappingRepo;
        for (P4FileSyncState syncState : cachedServerState) {
            if (lastRefresh.before(syncState.getLastUpdated())) {
                lastRefresh = syncState.getLastUpdated();
            }
        }
    }


    @NotNull
    private VirtualFile[] getKnownFilePaths() {
        List<VirtualFile> ret;
        serverStateLock.lock();
        try {
            ret = new ArrayList<VirtualFile>(cachedServerState.size());
            for (P4FileSyncState p4FileSyncState : cachedServerState) {
                ret.add(p4FileSyncState.getVirtualFile());
            }
        } finally {
            serverStateLock.unlock();
        }
        return ret.toArray(new VirtualFile[ret.size()]);
    }


    @NotNull
    public Map<VirtualFile, P4FileSyncState> getHaveFiles(@NotNull final Collection<VirtualFile> haves) {
        return mapToStates(haves);
    }


    @NotNull
    ServerQuery<Map<VirtualFile, P4FileSyncState>> createRefreshQuery(
            final Collection<VirtualFile> haves) {
        return new ServerQuery<Map<VirtualFile, P4FileSyncState>>() {
            @Nullable
            @Override
            public Map<VirtualFile, P4FileSyncState> query(@NotNull final P4Exec2 exec,
                    @NotNull final ClientCacheManager cacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                ServerConnection.assertInServerConnection();

                // We have a lock, so setup the mapping that we'll return.
                Map<VirtualFile, P4FileSyncState> ret = mapToStates(haves);

                loadServerCache(exec, alerts, ret);
                return ret;
            }
        };
    }


    protected final void loadServerCache(@NotNull P4Exec2 exec,
            @NotNull AlertManager alerts, Map<VirtualFile, P4FileSyncState> haves) {
        // Find just the have files that need a cache update.
        List<P4FileSyncState> toUpdate = new ArrayList<P4FileSyncState>(haves.size());
        for (Entry<VirtualFile, P4FileSyncState> entry : haves.entrySet()) {
            if (needsRefresh(entry.getValue())) {
                toUpdate.add(entry.getValue());
            }
        }


        if (! toUpdate.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Refreshing the cache for " +
                        getClass().getSimpleName() + "; last refresh was " +
                        getLastRefreshDate());
            }
            innerLoadServerCache(exec, alerts, toUpdate);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("No need to refresh the cache for " + getClass()
                    .getSimpleName() + "; last refresh was " + getLastRefreshDate());
        }
    }


    @Override
    protected void innerLoadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
        Set<P4FileSyncState> toUpdate;
        serverStateLock.lock();
        try {
            toUpdate = new HashSet<P4FileSyncState>(cachedServerState);
        } finally {
            serverStateLock.unlock();
        }
        innerLoadServerCache(exec, alerts, toUpdate);
    }


    private void innerLoadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts,
            @NotNull Collection<P4FileSyncState> toUpdate) {
        ServerConnection.assertInServerConnection();

        // Indexes should all match up.
        List<P4FileSyncState> syncStates = new ArrayList<P4FileSyncState>(toUpdate);
        List<IFileSpec> syncFiles = new ArrayList<IFileSpec>(syncStates.size());
        try {
            for (P4FileSyncState syncState : syncStates) {
                syncFiles.add(syncState.getFileSpec());
            }
        } catch (P4Exception e) {
            LOG.error(e);
            return;
        }
        final List<IFileSpec> haveList;
        try {
            haveList = exec.getHaveList(syncFiles);
        } catch (VcsException e) {
            LOG.info("Files with problems: " + syncStates);
            alerts.addWarning(
                    exec.getProject(),
                    P4Bundle.message("error.load-have.title"),
                    P4Bundle.message("error.load-have"),
                    e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
            // cannot use the cached server state, so clear it out.
            serverStateLock.lock();
            try {
                cachedServerState.clear();
            } finally {
                serverStateLock.unlock();
            }
            return;
        }
        if (haveList.size() != syncStates.size()) {
            LOG.info("Files with problems: " + syncStates + "; found " + haveList);
            alerts.addWarning(
                    exec.getProject(),
                    P4Bundle.message("error.load-have.title"),
                    P4Bundle.message("error.load-have"),
                    null, getKnownFilePaths());
            // cannot use the cached server state, so clear it out.
            serverStateLock.lock();
            try {
                cachedServerState.clear();
            } finally {
                serverStateLock.unlock();
            }
            return;
        }

        for (int i = 0; i < syncStates.size(); i++) {
            syncStates.get(i).update(haveList.get(i), fileMappingRepo);
        }
        lastRefresh = new Date();
    }

    @Override
    protected void rectifyCache(@NotNull final Project project,
            @NotNull final Collection<PendingUpdateState> pendingUpdateStates,
            @NotNull final AlertManager alerts) {
        // Nothing to do
    }

    @NotNull
    @Override
    protected Collection<UpdateGroup> getSupportedUpdateGroups() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefresh;
    }

    @Override
    protected void checkLocalIntegrity(@NotNull final List<PendingUpdateState> pendingUpdates) {
        // nothing to do, because there are no local changed versions
    }

    @NotNull
    private Map<VirtualFile, P4FileSyncState> mapToStates(@NotNull final Collection<VirtualFile> haves) {
        Map<VirtualFile, P4FileSyncState> ret = new HashMap<VirtualFile, P4FileSyncState>();
        serverStateLock.lock();
        try {
            for (VirtualFile have : haves) {
                ret.put(have, getSyncState(have));
            }
        } finally {
            serverStateLock.unlock();
        }
        return ret;
    }

    // if the have file isn't in the cache, we'll create it.
    @NotNull
    private P4FileSyncState getSyncState(@NotNull final VirtualFile have) {
        for (P4FileSyncState p4FileSyncState : cachedServerState) {
            if (have.equals(p4FileSyncState.getVirtualFile())) {
                return p4FileSyncState;
            }
        }

        final P4ClientFileMapping location =
                fileMappingRepo.getByLocation(FilePathUtil.getFilePath(have));
        P4FileSyncState p4FileSyncState = new P4FileSyncState(location);
        cachedServerState.add(p4FileSyncState);
        // force a sync, because we don't have this file.
        lastRefresh = NEVER_LOADED;
        return p4FileSyncState;
    }

    private boolean needsRefresh(P4FileSyncState state) {
        return (state.getLastUpdated().getTime() + MIN_REFRESH_INTERVAL_MS < System.currentTimeMillis());
    }
}
