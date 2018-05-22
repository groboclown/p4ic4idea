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

package net.groboclown.p4.server.impl.cache;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.cache.messagebus.AbstractCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.DescribeChangelistCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.FileActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobSpecCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ListClientsForUserCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheMessage;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Listens to cache update events, and updates the local project cache store.
 */
public class CacheStoreUpdateListener implements Disposable {
    private static final Logger LOG = Logger.getInstance(CacheStoreUpdateListener.class);

    private final ProjectCacheStore cache;
    private final String cacheId = AbstractCacheMessage.createCacheId();
    private boolean disposed = false;

    public CacheStoreUpdateListener(@NotNull ProjectCacheStore cache) {
        this.cache = cache;

        MessageBusClient.ApplicationClient mbClient = MessageBusClient.forApplication(this);
        CacheListener listener = new CacheListener();
        ClientActionMessage.addListener(mbClient, cacheId, listener);
        ClientOpenCacheMessage.addListener(mbClient, cacheId, listener);
        DescribeChangelistCacheMessage.addListener(mbClient, cacheId, listener);
        FileActionMessage.addListener(mbClient, cacheId, listener);
        JobCacheMessage.addListener(mbClient, cacheId, listener);
        JobSpecCacheMessage.addListener(mbClient, cacheId, listener);
        ListClientsForUserCacheMessage.addListener(mbClient, cacheId, listener);
        ServerActionCacheMessage.addListener(mbClient, cacheId, listener);

        // TODO perhaps add in listener for client config removal
    }


    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        // TODO dispose the cache data
    }

    /**
     * Update the opened file and changelist cache for the configuration.
     *
     * @param ref source
     * @param pendingChangelists pending changelists for the source
     * @param openedFiles files open for change in the source
     */
    public void setOpenedChanges(ClientServerRef ref,
            Collection<P4LocalChangelist> pendingChangelists,
            Collection<P4LocalFile> openedFiles) {
        // FIXME implement
        LOG.warn("FIXME implement setOpenedChanges");
    }


    // The listener doesn't need to be aware of complications around added pending events
    // conflicting with each other (for example, add file then remove file), because there's a
    // chance that the pending events are queued up while the server is taking a long time to
    // respond, rather than being offline.  The complications of understanding the order of
    // pending operations and their impact on interpretation is handled by the cache query
    // handler.

    private class CacheListener
            implements ClientActionMessage.Listener, ClientOpenCacheMessage.Listener,
            DescribeChangelistCacheMessage.Listener, FileActionMessage.Listener, JobCacheMessage.Listener,
            JobSpecCacheMessage.Listener, ListClientsForUserCacheMessage.Listener, ServerActionCacheMessage.Listener {

        @Override
        public void clientActionUpdate(@NotNull ClientActionMessage.Event event) {
            try {
                switch (event.getState()) {
                    case PENDING:
                        cache.writeActions(event.getClientRef(), (cache) -> {
                            cache.addAction(event.getAction());
                        });
                        break;
                    case COMPLETED:
                    case FAILED:
                        // For the purposes of this cache class, all we care about for the
                        // fail and completed cases is removing the pending action.
                        cache.writeActions(event.getClientRef(), (cache) -> {
                            cache.removeActionById(event.getAction().getActionId());
                        });
                        break;
                }
            } catch (InterruptedException e) {
                LOG.error("Waited too long for the write lock for accessing the server cache.", e);
            }
        }

        @Override
        public void openFilesChangelistsUpdated(@NotNull ClientOpenCacheMessage.Event event) {
            setOpenedChanges(event.getClientRef(), event.getPendingChangelists(), event.getOpenedFiles());
        }

        @Override
        public void describeChangelistUpdate(@NotNull DescribeChangelistCacheMessage.Event event) {

        }

        @Override
        public void fileActionUpdate(@NotNull FileActionMessage.Event event) {

        }

        @Override
        public void jobUpdate(@NotNull JobCacheMessage.Event event) {

        }

        @Override
        public void jobSpecUpdate(@NotNull JobSpecCacheMessage.Event event) {

        }

        @Override
        public void listClientsForUserUpdate(@NotNull ListClientsForUserCacheMessage.Event event) {

        }

        @Override
        public void serverActionUpdate(@NotNull ServerActionCacheMessage.Event event) {

        }
    }
}
