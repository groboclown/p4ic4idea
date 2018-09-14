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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.server.IServerInfo;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.server.VcsExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4ConnectionDisposedException;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateGroup;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.server.connection.Synchronizer.ActionRunner;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import net.groboclown.idea.p4ic.v2.ui.alerts.ConfigurationProblemHandler;
import net.groboclown.idea.p4ic.v2.ui.alerts.DisconnectedHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The multi-threaded connections to the Perforce server for a specific client.
 */
public class ServerConnection {
    private static final Logger LOG = Logger.getInstance(ServerConnection.class);
    private static final ThreadGroup CONNECTION_THREAD_GROUP = new ThreadGroup("P4ServerName Connection");
    private static final ThreadLocal<Boolean> THREAD_EXECUTION_ACTIVE = new ThreadLocal<Boolean>();
    private final BlockingQueue<UpdateAction> pendingUpdates = new LinkedBlockingDeque<UpdateAction>();
    private final Queue<UpdateAction> redo = new ArrayDeque<UpdateAction>();
    private final Lock redoLock = new ReentrantLock();
    private final AlertManager alertManager;
    private final ClientCacheManager cacheManager;
    private final ClientConfig config;
    private final ServerStatusController statusController;
    private final Lock clientExecLock = new ReentrantLock();
    private final Thread background;
    private final Synchronizer.ServerSynchronizer.ConnectionSynchronizer synchronizer;
    private volatile boolean disposed = false;
    private boolean loadedPendingUpdateStates = false;
    private volatile boolean setup = false;
    @Nullable
    private ClientExec clientExec;


    public static void assertInServerConnection() {
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        // We want to compare the get() against an actual value;
        // this means that the comparison will include null value checks.
        if (currentGroup != CONNECTION_THREAD_GROUP && THREAD_EXECUTION_ACTIVE.get() != Boolean.TRUE) {
            throw new IllegalStateException("Activity can only be run from within the ServerConnection action thread");
        }
    }


    public interface CreateUpdate {
        @NotNull
        Collection<PendingUpdateState> create(@NotNull ClientCacheManager mgr);
    }


    public interface CacheQuery<T> {
        T query(@NotNull ClientCacheManager mgr)
                throws InterruptedException;
    }


    public ServerConnection(@NotNull final AlertManager alertManager,
            @NotNull final ClientCacheManager cacheManager,
            @NotNull final ClientConfig config, @NotNull final ServerStatusController statusController,
            @NotNull final Synchronizer.ServerSynchronizer.ConnectionSynchronizer synchronizer,
            @Nullable final ClientExec initial) {
        this.synchronizer = synchronizer;
        this.alertManager = alertManager;
        this.cacheManager = cacheManager;
        this.config = config;
        this.statusController = statusController;
        this.clientExec = initial;

        background = new Thread(new QueueRunner(), "P4 P4ServerName Connection");
        background.setDaemon(false);
        background.setPriority(Thread.NORM_PRIORITY - 1);
        background.start();
    }


    public void postSetup(@NotNull final Project project) {
        if (setup) {
            // Already setup, or in the process of being
            // setup.
            return;
        }

        // This can severely hang the UI during startup, so run
        // in a background thread.
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                clientExecLock.lock();
                try {
                    // Already setup check, now that we're in the lock.
                    if (setup) {
                        return;
                    }

                    // Mark the connection has having been setup,
                    // so that we don't try to re-enter this
                    // method and possibly deadlock.
                    setup = true;

                    // First, check if the server is reachable.
                    // This is necessary to keep the pending changes from being gobbled
                    // up if we have a mistaken online mode set.  If we know we're
                    // working offline, then don't check if we're online (especially since
                    // the user can manually switch to offline mode).
                    checkIfOnline(project);

                    // Push all the cached pending updates into the queue for future
                    // processing.
                    if (!loadedPendingUpdateStates) {
                        queueUpdateActions(project, cacheManager.getCachedPendingUpdates());
                        loadedPendingUpdateStates = true;
                    }
                } finally {
                    clientExecLock.unlock();
                }
            }
        });
    }


    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        background.interrupt();
        if (clientExec != null) {
            // If the server communication is taking a really long
            // time, this lock attempt can block the EDT.  So, do
            // a quick attempt to lock, and if that fails, then
            // dispose in the background.
            if (clientExecLock.tryLock()) {
                try {
                    if (clientExec != null) {
                        clientExec.dispose();
                        clientExec = null;
                    }
                } finally {
                    clientExecLock.unlock();
                }
            } else {
                // could not dispose right now, so do it in the background.
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        clientExecLock.lock();
                        try {
                            if (clientExec != null) {
                                clientExec.dispose();
                                clientExec = null;
                            }
                        } finally {
                            clientExecLock.unlock();
                        }
                    }
                });
            }
        }
    }


    private void queueAction(@NotNull Project project, @NotNull ServerUpdateAction action) {
        LOG.info("Queueing action for execution: " + action);
        pendingUpdates.add(new UpdateAction(project, action));
    }

    /**
     * Run the command within the current thread.  This will still block if another action
     * is happening within the other thread.
     *
     * @param action action to run
     */
    public void runImmediately(@NotNull final Project project, @NotNull final ServerUpdateAction action)
            throws InterruptedException {
        synchronizer.runImmediateAction(new ActionRunner<Void>() {
            @Override
            public Void perform(@NotNull SynchronizedActionRunner runner)
                    throws InterruptedException {
                try {
                    THREAD_EXECUTION_ACTIVE.set(Boolean.TRUE);
                    action.perform(getExec(project), cacheManager, ServerConnection.this, runner, alertManager);
                } catch (P4InvalidConfigException e) {
                    alertManager.addCriticalError(new ConfigurationProblemHandler(project, statusController, e), e);
                } catch (P4ConnectionDisposedException e) {
                    LOG.info("Ran immediate action on disposed connection", e);
                } finally {
                    THREAD_EXECUTION_ACTIVE.remove();
                }
                return null;
            }
        });
    }

    @Nullable
    public <T> T query(@NotNull final Project project, @NotNull final ServerQuery<T> query)
            throws InterruptedException {
        return synchronizer.runImmediateAction(new ActionRunner<T>() {
            @Override
            public T perform(@NotNull SynchronizedActionRunner runner)
                    throws InterruptedException {
                try {
                    THREAD_EXECUTION_ACTIVE.set(Boolean.TRUE);
                    return query.query(getExec(project), cacheManager, ServerConnection.this, runner, alertManager);
                } catch (P4InvalidConfigException e) {
                    alertManager.addCriticalError(new ConfigurationProblemHandler(project, statusController, e), e);
                } catch (P4ConnectionDisposedException e) {
                    LOG.info("Ran query on disposed server", e);
                } finally {
                    THREAD_EXECUTION_ACTIVE.remove();
                }
                return null;
            }
        });
    }


    /**
     * Retry running a command that failed.  This should usually be put back at the head
     * of the action queue.  It is sometimes necessary if the command fails due to a
     * login or config issue.
     *
     * @param action action
     */
    public void requeueAction(@NotNull Project project, @NotNull ServerUpdateAction action) {
        pushAbortedAction(new UpdateAction(project, action));
    }


    // FIXME see if this is the right place for this action.
    public void flushCache(@NotNull Project project, final boolean includeLocal, final boolean force)
            throws InterruptedException {
        if (isWorkingOnline()) {
            runImmediately(project, new ServerUpdateAction() {
                @NotNull
                @Override
                public Collection<PendingUpdateState> getPendingUpdateStates() {
                    return Collections.emptyList();
                }

                @Override
                public void perform(@NotNull
                final P4Exec2 exec, @NotNull
                final ClientCacheManager clientCacheManager,
                        @NotNull
                        final ServerConnection connection, @NotNull
                final SynchronizedActionRunner syncRunner,
                        @NotNull
                        final AlertManager alerts)
                        throws InterruptedException {
                    // TODO does this need a read lock?
                    ServerConnectionManager.getInstance().flushCache(
                            clientCacheManager.getClientServerId(), includeLocal, force);
                }

                @Override
                public void abort(@NotNull
                final ClientCacheManager clientCacheManager) {

                }
            });
        }
    }


    public boolean isWorkingOnline() {
        return statusController.isWorkingOnline();
    }


    public boolean isWorkingOffline() {
        return statusController.isWorkingOffline();
    }


    public void workOffline() {
        statusController.disconnect();
    }


    public void workOnline(@NotNull Project project) {
        statusController.connect(project);
    }

    public ServerConnectedController getServerConnectedController() {
        return statusController;
    }


    public boolean isValid() {
        return statusController.isValid();
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


    public <T> T cacheQuery(@NotNull CacheQuery<T> q)
            throws InterruptedException {
        return q.query(cacheManager);
    }


    /**
     * Used in the few rare cases where the client connection is used outside the
     * scope of the P4Server objects.  If we don't have this method, and instead
     * require use of the getExec method, then there will be giant threading issues.
     * <p>
     * Callers must call {@link ClientExec#dispose()} when finished using it.
     *
     * @return the exec object to use in a one-off.
     * @throws P4InvalidConfigException
     */
    @Deprecated
    ClientExec oneOffClientExec(@NotNull ServerRunner.ErrorVisitorFactory errorVisitorFactory)
            throws P4InvalidConfigException {
        // This needs to not affect the existing configurations in the status controller.
        return ClientExec.createFor(config, statusController, errorVisitorFactory);
    }


    private P4Exec2 getExec(@NotNull Project project)
            throws P4InvalidConfigException, P4ConnectionDisposedException {
        if (disposed) {
            throw new P4ConnectionDisposedException();
        }
        // double-check locking.  This is why clientExec must be volatile.
        synchronized (clientExecLock) {
            if (clientExec == null) {
                clientExec = ClientExec.createFor(config, statusController);
            }
            return new P4Exec2(project, clientExec);
        }
    }

    /**
     * Informs the status controller about the offline status, if it's now disconnected.
     *
     * @param project
     */
    private void checkIfOnline(@NotNull final Project project) {
        if (statusController.isWorkingOffline()) {
            // Already marked as offline.
            return;
        }
        try {
            // Make sure we run this action in the synchronizer.  Otherwise, there can
            // be conflicts with AuthenticatedServer check-out commands.
            synchronizer.runImmediateAction(new ActionRunner<Void>() {
                @Override
                public Void perform(@NotNull SynchronizedActionRunner runner)
                        throws InterruptedException {
                    try {
                        final IServerInfo info = getExec(project).getServerInfo();
                        if (info != null) {
                            // correctly online.
                            return null;
                        }
                    } catch (P4DisconnectedException e) {
                        final DisconnectedHandler errorHandler = new DisconnectedHandler(project, statusController, e);
                        AlertManager.getInstance().addCriticalError(errorHandler, e);
                    } catch (VcsException e) {
                        LOG.warn(e);
                    }
                    // offline
                    return null;
                }
            });
        } catch (InterruptedException e) {
            // online/offline mode is indeterminate.
            LOG.warn(e);
        }
    }


    private void queueUpdateActions(@NotNull Project project, @NotNull Collection<PendingUpdateState> updates) {
        UpdateGroup currentGroup = null;
        List<PendingUpdateState> currentGroupUpdates = null;
        for (PendingUpdateState update : updates) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("adding update state as action: " + update);
            }

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


    private UpdateAction pullNextAction()
            throws InterruptedException {
        UpdateAction action;
        redoLock.lock();
        try {
            action = redo.poll();
        } finally {
            redoLock.unlock();
        }
        if (action == null) {
            LOG.debug("Polling pending updates for action");
            action = pendingUpdates.take();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("pulled action " + action + "; pending size " + pendingUpdates.size() + "; redo size " +
                    redo.size());
        }
        return action;
    }


    private void pushAbortedAction(@NotNull
    final UpdateAction updateAction) {
        redoLock.lock();
        try {
            redo.add(updateAction);
        } finally {
            redoLock.unlock();
        }
    }


    private class QueueRunner
            implements Runnable {
        @Override
        public void run() {
            while (!disposed) {
                // As part of the execution, we'll include an integrity check, to ensure the
                // local cache matches up with the remaining actions.
                // This needs to be done at some point, and it's good to have it done after
                // an action, and when the plugin first initializes itself.
                cacheManager.checkLocalIntegrity();


                // Wait for something to do first
                final UpdateAction action;
                try {
                    action = pullNextAction();
                } catch (InterruptedException e) {
                    // this is fine.
                    LOG.info(e);
                    continue;
                }

                try {
                    boolean didRun = synchronizer.runBackgroundAction(new ActionRunner<Void>() {
                        @Override
                        public Void perform(@NotNull SynchronizedActionRunner syncRunner)
                                throws InterruptedException {
                            LOG.info("Running action " + action);
                            final P4Exec2 exec;
                            try {
                                exec = getExec(action.project);
                                // Perform a second connection attempt, just to be sure.

                                // TODO currently disabled to see if this helps with connection speed.
                                // exec.getServerInfo();
                            } catch (P4InvalidConfigException e) {
                                alertManager.addCriticalError(new ConfigurationProblemHandler(action.project,
                                        statusController, e), e);
                                // Do not requeue the action.
                                cacheManager.removePendingUpdateStates(action.action.getPendingUpdateStates());
                                action.action.abort(cacheManager);
                                return null;
                            } catch (P4ConnectionDisposedException e) {
                                // Do not report this error.
                                // But requeue the action.
                                return null;
                            }
                            if (!action.project.isDisposed()) {
                                action.action.perform(exec,
                                        cacheManager, ServerConnection.this,
                                        syncRunner, alertManager);
                                // only remove the state once we've successfully
                                // processed the action.
                                cacheManager.removePendingUpdateStates(action.action.getPendingUpdateStates());

                                // force a changelist refresh
                                P4ChangesViewRefresher.refreshLater(action.project);
                            }

                            return null;
                        }
                    });
                    if (!didRun) {
                        // Had to wait for the action to run, so requeue it and try again.
                        pushAbortedAction(action);
                    }
                } catch (InterruptedException e) {
                    // Requeue the action, because it is still in the
                    // cached pending update states.
                    LOG.info(e);
                    pushAbortedAction(action);
                } catch (Throwable e) {
                    // Ensure exceptions that we should never trap are handled right.
                    VcsExceptionUtil.alwaysThrown(e);

                    // Big time error, so remove the update
                    cacheManager.removePendingUpdateStates(action.action.getPendingUpdateStates());
                    alertManager.addWarning(action.project,
                            P4Bundle.message("error.update-state"),
                            action.action.toString(),
                            e, getFilesFor(action.action.getPendingUpdateStates()));

                    // do not requeue action, because we removed it
                    // from the cached update list.
                    LOG.error(e);
                }
            }
        }
    }

    @Nullable
    private static FilePath[] getFilesFor(final Collection<PendingUpdateState> pendingUpdateStates) {
        List<FilePath> ret = new ArrayList<FilePath>(pendingUpdateStates.size());
        for (PendingUpdateState state : pendingUpdateStates) {
            Object file = state.getParameters().get(UpdateParameterNames.FILE.getKeyName());
            if (file != null && file instanceof String) {
                ret.add(FilePathUtil.getFilePath(file.toString()));
            } else {
                file = state.getParameters().get(UpdateParameterNames.FILE_SOURCE.getKeyName());
                if (file != null && file instanceof String) {
                    ret.add(FilePathUtil.getFilePath(file.toString()));
                }
            }
        }
        return ret.toArray(new FilePath[ret.size()]);
    }


    private static class UpdateAction {
        final ServerUpdateAction action;
        final Project project;

        UpdateAction(@NotNull Project project, @NotNull ServerUpdateAction action) {
            this.action = action;
            this.project = project;
        }

        @Override
        public String toString() {
            return "Action " + action;
        }
    }
}
