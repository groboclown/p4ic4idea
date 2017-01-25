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

package net.groboclown.idea.p4ic.background;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public class BackgroundAwtActionRunner {
    private static final Logger LOG = Logger.getInstance(BackgroundAwtActionRunner.class);


    public interface BackgroundAwtAction<T> {
        T runBackgroundProcess();

        void runAwtProcess(T value);
    }

    private static final Set<String> activeProcesses = new HashSet<String>();

    // CalledInAwt
    public static <T> void runBackgroundAwtAction(@NotNull final AsyncProcessIcon icon,
                                                  @NotNull final BackgroundAwtAction<T> action) {
        runBackgroundAwtActionOptionalIcon(icon, action);
    }

    // CalledInAwt
    public static <T> void runBackgrounAwtAction(@NotNull final BackgroundAwtAction<T> action) {
        runBackgroundAwtActionOptionalIcon(null, action);
    }

    // CalledInAwt
    private static <T> void runBackgroundAwtActionOptionalIcon(@Nullable final AsyncProcessIcon icon,
                                            @NotNull final BackgroundAwtAction<T> action) {
        final String processName = icon == null ? action.getClass().getSimpleName() : icon.getName();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested background action " + processName);
        }
        synchronized (activeProcesses) {
            if (activeProcesses.contains(processName)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" - process is already running in background (active: " + activeProcesses + ")");
                }
                return;
            }
            activeProcesses.add(processName);
        }
        if (icon != null) {
            icon.resume();
            icon.setVisible(true);
        }
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Running " + processName + " in background ");
                }
                T tmpValue;
                Exception tmpFailure;
                try {
                    tmpValue = action.runBackgroundProcess();
                    tmpFailure = null;
                } catch (Exception e) {
                    LOG.error("Background processing for " + processName + " failed", e);
                    tmpValue = null;
                    tmpFailure = e;
                } finally {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Background processing for " + processName
                                + " completed.  Queueing AWT processing.");
                    }
                }
                final T value = tmpValue;
                final Exception failure = tmpFailure;

                // NOTE: ApplicationManager.getApplication().invokeLater
                // will not work, because it will wait until the UI dialog
                // goes away, which means we can't see the results until
                // the UI element goes away, which is just backwards.

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Running " + processName + " in AWT");
                        }
                        try {
                            if (failure == null) {
                                action.runAwtProcess(value);
                            }
                        } finally {
                            if (icon != null) {
                                icon.suspend();
                                icon.setVisible(false);
                            }
                            synchronized (activeProcesses) {
                                activeProcesses.remove(processName);
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Remaining background processes active: " + activeProcesses);
                                }
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("AWT processing for " + processName + " completed");
                            }
                        }
                    }
                });
            }
        });
    }

}
