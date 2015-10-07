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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ThrowableComputable;
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
            private long connectionWaitTimeMillis = 1000 * 15;

            <T> T runImmediateAction(@NotNull final ActionRunner<T> runner) throws InterruptedException {
                // Acquire the IDE read lock.
                return ApplicationManager.getApplication().runReadAction(
                        new ThrowableComputable<T, InterruptedException>() {
                            @Override
                            public T compute() throws InterruptedException {
                                // Wait for the connection.
                                boolean acquired =
                                        connectionLock.tryLock(connectionWaitTimeMillis, TimeUnit.MILLISECONDS);
                                if (!acquired) {
                                    throw new InterruptedException("lock acquire timeout");
                                }
                                try {
                                    // Run the action.
                                    return runner.perform();
                                } finally {
                                    connectionLock.unlock();
                                }
                            }
                        });
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

                // We don't care if we wait forever, do don't directly reuse the runImmediately logic.
                ApplicationManager.getApplication().runReadAction(
                        new ThrowableComputable<Void, InterruptedException>() {
                            @Override
                            public Void compute() throws InterruptedException {
                                // Wait forever for the connection.
                                connectionLock.lock();
                                try {
                                    // Run the action.
                                    runner.perform();
                                    return null;
                                } finally {
                                    connectionLock.unlock();
                                }
                            }
                        });
                return true;
            }
        }
    }



    interface ActionRunner<T> {
        T perform() throws InterruptedException;
    }
}
