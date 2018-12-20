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

package net.groboclown.p4.server.impl.util;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.impl.connection.impl.LimitedConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A semaphore that includes tracing the list of who's waiting and who has a lock.
 */
public class TraceableSemaphore {
    // Note: uses the LimitedConnectionManager as the log source.
    private static final Logger LOG = Logger.getInstance(LimitedConnectionManager.class);

    private static final AtomicInteger ID_GEN = new AtomicInteger();
    private final String name;

    // Access to these two values must be synchronized on "name".
    private long timeout;
    private TimeUnit timeoutUnit;

    private final Semaphore sem;
    private final List<Requester> pending = Collections.synchronizedList(new ArrayList<>());
    private final List<Requester> active = Collections.synchronizedList(new ArrayList<>());

    public TraceableSemaphore(String name, int maximumConcurrentCount, long timeout, @NotNull TimeUnit timeoutUnit) {
        this.name = name;
        setTimeout(timeout, timeoutUnit);
        this.sem = new Semaphore(maximumConcurrentCount);
    }

    public void setTimeout(long timeout, @NotNull TimeUnit timeoutUnit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("bad timeout");
        }
        synchronized (name) {
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
        }
    }

    public static Requester createRequest() {
        int id = ID_GEN.incrementAndGet();
        String name = getRequestName();
        return new Requester(id, name);
    }

    public static class Requester {
        private final int id;
        private final String name;
        private volatile boolean active = false;
        private String str;

        private Requester(int id, String name) {
            this.id = id;
            this.name = name;
            this.str = Integer.toString(id) + ':' + name + " (pending since " + (new Date()) + ')';
        }

        private void activate() {
            this.active = true;
            this.str = Integer.toString(id) + ':' + name + " (active since " + (new Date()) + ')';
        }

        boolean isActive() {
            return active;
        }

        @Override
        public String toString() {
            return str;
        }
    }


    public void acquire(@NotNull Requester req) throws InterruptedException, CancellationException {
        if (LOG.isDebugEnabled()) {
            pending.add(req);
            LOG.debug(name + ": WAIT - wait queue = " + pending + "; active = " + active);
        }

        final long usableTimeout;
        final TimeUnit usableTimeoutUnit;
        synchronized (name) {
            usableTimeout = this.timeout;
            usableTimeoutUnit = this.timeoutUnit;
        }

        try {
            boolean captured = sem.tryAcquire(usableTimeout, usableTimeoutUnit);
            if (LOG.isDebugEnabled()) {
                pending.remove(req);
            }
            if (!captured) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(name + ": TIMEOUT - wait queue = " + pending + "; active = " + active);
                }
                throw new CancellationException("Waiting for lock timed out: exceeded " + usableTimeout + " " +
                        usableTimeoutUnit.toString().toLowerCase());
            }
            req.activate();
            active.add(req);
            if (LOG.isDebugEnabled()) {
                LOG.debug(name + ": ACQUIRE - wait queue = " + pending + "; active = " + active);
            }
        } catch (InterruptedException e) {
            if (LOG.isDebugEnabled()) {
                pending.remove(req);
                LOG.debug(name + ": INTERRUPT - wait queue = " + pending + "; active = " + active);
            }
            throw e;
        }
    }

    /**
     * Releases an active request, allowing another to run.
     *
     * @param req requested item to release
     */
    public void release(@NotNull Requester req) {
        if (LOG.isDebugEnabled()) {
            pending.remove(req);
        }
        if (req.isActive()) {
            sem.release();
            if (active.remove(req)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(name + ": RELEASE - wait queue = " + pending + "; active = " + active);
                }
            } else {
                LOG.warn("Did not store active request in the active list");
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug(name + ": RELEASE a timed out request - wait queue = " + pending + "; active = " + active);
        }
    }


    private static String getRequestName() {
        // Only perform the deep inspection if logging is enabled.
        if (!LOG.isDebugEnabled()) {
            return "<debug not enabled>";
        }

        Exception e = new Exception();
        e.fillInStackTrace();
        StackTraceElement[] stack = e.getStackTrace();

        // This method is index 0, the caller (createRequest) is 1,
        // the requestor is 2, and its root is 3.
        if (stack.length < 4) {
            return "<unknown - shallow>";
        }

        for (int i = 3; i < stack.length; i++) {
            String cz = stack[i].getClassName();

            if (cz.contains("lambda$") || cz.contains("ConnectionManager") || cz.contains("Answer") ||
                    cz.endsWith("ActionChoice") || cz.endsWith("ApplicationImpl") || cz.contains("ReduceOps")) {
                // Class names that have "lambda" in them are going to be lambda functions
                // that have no information for us.  Same goes for the other classes.
                continue;
            }

            String method = stack[i].getMethodName();
            if (method.startsWith("perform") || method.startsWith("lambda$") || method.equals("withConnection") ||
                    method.equals("onlineExec") || method.equals("run")) {
                // One of the generic methods that doesn't give any interesting information.
                continue;
            }

            int p = cz.lastIndexOf('.');
            if (p >= 0) {
               cz = cz.substring(p + 1);
            }

            return cz + ":" + method + "@" + stack[i].getLineNumber();
        }
        return "<unknown - " + stack.length + ">";
    }
}
