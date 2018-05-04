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

package net.groboclown.p4plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Consumer;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs a load operation in the background, and, when completed, sends a message
 * to all listeners.  The listeners will be called in the EDT.  This assumes just a
 * single item is produced, or a collection of items produced all at once.
 * <p>
 * The instance can only be used for one execution run.
 *
 * @param <E> type of item produced that the listeners wait on.
 */
public class EdtConsumerProcessor<E> {
    private static final Logger LOG = Logger.getInstance(EdtConsumerProcessor.class);

    private final List<Consumer<E>> listeners = new ArrayList<>();
    private final List<Runnable> runners = new ArrayList<>();
    private final Object sync = new Object();
    private boolean isLoaded = false;
    private E loadedValue;

    public void addListener(@NotNull final Consumer<E> listener) {
        synchronized (sync) {
            if (!isLoaded) {
                listeners.add(listener);
            } else {
                ApplicationManager.getApplication().invokeLater(() -> listener.consume(loadedValue));
            }
        }
    }

    public void addIcon(@NotNull final AsyncProcessIcon icon) {
        synchronized (sync) {
            if (!isLoaded) {
                icon.setVisible(true);
                runners.add(() -> icon.setVisible(false));
            } else {
                icon.setVisible(false);
            }
        }
    }

    /**
     * Disables the button when the action runs, and enables it when it isn't running.
     * If the component state depends on more than just running/not running, then
     * a runner should be added.
     */
    public void addDisabledWhileRunningComponent(@NotNull final JComponent component) {
        synchronized (sync) {
            if (!isLoaded) {
                if (ApplicationManager.getApplication().isDispatchThread()) {
                    component.setEnabled(false);
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> component.setEnabled(false));
                }
                runners.add(() -> component.setEnabled(true));
            } else {
                if (ApplicationManager.getApplication().isDispatchThread()) {
                    component.setEnabled(true);
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> component.setEnabled(true));
                }
            }
        }
    }

    public void addListener(@NotNull final Runnable runnable) {
        synchronized (sync) {
            if (!isLoaded) {
                runners.add(runnable);
            } else {
                ApplicationManager.getApplication().invokeLater(runnable);
            }
        }
    }


    /**
     *
     * @return null if the items haven't been loaded, or the list of loaded items.
     */
    @Nullable
    public E getItem() {
        synchronized (sync) {
            return loadedValue;
        }
    }

    /**
     * Performs the same calls to the runners and listeners, but
     * runs in the promise thread.  The runners and listeners will still execute
     * in the AWT.
     *
     * @param promise the promise to run
     * @param listenerAsync If true, the listeners will run asynchronously in the
     *                      AWT.  If false, then the promise thread will wait for the
     *                      listeners to finish.
     * @return the result of the promise running with the listeners called out.
     */
    public Promise<E> load(final Promise<E> promise, final boolean listenerAsync) {
        return promise.then((values) -> {
            try {
                synchronized (sync) {
                    loadedValue = values;
                    isLoaded = true;
                }
                // Run each listener by itself in the EDT.
                // Note the unsynchronized access to the items and to the listeners.
                // That's okay, because we've set the isLoaded state to false, which
                // was done in a synch block.
                for (final Consumer<E> listener : listeners) {
                    if (listenerAsync) {
                        ApplicationManager.getApplication().invokeLater(() -> listener.consume(loadedValue));
                    } else {
                        ApplicationManager.getApplication().invokeAndWait(() -> listener.consume(loadedValue));
                    }
                }
                listeners.clear();
                for (Runnable runner : runners) {
                    if (listenerAsync) {
                        ApplicationManager.getApplication().invokeLater(runner);
                    } else {
                        ApplicationManager.getApplication().invokeAndWait(runner);
                    }
                }
                runners.clear();
            } catch (Exception e) {
                LOG.error(e);
            }
            return values;
        });
    }

    /**
     *
     * @param callable
     * @param listenerAsync If true, the listeners will run asynchronously in the
     *                      AWT.  If false, then the promise thread will wait for the
     *                      listeners to finish.
     */
    public void load(final Callable<E> callable, boolean listenerAsync) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                E values = callable.call();
                synchronized (sync) {
                    loadedValue = values;
                    isLoaded = true;
                }
                // Run each listener by itself in the EDT.
                // Note the unsynchronized access to the items and to the listeners.
                // That's okay, because we've set the isLoaded state to false, which
                // was done in a synch block.
                for (final Consumer<E> listener : listeners) {
                    if (listenerAsync) {
                        ApplicationManager.getApplication().invokeLater(() -> listener.consume(loadedValue));
                    } else {
                        ApplicationManager.getApplication().invokeAndWait(() -> listener.consume(loadedValue));
                    }
                }
                listeners.clear();
                for (Runnable runner : runners) {
                    if (listenerAsync) {
                        ApplicationManager.getApplication().invokeLater(runner);
                    } else {
                        ApplicationManager.getApplication().invokeAndWait(runner);
                    }
                }
                runners.clear();
            } catch (Exception e) {
                LOG.error(e);
            }
        });
    }
}
