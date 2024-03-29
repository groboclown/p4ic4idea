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

package net.groboclown.p4.server.api.config;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

public interface LockTimeoutProvider {
    @Nonnegative
    int getLockTimeout();

    @NotNull
    TimeUnit getLockTimeoutUnit();


    <T> T withWriteLock(@NotNull ReadWriteLock lock, @NotNull InterruptableSupplier<T> supplier)
            throws InterruptedException;

    <T> T withReadLock(@NotNull ReadWriteLock lock, @NotNull InterruptableSupplier<T> supplier)
            throws InterruptedException;

    void withWriteLock(@NotNull ReadWriteLock lock, @NotNull InterruptableRunner runnable)
            throws InterruptedException;

    void withReadLock(@NotNull ReadWriteLock lock, @NotNull InterruptableRunner runnable)
            throws InterruptedException;

    /*
    <T> T withLock(@NotNull Lock lock, @NotNull InterruptableSupplier<T> supplier)
            throws InterruptedException;

    void withLock(@NotNull Lock lock, @NotNull InterruptableRunner runnable)
            throws InterruptedException;
    */

    interface InterruptableRunner {
        void run() throws InterruptedException;
    }

    interface InterruptableSupplier<T> {
        T get() throws InterruptedException;
    }
}
