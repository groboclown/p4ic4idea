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

package net.groboclown.p4.server.impl.config;

import net.groboclown.p4.server.api.config.LockTimeoutProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

public class LockTimeoutProviderImpl implements LockTimeoutProvider {
    private int timeout = 5;
    private TimeUnit unit = TimeUnit.SECONDS;

    @Override
    public int getLockTimeout() {
        return timeout;
    }

    @NotNull
    @Override
    public TimeUnit getLockTimeoutUnit() {
        return unit;
    }

    public <T> T withWriteLock(@NotNull ReadWriteLock lock, @NotNull InterruptableSupplier<T> supplier)
            throws InterruptedException {
        tryLock(lock.writeLock());
        try {
            return supplier.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void withWriteLock(@NotNull ReadWriteLock lock, @NotNull InterruptableRunner runnable)
            throws InterruptedException {
        tryLock(lock.writeLock());
        try {
            runnable.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public <T> T withReadLock(@NotNull ReadWriteLock lock, @NotNull InterruptableSupplier<T> supplier)
            throws InterruptedException {
        tryLock(lock.readLock());
        try {
            return supplier.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void withReadLock(@NotNull ReadWriteLock lock, @NotNull InterruptableRunner runnable)
            throws InterruptedException {
        tryLock(lock.readLock());
        try {
            runnable.run();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTimeout(int timeout, @NotNull TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be positive, found " + timeout);
        }
        this.timeout = timeout;
        this.unit = unit;
    }

    private final void tryLock(@NotNull Lock lock)
            throws InterruptedException {
        if (!lock.tryLock(getLockTimeout(), getLockTimeoutUnit())) {
            throw new InterruptedException("Timeout acquiring lock after " + getLockTimeout() + " " +
                    getLockTimeoutUnit().toString().toLowerCase());
        }
    }
}
