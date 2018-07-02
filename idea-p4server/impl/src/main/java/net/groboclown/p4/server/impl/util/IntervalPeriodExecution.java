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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Waits for a minimum quiet period before running the task.
 */
public class IntervalPeriodExecution {
    private static final Logger LOG = Logger.getInstance(IntervalPeriodExecution.class);

    private final Runnable runner;
    private final long minDelay;
    private final Object lock = new Object();
    private volatile boolean running = false;
    private volatile long lastRun = -1;
    private volatile boolean queued = false;

    public IntervalPeriodExecution(@NotNull Runnable runner, int delay, TimeUnit unit) {
        this.runner = runner;
        this.minDelay = unit.toMillis(delay);
    }

    public void requestRun() {
        synchronized (lock) {
            queued = true;
            if (!running) {
                // Note: use a one-time execution in the pool.  If instead this class kept its
                // own background thread, it leads to out-of-memory issues.
                ApplicationManager.getApplication().executeOnPooledThread(new DelayRunner());
            }
        }
    }

    private class DelayRunner implements Runnable {
        @Override
        public void run() {
            synchronized (lock) {
                running = true;
            }
            while (true) {
                LOG.debug("Starting queue check for interval period.");
                final long now = System.currentTimeMillis();
                long delayTime;
                synchronized (lock) {
                    if (!queued) {
                        LOG.debug("Nothing queued; stopping execution.");
                        running = false;
                        return;
                    }
                    // If lastRun < 0 (first run), this will be negative.
                    delayTime = lastRun + minDelay - now;
                }
                if (delayTime <= 0) {
                    LOG.debug("Executing queued request.");
                    synchronized (lock) {
                        queued = false;
                    }
                    runner.run();
                    // Note: if something is queued after running,
                    // we need to wait a minimum time before running again.
                    synchronized (lock) {
                        lastRun = System.currentTimeMillis();
                    }
                } else {
                    // Still need to wait some time before running.
                    LOG.debug("Waiting " + delayTime + "ms before trying the execution.");
                    try {
                        Thread.sleep(delayTime);
                    } catch (InterruptedException e) {
                        // This is an expected state.
                        // Continue on with the loop.
                    }
                }
            }
        }
    }
}
