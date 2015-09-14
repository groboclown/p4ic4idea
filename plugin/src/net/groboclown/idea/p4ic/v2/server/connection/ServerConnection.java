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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.RawServerExecutor;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateGroup;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.ui.alerts.ConfigurationProblemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The multi-threaded connections to the Perforce server for a specific client.
 * <p>
 * This is a future replacement for the {@link RawServerExecutor}, {@link ServerStoreService}, and
 * {@link net.groboclown.idea.p4ic.server.ServerStatus}.
 */
public class ServerConnection {
    private static final Logger LOG = Logger.getInstance(ServerConnection.class);
    private static final ThreadGroup CONNECTION_THREAD_GROUP = new ThreadGroup("Server Connection");
    private static final ThreadLocal<Boolean> THREAD_EXECUTION_ACTIVE = new ThreadLocal<Boolean>();
    private final Lock connectionLock = new ReentrantLock();
    private final BlockingQueue<UpdateAction> pendingUpdates = new LinkedBlockingDeque<UpdateAction>();
    private final Queue<UpdateAction> redo = new ArrayDeque<UpdateAction>();
    private final Lock redoLock = new ReentrantLock();
    private final AlertManager alertManager;
    private final ClientCacheManager cacheManager;
    private final ServerConfig config;
    private final ServerStatusController statusController;
    private final String clientName;
    private final Object clientExecLock = new Object();
    private final Thread background;
    private volatile boolean disposed = false;
    private boolean loadedPendingUpdateStates = false;
    @Nullable
    private volatile ClientExec clientExec;


    public static void assertInServerConnection() {
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        if (currentGroup != CONNECTION_THREAD_GROUP && THREAD_EXECUTION_ACTIVE.get() != Boolean.TRUE) {
            throw new IllegalStateException("Activity can only be run from within the ServerConnection action thread");
        }
    }


    public interface CreateUpdate {
        Collection<PendingUpdateState> create(@NotNull ClientCacheManager mgr);
    }


    public interface CacheQuery<T> {
        T query(@NotNull ClientCacheManager mgr) throws InterruptedException;
    }



    public ServerConnection(@NotNull final AlertManager alertManager,
            @NotNull ClientServerId clientServerId, @NotNull ClientCacheManager cacheManager,
            @NotNull ServerConfig config, @NotNull ServerStatusController statusController) {
        this.alertManager = alertManager;
        this.cacheManager = cacheManager;
        this.config = config;
        this.statusController = statusController;
        this.clientName = clientServerId.getClientId();

        background = new Thread(new QueueRunner());
        background.setDaemon(false);
        background.setPriority(Thread.NORM_PRIORITY - 1);
    }


    public synchronized void postSetup(@NotNull Project project) {
        if (! loadedPendingUpdateStates) {
            queueUpdateActions(project, cacheManager.getCachedPendingUpdates());
            loadedPendingUpdateStates = true;
        }
    }


    public void dispose() {
        disposed = true;
        background.interrupt();
        synchronized (clientExecLock) {
            if (clientExec != null) {
                clientExec.dispose();
            }
        }
    }



    public void queueAction(@NotNull Project project, @NotNull ServerUpdateAction action) {
        pendingUpdates.add(new UpdateAction(project, action));
    }

    /**
     * Run the command within the current thread.  This will still block if another action
     * is happening within the other thread.
     *
     * @param action action to run
     */
    public void runImmediately(@NotNull Project project, @NotNull ServerUpdateAction action)
            throws InterruptedException {
        startImmediateAction();
        try {
            action.perform(getExec(project), cacheManager, ServerConnection.this, alertManager);
        } catch (P4InvalidConfigException e) {
            alertManager.addCriticalError(new ConfigurationProblemHandler(), e);
        } finally {
            stopImmediateAction();
        }
    }

    @Nullable
    public <T> T query(@NotNull Project project, @NotNull ServerQuery<T> query) throws InterruptedException {
        startImmediateAction();
        try {
            return query.query(getExec(project), cacheManager, ServerConnection.this, alertManager);
        } catch (P4InvalidConfigException e) {
            alertManager.addCriticalError(new ConfigurationProblemHandler(), e);
            return null;
        } finally {
            stopImmediateAction();
        }
    }


    /**
     * Retry running a command that failed.  This should usually be put back at the head
     * of the action queue.  It is sometimes necessary if the command fails due to a
     * login or config issue.
     *
     * @param action action
     */
    public void requeueAction(@NotNull Project project, @NotNull ServerUpdateAction action) {
        redoLock.lock();
        try {
            redo.add(new UpdateAction(project, action));
        } finally {
            redoLock.unlock();
        }
    }


    public boolean isWorkingOnline() {
        return statusController.isWorkingOnline();
    }


    public boolean isWorkingOffline() {
        return statusController.isWorkingOffline();
    }





    public void queueUpdates(@NotNull Project project, @NotNull CreateUpdate update) {
        final Collection<PendingUpdateState> updates = update.create(cacheManager);
        List<PendingUpdateState> nonNullUpdates = new ArrayList<PendingUpdateState>(updates.size());
        for (PendingUpdateState updateState : updates) {
            if (updateState != null) {
                cacheManager.addPendingUpdateState(updateState);
                nonNullUpdates.add(updateState);
            }
        }
        queueUpdateActions(project, nonNullUpdates);
    }


    public <T> T cacheQuery(@NotNull CacheQuery<T> q) throws InterruptedException {
        return q.query(cacheManager);
    }


    P4Exec2 getExec(@NotNull Project project) throws P4InvalidConfigException {
        // double-check locking.  This is why clientExec must be volatile.
        synchronized (clientExecLock) {
            if (clientExec == null) {
                clientExec = new ClientExec(config, statusController, clientName);
            }
            return new P4Exec2(project, clientExec);
        }
    }


    private void queueUpdateActions(@NotNull Project project, @NotNull Collection<PendingUpdateState> updates) {
        UpdateGroup currentGroup = null;
        List<PendingUpdateState> currentGroupUpdates = null;
        for (PendingUpdateState update : updates) {
            if (currentGroup != null && !update.getUpdateGroup().equals(currentGroup)) {
                // new group, so add the old stuff and clear it out.
                if (!currentGroupUpdates.isEmpty()) {
                    queueAction(project,
                            currentGroup.getServerUpdateActionFactory().create(currentGroupUpdates));
                }
                currentGroupUpdates = null;
            }
            currentGroup = update.getUpdateGroup();
            if (currentGroupUpdates == null) {
                currentGroupUpdates = new ArrayList<PendingUpdateState>();
            }
            currentGroupUpdates.add(update);
        }
        if (currentGroup != null && currentGroupUpdates != null && !currentGroupUpdates.isEmpty()) {
            queueAction(project,
                    currentGroup.getServerUpdateActionFactory().create(currentGroupUpdates));
        }
    }


    void goOffline() {
        synchronized (clientExecLock) {
            if (clientExec != null) {
                clientExec.dispose();
                clientExec = null;
            }
        }
    }


    private void startImmediateAction() throws InterruptedException {
        connectionLock.lock();
        try {
            // FIXME make timeout configurable
            alertManager.waitForNoCriticalErrors(-1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            connectionLock.unlock();
            throw e;
        }
        THREAD_EXECUTION_ACTIVE.set(Boolean.TRUE);
    }

    private void stopImmediateAction() {
        THREAD_EXECUTION_ACTIVE.remove();
        connectionLock.unlock();
    }

    class QueueRunner implements Runnable {
        @Override
        public void run() {
            while (! disposed) {
                // Wait for something to do first
                UpdateAction action;
                redoLock.lock();
                try {
                    action = redo.poll();
                } finally {
                    redoLock.unlock();
                }
                if (action == null) {
                    try {
                        action = pendingUpdates.take();
                    } catch (InterruptedException e) {
                        // This triggers us to check for a disposed state.
                        LOG.info(e);
                        continue;
                    }
                }

                // wait for us to come online
                // FIXME make configurable
                try {
                    statusController.waitForOnline(-1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // need to reloop to check for disposed state, but first re-insert the update
                    LOG.info(e);
                    redoLock.lock();
                    try {
                        redo.add(action);
                    } finally {
                        redoLock.unlock();
                    }
                    continue;
                }

                // Now wait for the alerts to finish up.
                // FIXME make the wait time configurable
                connectionLock.lock();
                try {
                    alertManager.waitForNoCriticalErrors(-1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    connectionLock.unlock();

                    // need to reloop to check for disposed state, but first re-insert the update
                    LOG.info(e);
                    redoLock.lock();
                    try {
                        redo.add(action);
                    } finally {
                        redoLock.unlock();
                    }
                    continue;
                }


                if (statusController.isWorkingOffline()) {
                    // Went offline, probably due to the waited-on critical error.
                    // need to reloop to check for disposed state, but first re-insert the update
                    LOG.info("Went offline - retrying execution loop");
                    redoLock.lock();
                    try {
                        redo.add(action);
                    } finally {
                        redoLock.unlock();
                    }
                    continue;
                }


                // We now have something to do, and we can perform the action.
                // Note that it's possible for another critical error to pop up in this small
                // window of time; that's currently considered an acceptable situation.
                // Even if it does, it would be because of a separate server configuration,
                // and thus would be unrelated to this configuration's execution; we know this
                // because all executions of this connection are single-threaded.

                try {
                    action.action.perform(getExec(action.project), cacheManager, ServerConnection.this, alertManager);
                } catch (P4InvalidConfigException e) {
                    alertManager.addCriticalError(new ConfigurationProblemHandler(), e);
                } catch (InterruptedException e) {
                    // Do not requeue the action
                    LOG.info(e);
                } finally {
                    connectionLock.unlock();
                }
            }
        }
    }


    static class UpdateAction {
        final ServerUpdateAction action;
        final Project project;

        UpdateAction(@NotNull Project project, @NotNull ServerUpdateAction action) {
            this.action = action;
            this.project = project;
        }
    }
}