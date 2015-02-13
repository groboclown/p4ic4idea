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
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * A Vcs specific version of a {@link java.util.concurrent.Future} class.
 * It doesn't directly implement Future, so that it can have
 * the better throwable clauses.
 *
 * @param <T>
 */
public class VcsSettableFuture<T> implements VcsFuture<T>, VcsFutureSetter<T> {
    // Only one of these futures can be the "getter" at a time.
    // That's fine, because the EDT is only one thread.
    private static final Object SYNC = new Object();
    private static VcsSettableFuture<?> EDT_GETTER;

    private final List<Runnable> listeners = new ArrayList<Runnable>();
    private final BlockingQueue<Event<?>> events = new LinkedTransferQueue<Event<?>>();
    private volatile boolean done;
    private volatile Throwable ex;
    private volatile T value;
    private volatile boolean cancelled;


    public static <T> VcsSettableFuture<T> create() {
        return new VcsSettableFuture<T>();
    }


    private VcsSettableFuture() {}


    @Override
    public void runInEdt(@NotNull Runnable runner) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runner.run();
        } else {
            synchronized (SYNC) {
                if (EDT_GETTER == null) {
                    // no "get" waiting in the EDT thread
                    ApplicationManager.getApplication().invokeLater(runner);
                } else {
                    // don't know, and don't care, about the EDT_GETTER value type.
                    EDT_GETTER.events.add(new Event<Void>(null, null, false, runner));
                }
            }
        }
    }


    public void addListener(@NotNull Runnable runnable) {
        listeners.add(runnable);
    }


    @Override
    public void set(T value) {
        done = true;
        this.value = value;
        events.add(new Event<T>(value, null, false, null));
    }


    @Override
    public void setException(@NotNull Throwable t) {
        done = true;
        ex = t;
        events.add(new Event<T>(null, t, false, null));
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        done = true;
        cancelled = true;
        events.add(new Event<T>(null, null, true, null));
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public T get() throws VcsException, CancellationException {
        return get(-1, TimeUnit.MILLISECONDS);
    }

    @Override
    public T get(long timeout, @NotNull TimeUnit unit) throws VcsException, CancellationException {
        boolean isEdtGetter = false;
        if (ApplicationManager.getApplication().isDispatchThread()) {
            synchronized (SYNC) {
                if (EDT_GETTER == null) {
                    // we are now the EDT_GETTER
                    EDT_GETTER = this;
                    isEdtGetter = true;
                } else {
                    throw new VcsException("Double future 'get' call in the EDT");
                }
            }
        }
        try {
            long now = System.currentTimeMillis();
            long endTime = now + unit.toMillis(timeout);
            if (timeout < 0) {
                endTime = Long.MAX_VALUE - now - 1;
            }
            while (!done && (timeout < 0 || endTime < now)) {
                long waitTime = endTime - now;
                try {
                    Event ev = events.poll(waitTime, TimeUnit.MILLISECONDS);
                    if (ev != null && ev.edtAction != null) {
                        ev.edtAction.run();
                    }
                } catch (InterruptedException e) {
                    cancelled = true;
                    done = true;
                    ex = e;
                }
                now = System.currentTimeMillis();
            }
            if (done) {
                for (Runnable listener : listeners) {
                    listener.run();
                }
            }
            if (ex != null) {
                if (ex instanceof InterruptedException) {
                    ex.printStackTrace();
                    CancellationException ce = new CancellationException(ex.getMessage());
                    ce.initCause(ex);
                    throw ce;
                }
                if (ex instanceof CancellationException) {
                    throw (CancellationException) ex;
                }
                if (ex instanceof VcsException) {
                    throw (VcsException) ex;
                }
                throw new VcsException(ex);
            }
            if (cancelled) {
                throw new CancellationException("Cancelled task");
            }
            return value;
        } finally {
            if (isEdtGetter) {
                synchronized (SYNC) {
                    EDT_GETTER = null;
                }
            }
        }
    }


    /**
     * Immediately gets the value for the future.  If the future isn't
     * ready yet, it cancels the future and throws a CancellationException.
     *
     * @return the value set to the future.
     * @throws com.intellij.openapi.vcs.VcsException
     * @throws java.util.concurrent.CancellationException
     */
    @Override
    public T getImmediately() throws VcsException, CancellationException {
        if (!isDone()) {
            cancel(true);
            throw new CancellationException("Not completed in time");
        }
        if (isCancelled()) {
            throw new CancellationException("Cancelled task");
        }
        return get();
    }


    protected void executeRunner(@NotNull Runnable runner) {
        events.add(new Event<T>(null, null, false, runner));
    }


    private static class Event<T> {
        public final T value;
        public final Throwable ex;
        public final boolean cancelled;
        public final Runnable edtAction;


        private Event(T value, Throwable ex, boolean cancelled, Runnable edtAction) {
            this.value = value;
            this.ex = ex;
            this.cancelled = cancelled;
            this.edtAction = edtAction;
        }
    }
}
