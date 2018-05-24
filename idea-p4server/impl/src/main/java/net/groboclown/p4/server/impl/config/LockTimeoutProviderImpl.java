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

    @Override
    public <T> T withLock(@NotNull Lock lock, @NotNull InterruptableSupplier<T> supplier)
            throws InterruptedException {
        lock.tryLock(getLockTimeout(), getLockTimeoutUnit());
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void withLock(@NotNull Lock lock, @NotNull InterruptableRunner runnable)
            throws InterruptedException {
        lock.tryLock(getLockTimeout(), getLockTimeoutUnit());
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    public void setTimeout(int timeout, @NotNull TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be positive, found " + timeout);
        }
        this.timeout = timeout;
        this.unit = unit;
    }
}
