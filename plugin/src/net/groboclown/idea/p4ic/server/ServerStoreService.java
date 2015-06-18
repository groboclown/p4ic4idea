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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.background.VcsSettableFuture;
import net.groboclown.idea.p4ic.changes.P4ChangeListCache;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigListener;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.*;


/**
 * An application-wide service that keeps track of all the Perforce
 * server connections.  In this way, the user will only ever be
 * notified once for each Perforce server that is disconnected.
 */
public class ServerStoreService implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(ServerStoreService.class);

    private final Object sync = new Object();
    private final Map<ServerConfig, ServerData> servers = new HashMap<ServerConfig, ServerData>();
    private final MyConfigChangedListener configChangedListener = new MyConfigChangedListener();
    private MessageBusConnection appMessageBus;

    public static ServerStoreService getInstance() {
        return ServiceManager.getService(ServerStoreService.class);
    }


    @NotNull
    public ServerStatus getServerStatus(@NotNull Project project, @NotNull ServerConfig config)
            throws P4InvalidConfigException {
        ConnectionHandler.getHandlerFor(config).validateConfiguration(project, config);
        ServerData ret;
        synchronized (sync) {
            ret = servers.get(config);
            if (ret == null) {
                ret = new ServerData(config);
                servers.put(config, ret);
            }
            ret.addReference(project);
        }
        return ret;
    }


    public void changeServerConfig(@NotNull Project project, @NotNull ServerConfig oldServerConfig,
            @NotNull ServerConfig newServerConfig) {
        removeServerConfig(project, oldServerConfig);
        // The new server config will be implicitly added on the next getServerStatus call.
    }


    public void removeServerConfig(@NotNull Project project, @NotNull ServerConfig serverConfig) {
        synchronized (sync) {
            ServerData data = servers.get(serverConfig);
            if (data != null) {
                if (data.deregister(project)) {
                    data.forceDisconnect();
                    P4ChangeListCache.getInstance().invalidateServerCache(serverConfig);
                    servers.remove(serverConfig);
                }
            }
        }
    }


    @Override
    public void initComponent() {
        appMessageBus = ApplicationManager.getApplication().getMessageBus().connect();
        appMessageBus.subscribe(
                P4ConfigListener.TOPIC, configChangedListener);
    }

    @Override
    public void disposeComponent() {
        synchronized (sync) {
            for (ServerData data: servers.values()) {
                data.forceDisconnect();
            }
        }
        if (appMessageBus != null) {
            appMessageBus.disconnect();
            appMessageBus = null;
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Perforce Server Store Service";
    }

    public void workOnline() {
        synchronized (sync) {
            for (ServerData data: servers.values())
            {
                data.onReconnect();
            }
        }
    }


    private static class ServerData implements ServerStatus {
        private final Object connectionSync = new Object();
        private final Object changeSync = new Object();
        private final Object clientSync = new Object();
        private final ServerConfig config;
        private final Map<String, RawServerExecutor> clientExec = new HashMap<String, RawServerExecutor>();
        private volatile boolean onlineChanging;
        private final List<SoftReference<Project>> referenceCounts = new ArrayList<SoftReference<Project>>();

        // we are online by default, until told otherwise
        private volatile boolean isOnline = true;

        ServerData(@NotNull ServerConfig config) {
            this.config = config;
        }


        @Override
        public ServerExecutor getExecutorForClient(@NotNull Project project, @NotNull String clientName) {
            RawServerExecutor exec;
            addReference(project);
            synchronized (clientSync) {
                exec = clientExec.get(clientName);
                if (exec == null) {
                    exec = new RawServerExecutor(this, clientName);
                    clientExec.put(clientName, exec);
                }
            }
            return new ServerExecutor(project, exec);
        }


        private void addReference(@NotNull Project project) {
            synchronized (clientSync) {
                Iterator<SoftReference<Project>> iter = referenceCounts.iterator();
                boolean found = false;
                while (iter.hasNext()) {
                    SoftReference<Project> ref = iter.next();
                    Project p = ref.get();
                    // yes, ==
                    if (p == project) {
                        found = true;
                    } else if (p == null || p.isDisposed()) {
                        iter.remove();
                    }
                }
                if (! found) {
                    referenceCounts.add(new SoftReference<Project>(project));
                }
            }
        }


        private boolean isOrphaned() {
            return ! hasReferences();
        }


        private boolean hasReferences() {
            synchronized (clientSync) {
                if (referenceCounts.isEmpty()) {
                    return false;
                }
                Iterator<SoftReference<Project>> iter = referenceCounts.iterator();
                while (iter.hasNext()) {
                    SoftReference<Project> ref = iter.next();
                    Project p = ref.get();
                    if (p == null || p.isDisposed()) {
                        iter.remove();
                    }
                }
                return ! referenceCounts.isEmpty();
            }
        }


        private boolean deregister(@NotNull Project project) {
            synchronized (clientSync) {
                Iterator<SoftReference<Project>> iter = referenceCounts.iterator();
                while (iter.hasNext()) {
                    SoftReference<Project> ref = iter.next();
                    Project p = ref.get();
                    // yes, ==
                    if (p == null || p == project || p.isDisposed()) {
                        iter.remove();
                    }
                }
                return referenceCounts.isEmpty();
            }
        }


        @Nullable
        private Project getAnyProjectReference() {
            synchronized (clientSync) {
                Iterator<SoftReference<Project>> iter = referenceCounts.iterator();
                while (iter.hasNext()) {
                    SoftReference<Project> ref = iter.next();
                    Project p = ref.get();
                    if (p == null || p.isDisposed()) {
                        iter.remove();
                    } else {
                        // Don't loop through all the projects; no need right now.
                        return p;
                    }
                }
            }
            return null;
        }


        @Override
        public void changeClientName(@NotNull String oldClientName, @NotNull String newClientName) {
            synchronized (clientSync) {
                final RawServerExecutor oldServer = clientExec.remove(oldClientName);
                if (oldServer != null) {
                    oldServer.dispose();
                }

                // No need to add a new one for the new client; it is implicitly done on the get method.
            }
        }


        @Override
        public void removeClient(@NotNull String clientName) {
            // This will remove the old reference
            changeClientName(clientName, "");
        }


        @NotNull
        @Override
        public ServerConfig getConfig() {
            return config;
        }

        @Override
        public boolean isWorkingOffline() {
            // Using volatile
            //synchronized (changeSync) {
            //    synchronized (connectionSync) {
            //        return !isOnline;
            //    }
            //}
            return !isOnline;
        }

        @Override
        public boolean isWorkingOnline() {
            // Using volatile
            //synchronized (changeSync) {
            //    synchronized (connectionSync) {
            //        return isOnline;
            //    }
            //}
            return isOnline;
        }


        @Override
        public void onReconnect() {
            Project someProject = getAnyProjectReference();
            if (someProject == null) {
                // no references; abort
                return;
            }
            synchronized (changeSync) {
                while (onlineChanging) {
                    try {
                        changeSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                onlineChanging = true;

                // Test the connection
                // make the deep inspection think we're online
                boolean newExec = false;
                RawServerExecutor exec = null;
                try {
                    if (clientExec.isEmpty()) {
                        newExec = true;
                        exec = new RawServerExecutor(this, null);
                    } else {
                        exec = clientExec.values().iterator().next();
                    }

                    // Ensure this testing one thinks we're online before
                    // calling.
                    isOnline = true;
                    exec.wentOnline();

                    // This call should handle the disconnect calls.
                    exec.getUserClients(someProject);


                    // At this point, we're reconnected okay, so make the
                    // announcement
                    synchronized (connectionSync) {
                        for (RawServerExecutor exec2 : clientExec.values()) {
                            exec2.wentOnline();
                        }
                        for (SoftReference<Project> sp : referenceCounts) {
                            Project p = sp.get();
                            if (p != null && !p.isDisposed()) {
                                p.getMessageBus().
                                        syncPublisher(P4RemoteConnectionStateListener.TOPIC).
                                        onPerforceServerConnected(config);
                            }
                        }
                    }
                } catch (VcsException e) {
                    LOG.info("Reconnect failed", e);
                    for (SoftReference<Project> sp : referenceCounts) {
                        Project p = sp.get();
                        if (p != null && !p.isDisposed()) {
                            p.getMessageBus().
                                    syncPublisher(P4RemoteConnectionStateListener.TOPIC).
                                    onPerforceServerDisconnected(config);
                        }
                    }
                    isOnline = false;
                } finally {
                    if (newExec && exec != null) {
                        exec.dispose();
                    }
                    onlineChanging = false;
                    changeSync.notifyAll();
                }
            }
        }

        @Override
        public boolean onDisconnect() {
            if (! isOnline) {
                // user already decided to go offline.
                return false;
            }
            if (onlineChanging) {
                // Online mode already being adjusted.  Caller
                // should not retry the connection.
                return false;
            }

            synchronized (changeSync) {
                // try check again now that we're in the synchronized
                // block.  Note that onlineChanging is volatile, so
                // this double-checking will work.
                if (onlineChanging) {
                    return false;
                }
                onlineChanging = true;
                try {
                    synchronized (connectionSync) {
                        VcsSettableFuture<OnServerDisconnectListener.OnDisconnectAction> future =
                                VcsSettableFuture.create();
                        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                                OnServerDisconnectListener.TOPIC).onDisconnect(config, future);
                        boolean wentOffline;
                        try {
                            // This is the potential for a deadlock.  This
                            // is also why onlineChanging is checked outside
                            // the synchronized block.
                            wentOffline = future.get() ==
                                    OnServerDisconnectListener.OnDisconnectAction.WORK_OFFLINE;
                        } catch (VcsException e) {
                            wentOffline = true;
                        }

                        if (wentOffline) {
                            wentOffline();
                        } else {
                            wentOnline();
                        }
                        return ! wentOffline;
                    }
                } finally {
                    onlineChanging = false;
                    changeSync.notifyAll();
                }
            }
        }

        @Override
        public void forceDisconnect() {
            synchronized (changeSync) {
                while (onlineChanging) {
                    try {
                        changeSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (! isOnline) {
                    return;
                }
                onlineChanging = true;
                try {
                    wentOffline();
                } finally {
                    onlineChanging = false;
                    changeSync.notifyAll();
                }
            }
        }


        private void wentOnline() {
            synchronized (connectionSync) {
                isOnline = true;
                for (RawServerExecutor exec : clientExec.values()) {
                    exec.wentOnline();
                }
                for (SoftReference<Project> sp : referenceCounts) {
                    Project p = sp.get();
                    if (p != null && !p.isDisposed()) {
                        p.getMessageBus().
                                syncPublisher(P4RemoteConnectionStateListener.TOPIC).
                                onPerforceServerConnected(config);
                    }
                }
            }
        }


        // Must be run inside synchronized (changeSync), and inside
        // a try/catch block for setting the onlineChanging state.
        private void wentOffline() {
            synchronized (connectionSync) {
                isOnline = false;
                for (RawServerExecutor exec : clientExec.values()) {
                    exec.wentOffline();
                }
                for (SoftReference<Project> sp: referenceCounts) {
                    Project p = sp.get();
                    if (p != null && ! p.isDisposed()) {
                        p.getMessageBus().
                                syncPublisher(P4RemoteConnectionStateListener.TOPIC).
                                onPerforceServerDisconnected(config);
                    }
                }
            }
        }
    }


    private class MyConfigChangedListener implements P4ConfigListener {
        @Override
        public void configChanges(@NotNull Project project, @NotNull P4Config original, @NotNull P4Config config) {
            changeServerConfig(project,
                    ServerConfig.createOldServerConfig(original),
                    ServerConfig.createNewServerConfig(config));
        }

        @Override
        public void configurationProblem(@NotNull Project project, @NotNull P4Config config, @NotNull P4InvalidConfigException ex) {
            // ignore
            LOG.info("Problem in config: " + config, ex);
        }
    }
}
