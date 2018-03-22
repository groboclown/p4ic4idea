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
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import net.groboclown.idea.p4ic.server.exceptions.P4ConnectionDisposedException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Centralizes all the logic around synchronizing the server connection actions.
 * Because there's lots of wait states that need to be cleared, along with the
 * terror of the IDE read state, centralizing this logic makes some sense.
 * <p></p>
 * The synchronizer is associated with the AlertManager, so that it can be
 * one-per-application.  However, each project has their own requirements.
 */
class Synchronizer {
    private static final Logger LOG = Logger.getInstance(Synchronizer.class);


    // Locks global to the application.

    private final Lock alertCriticalLock = new ReentrantLock();
    private final Condition alertCriticalActiveCondition = alertCriticalLock.newCondition();
    private boolean inCriticalError = false;

    private long lockWaitTimeMillis = -1;


    ServerSynchronizer createServerSynchronizer() {
        return new ServerSynchronizer();
    }


    void criticalErrorActive() {
        alertCriticalLock.lock();
        try {
            // no need to signal
            inCriticalError = true;
        } finally {
            alertCriticalLock.unlock();
        }
    }


    void criticalErrorsCleared() {
        alertCriticalLock.lock();
        try {
            inCriticalError = false;
            // There may be lots of threads waiting on this.
            // We can safely wake them all up.
            alertCriticalActiveCondition.signalAll();
        } finally {
            alertCriticalLock.unlock();
        }
    }


    private boolean waitForCriticalErrorsToClear() throws InterruptedException {
        boolean waited = false;
        alertCriticalLock.lockInterruptibly();
        try {
            long expires = System.currentTimeMillis() + lockWaitTimeMillis;
            while (inCriticalError) {
                LOG.info("Waiting for critical errors to clear");
                waited = true;
                if (lockWaitTimeMillis < 0) {
                    alertCriticalActiveCondition.await();
                } else {
                    long now = System.currentTimeMillis();
                    if (now > expires) {
                        throw new InterruptedException("timed out waiting for critical alerts to clear");
                    }
                    // Ignore return code.
                    // If this times out, then the loop will
                    // cause an InterruptedException.
                    // If it doesn't time out, the loop will
                    // cause a return value.
                    alertCriticalActiveCondition.await(expires - now, TimeUnit.MILLISECONDS);
                }
            }
        } finally {
            alertCriticalLock.unlock();
        }
        return waited;
    }


    class ServerSynchronizer {
        private final Lock onlineLock = new ReentrantLock();
        private final Condition offlineCondition = onlineLock.newCondition();
        private boolean isOnline = false;
        private InnerSynchronizedActionRunner syncRunner = new InnerSynchronizedActionRunner();


        ConnectionSynchronizer createConnectionSynchronizer() {
            return new ConnectionSynchronizer();
        }



        void wentOnline() {
            onlineLock.lock();
            try {
                isOnline = true;
                // All threads can start accessing the connection.
                offlineCondition.signalAll();
            } finally {
                onlineLock.unlock();
            }
        }


        void wentOffline() {
            onlineLock.lock();
            try {
                // Do not signal in this circumstance.
                isOnline = false;
            } finally {
                onlineLock.unlock();
            }
        }


        private boolean waitForOnline() throws InterruptedException {
            boolean waited = false;
            onlineLock.lockInterruptibly();
            try {
                long expires = System.currentTimeMillis() + lockWaitTimeMillis;
                while (!isOnline) {
                    LOG.info("Waiting for online status");
                    waited = true;
                    if (lockWaitTimeMillis < 0) {
                        offlineCondition.await();
                    } else {
                        long now = System.currentTimeMillis();
                        if (now > expires) {
                            throw new InterruptedException("timed out waiting for connection to go online");
                        }
                        offlineCondition.await(expires - now, TimeUnit.MILLISECONDS);
                    }
                }
            } finally {
                onlineLock.unlock();
            }
            return waited;
        }

        class ConnectionSynchronizer {
            private final Lock connectionLock = new ReentrantLock();
            private volatile boolean inLock = false;
            private long connectionWaitTimeMillis = 1000 * 15;

            <T> T runImmediateAction(@NotNull final ActionRunner<T> runner) throws InterruptedException {
                // Acquire the IDE read lock.

                // TODO the actual read lock should be delayed until absolutely necessary.
                // This can cause the EDT to wait while a Perforce server connection request
                // runs, when the lock isn't necessary.

                // Wait a limited time for the connection.
                boolean acquired =
                        connectionLock.tryLock(connectionWaitTimeMillis, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    throw new InterruptedException("lock acquire timeout");
                }
                // Should not be re-entrant.
                if (inLock) {
                    throw new InterruptedException("Already running in lock");
                }
                inLock = true;
                try {
                    // Run the action.
                    return runner.perform(syncRunner);
                } finally {
                    inLock = false;
                    connectionLock.unlock();
                }
            }


            /**
             * @return true if the action ran, or false if a wait happened.
             */
            boolean runBackgroundAction(@NotNull final ActionRunner<?> runner) throws InterruptedException {
                // First, wait for going online
                if (waitForOnline()) {
                    return false;
                }

                // Then wait for the alerts to clear.
                if (waitForCriticalErrorsToClear()) {
                    return false;
                }

                // We don't care if we wait forever, don't directly reuse the runImmediately logic.

                // TODO the actual read lock should be delayed until absolutely necessary.
                // This can cause the EDT to wait while a Perforce server connection request
                // runs, when the lock isn't necessary.

                // Wait forever for the connection.
                connectionLock.lock();
                try {
                    // Run the action.
                    runner.perform(syncRunner);
                } finally {
                    connectionLock.unlock();
                }
                return true;
            }
        }
    }


    private class InnerSynchronizedActionRunner implements SynchronizedActionRunner {
        @Override
        public <T, E extends Throwable> T read(final ThrowableComputable<T, E> runner) throws E {
            return ApplicationManager.getApplication().runReadAction(runner);
        }

        @Override
        public <T> T read(final Computable<T> runner) {
            return ApplicationManager.getApplication().runReadAction(runner);
        }

        @Override
        public void read(final Runnable runner) {
            ApplicationManager.getApplication().runReadAction(runner);
        }

        @Override
        public <T, E extends Throwable> T write(final ThrowableComputable<T, E> runner) throws E {
            return ApplicationManager.getApplication().runWriteAction(runner);
        }

        @Override
        public <T> T write(final Computable<T> runner) {
            return ApplicationManager.getApplication().runWriteAction(runner);
        }

        @Override
        public void write(final Runnable runner) {
            ApplicationManager.getApplication().runWriteAction(runner);
        }
    }



    interface ActionRunner<T> {
        T perform(@NotNull SynchronizedActionRunner runner) throws InterruptedException;
    }
}
