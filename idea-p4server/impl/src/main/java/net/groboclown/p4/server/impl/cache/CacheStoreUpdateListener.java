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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4.server.api.cache.CachePendingActionHandler;
import net.groboclown.p4.server.api.cache.messagebus.AbstractCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.DescribeChangelistCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.FileActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.FileCacheUpdatedMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobSpecCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ListClientsForUserCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheMessage;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.util.FileTreeUtil;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Listens to cache update events, and updates the local project cache store.
 */
public class CacheStoreUpdateListener {
    private static final Logger LOG = Logger.getInstance(CacheStoreUpdateListener.class);
    private static final String SERVER_CACHE_TIMEOUT_MESSAGE = "Waited too long for the write lock for accessing the server cache.";

    private final Project project;
    private final ProjectCacheStore cache;
    private final CachePendingActionHandler pendingCache;

    // TODO change this so that the creation is one call, and registering the listener
    // is another call, that takes the appClient and projectClient as arguments.
    // In this way, the registration / disposable stuff is clear to the caller and
    // not needed to know by this object.
    public CacheStoreUpdateListener(@NotNull Project project,
            @NotNull ProjectCacheStore cache, @NotNull Disposable disposableParent) {
        this.project = project;
        this.cache = cache;

        // TODO should be an argument, rather than the cache directly?
        this.pendingCache = new CachePendingActionHandlerImpl(cache);

        MessageBusClient.ApplicationClient appClient = MessageBusClient.forApplication(disposableParent);
        MessageBusClient.ProjectClient projectClient = MessageBusClient.forProject(project, disposableParent);
        CacheListener listener = new CacheListener();
        String cacheId = AbstractCacheMessage.createCacheId(project, CacheStoreUpdateListener.class);
        ClientActionMessage.addListener(appClient, cacheId, listener);

        // TODO this causes the event listener to be fired twice.
        // Somewhere, this ClientOpenCacheMessage call invokes the exact same listener
        // method twice.  Either the listener is registered twice, or the event object
        // is being passed to send() twice.
        ClientOpenCacheMessage.addListener(appClient, cacheId, listener);

        DescribeChangelistCacheMessage.addListener(appClient, cacheId, listener);

        FileActionMessage.addListener(appClient, cacheId, listener);

        JobCacheMessage.addListener(appClient, cacheId, listener);
        JobSpecCacheMessage.addListener(appClient, cacheId, listener);
        ListClientsForUserCacheMessage.addListener(appClient, cacheId, listener);
        ServerActionCacheMessage.addListener(appClient, cacheId, listener);
        ClientConfigRemovedMessage.addListener(projectClient, cacheId, listener);
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Opened changes for reference " + ref);
            LOG.debug("Opened changelists: " + pendingChangelists);
            LOG.debug("Opened files: " + openedFiles);
        }

        // Debug data.
        Set<P4LocalFile> unusedFiles = new HashSet<>(openedFiles);

        for (ClientConfigRoot root : getClientConfigRoots()) {
            if (ref.equals(root.getClientConfig().getClientServerRef())) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Writing data to the cache of root " + root);
                    }
                    cache.write(root.getClientConfig(), (store) -> {
                        List<P4LocalFile> rootFiles = new ArrayList<>();
                        for (P4LocalFile openedFile : openedFiles) {
                            if (FileTreeUtil.isSameOrUnder(root.getClientRootDir(), openedFile.getFilePath())) {
                                rootFiles.add(openedFile);
                            } else if (LOG.isDebugEnabled()) {
                                LOG.debug("File " + openedFile.getFilePath() + " not under root " +
                                        root.getClientRootDir());
                            }
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Associating with " + root + " files " + rootFiles);
                        }
                        unusedFiles.removeAll(rootFiles);
                        store.setFiles(rootFiles);

                        store.setChangelists(pendingChangelists);
                    });
                } catch (InterruptedException e) {
                    InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                            "Could not write to the cache due to lock timeout", e)));
                }
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Root " + root + ", ref " + root.getClientConfig().getClientServerRef() +
                        " is not the same ref as " + ref);
                LOG.debug("param ref: clientname: [" + ref.getClientName() + "]; server [" +
                        ref.getServerName().getServerPort() + "]; protocol [" + ref.getServerName().getServerProtocol() + "]");
                ClientServerRef rt = root.getClientConfig().getClientServerRef();
                LOG.debug("root ref: clientname: [" + rt.getClientName() + "]; server [" +
                        rt.getServerName().getServerPort() + "]; protocol [" + rt.getServerName().getServerProtocol() + "]");
            }
        }

        // Cache store completed refresh.  Send a notification.
        //FileCacheUpdatedMessage.send(project).onFilesCacheUpdated(new FileCacheUpdatedMessage.FileCacheUpdateEvent(
        //        openedFiles.toArray(new P4LocalFile[0])));

        if (LOG.isDebugEnabled() && !unusedFiles.isEmpty()) {
            LOG.debug("Unused file associations: " + unusedFiles);
        }
    }

    private Collection<ClientConfigRoot> getClientConfigRoots() {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? Collections.emptyList() : reg.getClientConfigRoots();
    }

    private void handleClientAction(@NotNull ClientActionMessage.Event event)
            throws InterruptedException {
        switch (event.getState()) {
            case PENDING: {
                pendingCache.writeActions(event.getClientRef(), (cache) ->
                        cache.addAction(event.getAction()));
                break;
            }
            case COMPLETED:
            case FAILED:
                // For the purposes of this cache class, all we care about for the
                // fail and completed cases is removing the pending action.
                pendingCache.writeActions(event.getClientRef(), (cache) ->
                        cache.removeActionById(event.getAction().getActionId()));

                // handling changelist specific changes to the IDE mappings is done by P4ChangeProvider

                break;
        }
        if (event.getClientRef() != null) {
            sendCachedFiles(event.getClientRef());
        }
    }

    private void sendCachedFiles(@NotNull ClientServerRef config)
            throws InterruptedException {
        FileCacheUpdatedMessage.send(project).onFilesCacheUpdated(new FileCacheUpdatedMessage.FileCacheUpdateEvent(
                pendingCache.readActions(config, (Function<Stream<ActionChoice>, FilePath[]>) actions -> {
                    List<FilePath> files = new ArrayList<>();
                    actions.forEach((a) -> files.addAll(a.getAffectedFiles()));
                    return files.toArray(new FilePath[0]);
                })
        ));
    }

    @NotNull
    private Collection<ClientConfig> getActiveClientConfigs() {
        final Set<ClientConfig> configs = new HashSet<>();
        getClientConfigRoots().stream()
                .map(ClientConfigRoot::getClientConfig)
                .forEach(configs::add);
        return configs;
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
            JobSpecCacheMessage.Listener, ListClientsForUserCacheMessage.Listener, ServerActionCacheMessage.Listener,
            ClientConfigRemovedMessage.Listener{

        @Override
        public void clientActionUpdate(@NotNull ClientActionMessage.Event event) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Caching action " + event.getAction());
            }
            try {
                handleClientAction(event);
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
        }

        @Override
        public void openFilesChangelistsUpdated(@NotNull ClientOpenCacheMessage.Event event) {
            setOpenedChanges(event.getClientRef(), event.getPendingChangelists(), event.getOpenedFiles());
        }

        @Override
        public void describeChangelistUpdate(@NotNull DescribeChangelistCacheMessage.Event event) {
            // Note: not caching the committed changelist details, because the IDE handles that for us.
            // addChangelistDetails(event.getRequestedChangelist(), event.getUpdatedChangelist());
        }

        @Override
        public void fileActionUpdate(@NotNull FileActionMessage.Event event) {
            try {
                if (event.getState() == FileActionMessage.ActionState.PENDING) {
                    pendingCache.writeActions(event.getClientRef(), (store) -> store.addAction(event.getClientAction()));
                } else {
                    pendingCache.writeActions(event.getClientRef(),
                            (store) -> store.removeActionById(event.getClientAction().getActionId()));
                }
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
            // Do not fire a cache update message.
        }

        @Override
        public void jobUpdate(@NotNull JobCacheMessage.Event event) {
            try {
                cache.write(event.getServerName(), (store) -> store.addJob(event.getJob()));
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
        }

        @Override
        public void jobSpecUpdate(@NotNull JobSpecCacheMessage.Event event) {
            try {
                cache.write(event.getServerName(), (store) -> store.setJobSpec(event.getJobSpec()));
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
        }

        @Override
        public void listClientsForUserUpdate(@NotNull ListClientsForUserCacheMessage.Event event) {
            try {
                cache.write(event.getServerName(), (store) -> store.setUserClients(event.getUser(), event.getClients()));
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
        }

        @Override
        public void serverActionUpdate(@NotNull ServerActionCacheMessage.Event event) {
            try {
                if (event.getState() == ServerActionCacheMessage.ActionState.PENDING) {
                    // Add the event
                    pendingCache.writeActions(event.getServerName(), (store) -> store.addAction(event.getServerAction()));
                } else {
                    // Remove the event
                    pendingCache.writeActions(event.getServerName(),
                            (store) -> store.removeActionById(event.getServerAction().getActionId()));
                }
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
        }

        @Override
        public void clientConfigurationRemoved(@NotNull ClientConfigRemovedMessage.Event event) {
            Collection<ClientConfig> activeConfigs = getActiveClientConfigs();
            try {
                cache.cleanClientCache(activeConfigs);
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(
                        SERVER_CACHE_TIMEOUT_MESSAGE, e)));
            }
        }
    }
}
