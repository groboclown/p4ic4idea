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
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
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

    private static ThrowableHandled throwableHandled = new ThrowableHandled();

    // Every error and warning added causes a new event added.
    private final List<WarningMsg> pendingWarnings = new ArrayList<WarningMsg>();
    private final Deque<ErrorMsg> criticalErrorHandlers = new ArrayDeque<ErrorMsg>();
    private final Lock eventLock = new ReentrantLock();
    private final Condition eventPending = eventLock.newCondition();
    private Thread handlerThread;
    private final Synchronizer synchronizer = new Synchronizer();
    private volatile int criticalErrorCount = 0;
    private volatile boolean disposed;



    public static AlertManager getInstance() {
        return ApplicationManager.getApplication().getComponent(AlertManager.class);
    }


    public void addWarning(@NotNull String message, @Nullable VcsException ex) {
        if (ex != null && throwableHandled.isHandled(ex)) {
            LOG.info("Skipped duplicate handling of " + ex);
            return;
        }
        eventLock.lock();
        try {
            // FIXME make debug
            LOG.info("adding warning " + message, ex);
            pendingWarnings.add(new WarningMsg(message, ex));
            eventPending.signal();
        } finally {
            eventLock.unlock();
        }
    }


    public <T> boolean addWarnings(@NotNull final String message, @NotNull MessageResult<T> result,
            boolean ignoreFileNotFound) {
        return addWarnings(message, result.getMessages(), ignoreFileNotFound);
    }


    /**
     *
     * @param message localized message
     * @param msgs status messages
     * @param ignoreFileNotFound ignore "file not found" messages if true
     * @return true if the messages contained an error, false if not.
     */
    public boolean addWarnings(@Nls @NotNull final String message, @NotNull final List<P4StatusMessage> msgs,
            final boolean ignoreFileNotFound) {
        List<String> statusMessages = new ArrayList<String>(msgs.size());
        for (P4StatusMessage msg : msgs) {
            if (msg != null && msg.isError() && (!ignoreFileNotFound ||
                    !msg.isFileNotFoundError())) {
                statusMessages.add(msg.toString());
            }
        }
        if (! statusMessages.isEmpty()) {
            // TODO make as a local message
            addWarning(message + ": " + statusMessages, null);
            return true;
        }
        return false;
    }

    public void addNotice(@NotNull @Nls final String message, @Nullable final VcsException ex) {
        if (ex != null && throwableHandled.isHandled(ex)) {
            LOG.debug("Skipped duplicate handling of " + ex);
            return;
        }

        // FIXME this should be turned into a UI-friendly element
        LOG.warn(message, ex);
    }

    public void addNotices(@NotNull final String message, @NotNull final List<P4StatusMessage> msgs,
            final boolean ignoreFileNotFound) {
        for (P4StatusMessage msg : msgs) {
            if (msg != null && msg.isError() && (! ignoreFileNotFound ||
                    ! msg.isFileNotFoundError())) {
                // FIXME this should be turned into a UI-friendly element
                LOG.warn(message, P4StatusMessage.messageAsError(msg));
            }
        }
    }


    public void addCriticalError(@NotNull final CriticalErrorHandler error,
            @Nullable Throwable src) {
        if (src != null && throwableHandled.isHandled(src)) {
            LOG.info("Skipped duplicate handling of " + src);
            return;
        }
        LOG.info("Critical error", src);
        eventLock.lock();
        try {
            criticalErrorCount++;
            synchronizer.criticalErrorActive();
            criticalErrorHandlers.add(new ErrorMsg(error, src));
            eventPending.signal();
        } finally {
            eventLock.unlock();
        }
    }


    @Override
    public void initComponent() {
        handlerThread = new Thread(new MessageHandler(), getComponentName());
        handlerThread.setDaemon(true);
        handlerThread.setPriority(Thread.NORM_PRIORITY + 1);
        handlerThread.start();
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

    /** Called by {@link ServerConnectionManager} */
    @NotNull
    Synchronizer.ServerSynchronizer createServerSynchronizer() {
        return synchronizer.createServerSynchronizer();
    }


    private void handleWarnings(@NotNull final List<WarningMsg> warnings) {
        // FIXME handle all the warnings in a single UI message.
        for (WarningMsg warning : warnings) {
            LOG.warn(warning.message, warning.warning);
        }
    }

    // Run by the ErrorMsg class, from within the EDT
    private void handleError(@NotNull final ErrorMsg errorMsg) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        // Critical errors are handled one at a time.
        try {
            errorMsg.runHandlerInEDT();
        } finally {
            eventLock.lock();
            try {
                criticalErrorCount--;
                if (criticalErrorCount <= 0) {
                    criticalErrorCount = 0;
                    synchronizer.criticalErrorsCleared();
                }
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
        //final Client client;
        final Throwable src;
        final Date when = new Date();

        ErrorMsg(@NotNull final CriticalErrorHandler error, final Throwable src) {
            //this.client = client;
            this.error = error;
            this.src = src;
        }

        @Override
        public void run() {
            // This bounces between classes (in the manager, in here, in the manager, in here),
            // but it's for general containment of responsibilities.
            LOG.info("Handling critical error " + src);
            handleError(this);
            LOG.info("Completed handling critical error " + src);
        }


        void runHandlerInEDT() {
            ApplicationManager.getApplication().assertIsDispatchThread();
            try {
                LOG.info("Handling error " + error);
                error.handleError(when);
            } catch (Exception e) {
                LOG.warn("Error handler " + error.getClass().getSimpleName() + " caused error", e);
            }
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


    /**
     * Keeps track of all the throwables that have been handled by this class.
     * It allows duplicates (those that were pushed up the stack but handled
     * here) to be correctly dealt with.
     */
    static class ThrowableHandled {
        // Use a weak reference; that way, when the throwable is no longer in
        // scope (the stacks have finished dealing with it), it will be removed
        // from the list.
        private final List<WeakReference<Throwable>> refs = new ArrayList<WeakReference<Throwable>>();
        private final Lock lock = new ReentrantLock();

        /**
         * Checks if the throwable has been registered; if not, it is registered.
         * @param t throwable
         * @return true if it is already registered, false if it is added.
         */
        boolean isHandled(@NotNull Throwable t) {
            Set<Throwable> causes = new HashSet<Throwable>();
            Throwable current = t;
            Throwable prev = null;
            while (current != null && current != prev) {
                causes.add(current);
                prev = current;
                current = prev.getCause();
            }
            lock.lock();
            try {
                Iterator<WeakReference<Throwable>> iter = refs.iterator();
                while (iter.hasNext()) {
                    final WeakReference<Throwable> ref = iter.next();
                    final Throwable that = ref.get();
                    if (that == null) {
                        iter.remove();
                    } else if (causes.contains(that)) {
                        return true;
                    }
                }
                refs.add(new WeakReference<Throwable>(t));
                return false;
            } finally {
                lock.unlock();
            }
        }
    }


}
