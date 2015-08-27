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
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the active alerts that we ask the user to respond to.
 * These always happen outside of the execution actions.
 *
 * There should only one of these per application.
 */
public class AlertManager implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(AlertManager.class);

    private static final long POLL_TIMEOUT = 10;
    private static final TimeUnit POLL_TIMEOUT_UNIT = TimeUnit.SECONDS;

    // Every error and warning added causes a new event added.
    private final List<WarningMsg> pendingWarnings = new ArrayList<WarningMsg>();
    private final Deque<ErrorMsg> criticalErrorHandlers = new ArrayDeque<ErrorMsg>();
    private final Lock eventLock = new ReentrantLock();
    private final Condition eventPending = eventLock.newCondition();
    private final Condition noPendingCriticalErrors = eventLock.newCondition();
    private Thread handlerThread;
    private volatile int criticalErrorCount = 0;
    private volatile boolean disposed;


    public static AlertManager getInstance() {
        // FIXME
        throw new IllegalStateException("needs to be registered in plugin.xml");

        //return ApplicationManager.getApplication().getComponent(AlertManager.class);
    }


    public void addWarning(@NotNull String message, @Nullable VcsException ex) {
        eventLock.lock();
        try {
            pendingWarnings.add(new WarningMsg(message, ex));
            eventPending.signal();
        } finally {
            eventLock.unlock();
        }
    }

    public void addNotice(@NotNull final String message, @Nullable final VcsException ex) {
        // TODO this should be turned into a UI-friendly element
        LOG.warn(message, ex);
    }


    public void addCriticalError(@NotNull P4Server server, @NotNull final CriticalErrorHandler error) {
        eventLock.lock();
        try {
            // don't use "offer"; we expect this to always work.
            criticalErrorCount++;
            criticalErrorHandlers.add(new ErrorMsg(server, error));
            eventPending.signal();
        } finally {
            eventLock.unlock();
        }
    }


    /**
     * Wait until there are no more critical errors pending, or until
     * a timeout or a thread interruption.
     */
    public void waitForNoCriticalErrors(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        long now = System.currentTimeMillis();
        long timeoutMs = unit.toMillis(timeout);
        eventLock.lock();
        try {
            while (criticalErrorCount > 0) {
                long sleepTime = timeoutMs - now;
                if (sleepTime < 0) {
                    // TODO localize the message?
                    throw new InterruptedException("timed out");
                }
                noPendingCriticalErrors.await(timeoutMs, TimeUnit.MILLISECONDS);
            }
        } finally {
            eventLock.unlock();
        }
    }


    public boolean hasCriticalErrorsPending() {
        eventLock.lock();
        try {
            return criticalErrorCount > 0;
        } finally {
            eventLock.unlock();
        }
    }


    @Override
    public void initComponent() {
        handlerThread = new Thread(new MessageHandler(), "Perforce Alert Manager");
        handlerThread.setDaemon(true);
        handlerThread.setPriority(Thread.NORM_PRIORITY + 1);
    }

    @Override
    public void disposeComponent() {
        disposed = true;
        if (handlerThread != null) {
            eventLock.lock();
            try {
                eventPending.signal();
            } finally {
                eventLock.unlock();
            }
            handlerThread.interrupt();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Perforce Alert Manager";
    }


    private void handleWarnings(@NotNull final List<WarningMsg> warnings) {
        // FIXME handle all the warnings in a single UI message.
        for (WarningMsg warning : warnings) {
            LOG.error(warning.message, warning.warning);
        }
    }

    // Run by the ErrorMsg class, from within the EDT
    private void handleError(@NotNull final ErrorMsg errorMsg) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        // Critical errors are handled one at a time.
        try {
            errorMsg.error.handleError(errorMsg.when, errorMsg.server);
        } finally {
            eventLock.lock();
            try {
                criticalErrorCount--;
                noPendingCriticalErrors.signal();
            } finally {
                eventLock.unlock();
            }
        }
    }


    class MessageHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    ErrorMsg errorMsg = null;
                    List<WarningMsg> warningMsgs = null;
                    eventLock.lock();
                    try {
                        // Loop until we get an action to perform.
                        // Actions are performed outside the event lock.
                        while (errorMsg == null && warningMsgs == null) {
                            if (disposed) {
                                return;
                            }

                            // Always handle pending critical errors first.
                            if (!criticalErrorHandlers.isEmpty()) {
                                errorMsg = criticalErrorHandlers.poll();
                            } else if (!pendingWarnings.isEmpty()) {
                                warningMsgs = new ArrayList<WarningMsg>(pendingWarnings);
                                pendingWarnings.clear();
                            }
                            if (errorMsg == null && warningMsgs == null) {
                                // wait
                                eventPending.await(POLL_TIMEOUT, POLL_TIMEOUT_UNIT);
                            }
                        }
                    } finally {
                        eventLock.unlock();
                    }
                    if (errorMsg != null) {
                        ApplicationManager.getApplication().invokeLater(errorMsg);
                    } else if (warningMsgs != null) {
                        // should always be true, but just to be sure, check that it
                        // isn't null.
                        handleWarnings(warningMsgs);
                    }
                } catch (InterruptedException e) {
                    // stops the polling
                }
                if (disposed) {
                    return;
                }
            }
        }
    }


    class ErrorMsg implements Runnable {
        final CriticalErrorHandler error;
        final P4Server server;
        final Date when = new Date();

        ErrorMsg(@NotNull P4Server server, @NotNull final CriticalErrorHandler error) {
            this.server = server;
            this.error = error;
        }

        @Override
        public void run() {
            handleError(this);
        }
    }


    static class WarningMsg {
        final String message;
        final VcsException warning;
        final Date when = new Date();

        WarningMsg(@NotNull final String message, @Nullable final VcsException warning) {
            this.message = message;
            this.warning = warning;
        }
    }
}
