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
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs a load operation in the background, and, when completed, sends a message
 * to all listeners.  The listeners will be called in the EDT.
 *
 * @param <E>
 */
public class AsyncLoader<E> {
    private static final Logger LOG = Logger.getInstance(AsyncLoader.class);

    private final List<AsyncLoadedListener<E>> listeners = new ArrayList<AsyncLoadedListener<E>>();
    private final List<Runnable> runners = new ArrayList<Runnable>();
    private final Object sync = new Object();
    private boolean isLoaded = false;
    private Collection<E> items;

    public interface AsyncLoadedListener<E> {
        void onLoaded(@NotNull Collection<E> items);
    }

    public void addListener(@NotNull final AsyncLoadedListener<E> listener) {
        synchronized (sync) {
            if (!isLoaded) {
                listeners.add(listener);
            } else {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        listener.onLoaded(items);
                    }
                });
            }
        }
    }

    public void addListener(@NotNull final AsyncProcessIcon icon) {
        synchronized (sync) {
            if (!isLoaded) {
                icon.setVisible(true);
                runners.add(new Runnable() {
                    @Override
                    public void run() {
                        icon.setVisible(false);
                    }
                });
            } else {
                icon.setVisible(false);
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
    public Collection<E> getItems() {
        synchronized (sync) {
            return items;
        }
    }

    public void load(final Callable<Collection<E>> callable) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Collection<E> values = callable.call();
                    synchronized (sync) {
                        items = Collections.unmodifiableCollection(values);
                        isLoaded = true;
                    }
                    // Run each listener by itself in the EDT.
                    // Note the unsynchronized access to the items and to the listeners.
                    // That's okay, because we've set the isLoaded state to false, which
                    // was done in a synch block.
                    for (final AsyncLoadedListener<E> listener : listeners) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                listener.onLoaded(items);
                            }
                        });
                    }
                    listeners.clear();
                    for (Runnable runner : runners) {
                        ApplicationManager.getApplication().invokeLater(runner);
                    }
                    runners.clear();
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
        });
    }
}
