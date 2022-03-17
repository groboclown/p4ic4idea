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

package net.groboclown.idea.mock;

import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.pico.DefaultPicoContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


// extends MockApplication, so we can get the advantages of not needing to
// constantly keep this up to date with the latest API changes.
public class P4icMockApplication extends MockApplication {
    private AccessToken writeLock = new MockLockToken();
    private AccessToken readLock = new MockLockToken();
    private MessageBus bus = new SingleThreadedMessageBus(null);
    private Function<Callable<Object>, Future<Object>> pooledRunner;
    private Consumer<Runnable> edtLaterRunner;
    private Consumer<Runnable> edtWaitRunner;
    private boolean isDispatchThread = true;
    private boolean isWriteThread = true;
    private boolean isWriteAllowed = true;
    private boolean isReadAllowed = true;


    P4icMockApplication(@NotNull Disposable parentDisposable) {
        super(parentDisposable);
        setInThreadRunners();
    }

    @NotNull
    public static P4icMockApplication setUp(@NotNull Disposable parentDisposable) {
        P4icMockApplication app = new P4icMockApplication(parentDisposable);
        ApplicationManager.setApplication(app, parentDisposable);
        return app;
    }

    public void setIsDispatchThread(boolean value) {
        this.isDispatchThread = value;
    }

    public void setIsWriteThread(boolean value) {
        this.isWriteThread = value;
    }

    public void setIsWriteAllowed(boolean value) {
        this.isWriteAllowed = value;
    }

    public void setIsReadAllowed(boolean value) {
        this.isReadAllowed = value;
    }

    public void setPooledRunner(Function<Callable<Object>, Future<Object>> runner) {
        assertNotNull(runner);
        this.pooledRunner = runner;
    }

    public void setInThreadPooledRunner() {
        setPooledRunner((runner) -> {
            try {
                runner.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            CompletableFuture<?> ret = new CompletableFuture<>();
            ret.complete(null);
            return (Future<Object>) ret;
        });
    }

    public void setEdtLaterRunner(Consumer<Runnable> runner) {
        assertNotNull(runner);
        this.edtLaterRunner = runner;
    }

    public void setEdtLaterRunner(Function<Callable<Object>, Future<Object>> runner) {
        assertNotNull(runner);
        this.edtLaterRunner = (arg) -> runner.apply(() -> {
            arg.run();
            return null;
        });
    }

    public void setEdtWaitRunner(Consumer<Runnable> runner) {
        assertNotNull(runner);
        this.edtWaitRunner = runner;
    }

    public void setInThreadRunners() {
        setInThreadPooledRunner();
        setEdtLaterRunner(Runnable::run);
        setEdtWaitRunner(Runnable::run);
    }

    @Override
    public void invokeLaterOnWriteThread(@NotNull Runnable runnable) {
        pooledRunner.apply(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void invokeLaterOnWriteThread(@NotNull Runnable runnable, @NotNull ModalityState modalityState) {
        pooledRunner.apply(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void invokeLaterOnWriteThread(@NotNull Runnable runnable, @NotNull ModalityState modalityState,
            @NotNull Condition<?> condition) {
        pooledRunner.apply(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        edtLaterRunner.accept(runnable);
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable, @NotNull Condition<?> condition) {
        edtLaterRunner.accept(runnable);
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable, @NotNull ModalityState modalityState) {
        edtLaterRunner.accept(runnable);
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable, @NotNull ModalityState modalityState,
            @NotNull Condition<?> condition) {
        edtLaterRunner.accept(runnable);
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable, @NotNull ModalityState modalityState)
            throws ProcessCanceledException {
        edtWaitRunner.accept(runnable);
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable)
            throws ProcessCanceledException {
        edtWaitRunner.accept(runnable);
    }

    @Override
    public @NotNull Future<?> executeOnPooledThread(@NotNull Runnable runnable) {
        return pooledRunner.apply(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public @NotNull <T> Future<T> executeOnPooledThread(@NotNull Callable<T> callable) {
        return (Future<T>) pooledRunner.apply((Callable<Object>) callable);
    }

    @Override
    public void runReadAction(@NotNull Runnable runnable) {
        assertReadAccessAllowed();
        runnable.run();
    }

    @Override
    public <T> T runReadAction(@NotNull Computable<T> computable) {
        assertReadAccessAllowed();
        return computable.compute();
    }

    @Override
    public <T, E extends Throwable> T runReadAction(@NotNull ThrowableComputable<T, E> throwableComputable)
            throws E {
        assertReadAccessAllowed();
        return throwableComputable.compute();
    }

    @Override
    public void runWriteAction(@NotNull Runnable runnable) {
        assertWriteAccessAllowed();
        runnable.run();
    }

    @Override
    public <T> T runWriteAction(@NotNull Computable<T> computable) {
        assertWriteAccessAllowed();
        return computable.compute();
    }

    @Override
    public <T, E extends Throwable> T runWriteAction(@NotNull ThrowableComputable<T, E> throwableComputable)
            throws E {
        assertWriteAccessAllowed();
        return throwableComputable.compute();
    }

    @Override
    public boolean hasWriteAction(@NotNull Class<?> aClass) {
        return false;
    }

    @Override
    public void assertReadAccessAllowed() {
        assertTrue(this.isReadAllowed);
    }

    @Override
    public void assertWriteAccessAllowed() {
        assertTrue(this.isWriteAllowed);
    }

    @Override
    public void assertReadAccessNotAllowed() {
        assertFalse(this.isReadAllowed);
    }

    @Override
    public void assertIsDispatchThread() {
        assertTrue(this.isDispatchThread);
    }

    @Override
    public void assertIsNonDispatchThread() {
        assertFalse(this.isDispatchThread);
    }

    @Override
    public void assertIsWriteThread() {
        assertTrue(this.isWriteThread);
    }

    @Override
    public boolean isWriteAccessAllowed() {
        return this.isWriteAllowed;
    }

    @Override
    public boolean isReadAccessAllowed() {
        return this.isReadAllowed;
    }

    @Override
    public boolean isDispatchThread() {
        return this.isDispatchThread;
    }

    @Override
    public boolean isWriteThread() {
        return this.isWriteThread;
    }

}
