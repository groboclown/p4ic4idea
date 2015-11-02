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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.VcsExceptionUtil;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import net.groboclown.idea.p4ic.v2.ui.warning.WarningMessage;
import net.groboclown.idea.p4ic.v2.ui.warning.WarningUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
    private final List<WarningMessage> pendingWarnings = new ArrayList<WarningMessage>();
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

    public void addWarning(@NotNull Project project, @Nls @NotNull String title, @Nls @NotNull String details,
            @Nullable Throwable ex, @Nullable FilePath[] affectedFiles) {
        List<VirtualFile> files = new ArrayList<VirtualFile>();
        if (affectedFiles != null) {
            for (FilePath file : affectedFiles) {
                if (file != null && file.getVirtualFile() != null) {
                    files.add(file.getVirtualFile());
                }
            }
        }
        addWarning(project, title, details, ex, files.toArray(new VirtualFile[files.size()]));
    }

    public void addWarning(@NotNull Project project, @Nls @NotNull String title, @Nls @NotNull String details,
            @Nullable Throwable ex, @NotNull FilePath affectedFiles) {
        addWarning(project, title, details, ex, new FilePath[] { affectedFiles });
    }

    public void addWarning(@NotNull Project project, @Nls @NotNull String title, @Nls @NotNull String details,
            @Nullable Throwable ex, @NotNull Collection<FilePath> affectedFiles) {
        addWarning(project, title, details, ex, affectedFiles.toArray(new FilePath[affectedFiles.size()]));
    }

    public void addWarning(@NotNull Project project, @Nls @NotNull String title, @Nls @NotNull String details,
            @Nullable Throwable ex, @NotNull VirtualFile[] affectedFiles) {
        if (ex != null && throwableHandled.isHandled(ex)) {
            LOG.info("Skipped duplicate handling of " + ex);
            return;
        }
        eventLock.lock();
        try {
            LOG.warn(details, ex);
            pendingWarnings.add(new WarningMessage(project, title, details, ex, affectedFiles));
            eventPending.signal();
        } finally {
            eventLock.unlock();
        }
    }

    public void addWarning(@NotNull Project project, @Nls @NotNull String title, @Nls @NotNull String details,
            @Nullable Exception ex, @NotNull List<IFileSpec> specs) {
        FilePath[] files = new FilePath[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            files[i] = FilePathUtil.getFilePath(specs.get(i).getClientPathString());
        }
        addWarning(project, title, details, ex, files);
    }


    public <T> boolean addWarnings(@NotNull Project project, @Nls @NotNull final String message,
            @NotNull MessageResult<T> result, boolean ignoreFileNotFound) {
        return addWarnings(project, message, result.getMessages(), ignoreFileNotFound);
    }


    /**
     *
     * @param title localized message
     * @param msgs status messages
     * @param ignoreFileNotFound ignore "file not found" messages if true
     * @return true if the messages contained an error, false if not.
     */
    public boolean addWarnings(@NotNull Project project, @Nls @NotNull final String title,
            @NotNull final List<P4StatusMessage> msgs,
            final boolean ignoreFileNotFound) {
        boolean wasWarning = false;

        for (P4StatusMessage msg : msgs) {
            if (msg != null && msg.isError() && (!ignoreFileNotFound ||
                    !msg.isFileNotFoundError())) {
                addWarning(project, title, msg.toString(), null,
                        msg.getFilePath() == null
                                ? FilePathUtil.getFilePath(project.getBaseDir())
                                : msg.getFilePath());
                wasWarning = true;
            }
        }
        return wasWarning;
    }

    public void addNotice(@NotNull Project project, @NotNull @Nls final String message, @Nullable final Exception ex,
            @Nullable FilePath... files) {
        if (ex != null && throwableHandled.isHandled(ex)) {
            LOG.debug("Skipped duplicate handling of " + ex);
            return;
        }

        // For now, it'll be in the warnings.
        if (files == null) {
            files = new FilePath[0];
        }
        addWarning(project, message, ex == null ? "" : (ex.getMessage() == null ? "" : ex.getMessage()), ex,
                files);
        LOG.warn(message, ex);
    }

    public void addNotices(@NotNull Project project, @Nls @NotNull final String message,
            @NotNull final List<P4StatusMessage> msgs,
            final boolean ignoreFileNotFound) {
        addWarnings(project, message, msgs, ignoreFileNotFound);

        // TODO make an actual notice, not just reuse the warning code
//
//        for (P4StatusMessage msg : msgs) {
//            if (msg != null && msg.isError() && (! ignoreFileNotFound ||
//                    ! msg.isFileNotFoundError())) {
//                addNotice(project, message, )
//            }
//        }
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


    private void handleWarnings(@NotNull final List<WarningMessage> warnings) {
        // See AbstractVcsHelperImpl and AbstractVcsHelper
        // tab name VcsBundle.message("message.title.annotate")
        LOG.info("start handleWarnings");
        WarningUI.showWarnings(warnings);
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
                    List<WarningMessage> warningMessages = null;
                    eventLock.lock();
                    try {
                        // Loop until we get an action to perform.
                        // Actions are performed outside the event lock.
                        while (errorMsg == null && warningMessages == null) {
                            if (disposed) {
                                return;
                            }

                            // Always handle pending critical errors first.
                            if (!criticalErrorHandlers.isEmpty()) {
                                errorMsg = criticalErrorHandlers.poll();
                            } else if (!pendingWarnings.isEmpty()) {
                                warningMessages = new ArrayList<WarningMessage>(pendingWarnings);
                                pendingWarnings.clear();
                            }
                            if (errorMsg == null && warningMessages == null) {
                                // wait
                                eventPending.await(POLL_TIMEOUT, POLL_TIMEOUT_UNIT);
                            }
                        }
                    } finally {
                        eventLock.unlock();
                    }
                    if (errorMsg != null) {
                        // Note that ApplicationManager.getApplication().invokeLater
                        // will wait for the UI dialogs to be closed before running.
                        // We do not want that behavior.
                        SwingUtilities.invokeLater(errorMsg);
                    } else if (warningMessages != null) {
                        // should always be true, but just to be sure, check that it
                        // isn't null.
                        handleWarnings(warningMessages);
                    }
                } catch (InterruptedException e) {
                    // stops the polling
                } catch (Throwable t) {
                    // make sure we always throw certain exceptions
                    VcsExceptionUtil.alwaysThrown(t);
                    LOG.error(t);
                }
                if (disposed) {
                    return;
                }
            }
        }
    }


    class ErrorMsg implements Runnable {
        final CriticalErrorHandler error;
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
