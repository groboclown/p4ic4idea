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

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.RawServerExecutor;
import net.groboclown.idea.p4ic.server.ServerStatus;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The multi-threaded connections to the Perforce server.
 *
 * This is a future replacement for the {@link RawServerExecutor}.
 *
 * TODO better understand the {@link net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState}
 * ownership and this file.
 */
public class ServerConnection {
    private static final ThreadGroup CONNECTION_THREAD_GROUP = new ThreadGroup("Server Connection");
    private static final ThreadLocal<Boolean> THREAD_EXECUTION_ACTIVE = new ThreadLocal<Boolean>();
    private final Lock connectionLock = new ReentrantLock();
    private final Queue<PendingUpdateEntry> pendingUpdates = new LinkedBlockingDeque<PendingUpdateEntry>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Project project;
    private final ServerStatus serverStatus;
    private final AlertManager alertManager;
    private P4Exec2 exec;


    public static void assertInServerConnection() {
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        if (currentGroup != CONNECTION_THREAD_GROUP && THREAD_EXECUTION_ACTIVE.get() != Boolean.TRUE) {
            throw new IllegalStateException("Activity can only be run from within the ServerConnection action thread");
        }
    }


    public ServerConnection(@NotNull final Project project, @NotNull final AlertManager alertManager,
            @NotNull ServerConfig serverConfig) throws P4InvalidConfigException {
        this.project = project;
        this.alertManager = alertManager;
        this.serverStatus = ServerStoreService.getInstance().getServerStatus(project, serverConfig);
    }


    public void queueAction(@NotNull P4Server server, @NotNull ServerUpdateAction action) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    public void runImmediately(@NotNull P4Server server, @NotNull ServerUpdateAction action) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    public void query(@NotNull P4Server server, @NotNull ServerQuery query) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }


    /**
     * Retry running a command that failed.  This should usually be put back at the head
     * of the action queue.  It is sometimes necessary if the command fails due to a
     * login or config issue.
     *
     * @param server
     * @param action
     */
    public void requeueAction(@NotNull P4Server server, @NotNull ServerUpdateAction action) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }



    private void startImmediateAction() {
        connectionLock.lock();
        THREAD_EXECUTION_ACTIVE.set(Boolean.TRUE);
    }

    private void stopImmediateAction() {
        THREAD_EXECUTION_ACTIVE.remove();
        connectionLock.unlock();
    }



    private static class PendingUpdateEntry {
        final ServerUpdateAction action;
        final P4Server server;

        private PendingUpdateEntry(final ServerUpdateAction action, final P4Server server) {
            this.action = action;
            this.server = server;
        }
    }

}
