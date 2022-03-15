// Copyright (C) Zilliant, Inc.
package net.groboclown.idea.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ModalityInvokator;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.pico.DefaultPicoContainer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockApplication implements Application {
    private final DefaultPicoContainer picoContainer = new DefaultPicoContainer();
    private final Map<Key<?>, Object> userData = new HashMap<>();
    private ModalityState modalityState = new MockModalityState();
    private MockModalityInvokator invokator = new MockModalityInvokator();
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

    public MockApplication() {
        setInThreadRunners();
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
    public void invokeLaterOnWriteThread(Runnable runnable, ModalityState modalityState) {
        pooledRunner.apply(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void invokeLaterOnWriteThread(Runnable runnable, ModalityState modalityState,
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
    public void addApplicationListener(@NotNull ApplicationListener applicationListener) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void addApplicationListener(@NotNull ApplicationListener applicationListener,
            @NotNull Disposable disposable) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void removeApplicationListener(@NotNull ApplicationListener applicationListener) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void saveAll() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void saveSettings() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void exit() {
        throw new IllegalStateException("not implemented");
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

    @Override
    public @NotNull ModalityInvokator getInvokator() {
        return invokator;
    }

    @Override
    public @NotNull ModalityState getCurrentModalityState() {
        return modalityState;
    }

    @Override
    public @NotNull ModalityState getModalityStateForComponent(@NotNull Component component) {
        return modalityState;
    }

    @Override
    public @NotNull ModalityState getDefaultModalityState() {
        return modalityState;
    }

    @Override
    public @NotNull ModalityState getNoneModalityState() {
        return modalityState;
    }

    @Override
    public @NotNull ModalityState getAnyModalityState() {
        return modalityState;
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getIdleTime() {
        return 0;
    }

    @Override
    public boolean isUnitTestMode() {
        // Yes, we're in unit-test mode.
        return true;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        // Unit tests are headless.
        return true;
    }

    @Override
    public boolean isCommandLine() {
        return false;
    }

    @Override
    public boolean isRestartCapable() {
        return false;
    }

    @Override
    public void restart() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public @NotNull AccessToken acquireReadActionLock() {
        return readLock;
    }

    @Override
    public @NotNull AccessToken acquireWriteActionLock(@NotNull Class<?> aClass) {
        return writeLock;
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public boolean isEAP() {
        return false;
    }

    @Override
    public <T> T getComponent(@NotNull Class<T> aClass) {
        return aClass.cast(getPicoContainer().getComponentInstance(aClass));
    }

    @Override
    public @NotNull PicoContainer getPicoContainer() {
        return picoContainer;
    }

    @Override
    public @NotNull MessageBus getMessageBus() {
        return bus;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public @NotNull Condition<?> getDisposed() {
        return new Condition<Object>() {
            @Override
            public boolean value(Object o) {
                return false;
            }
        };
    }

    // Introduced in v>=211
    // @Override
    public @NotNull RuntimeException createError(@NotNull Throwable throwable, @NotNull PluginId pluginId) {
        return new RuntimeException(throwable);
    }


    // Introduced in v>=211
    //@Override
    public @NotNull RuntimeException createError(@NotNull @NonNls String s, @NotNull PluginId pluginId) {
        return new RuntimeException(s);
    }

    // Introduced in v>=211
    //@Override
    public @NotNull RuntimeException createError(@NotNull @NonNls String s, @NotNull PluginId pluginId,
            @Nullable Map<String, String> map) {
        return new RuntimeException(s);
    }

    // Removed in v>=211
    //@Override
    public @NotNull <T> Class<T> loadClass(@NotNull String s, @NotNull PluginDescriptor pluginDescriptor)
            throws ClassNotFoundException {
        return (Class<T>) getClass().getClassLoader().loadClass(s);
    }

    @Override
    public void dispose() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    @Nullable
    public <T> T getUserData(@NotNull Key<T> key) {
        return (T) userData.get(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
        if (t == null) {
            userData.remove(key);
        } else {
            userData.put(key, t);
        }
    }

    public DefaultPicoContainer mockGetPicoContainer() {
        return picoContainer;
    }


    private class MockModalityInvokator implements ModalityInvokator {

        @Override
        public @NotNull ActionCallback invokeLater(@NotNull Runnable runnable) {
            runnable.run();
            return new ActionCallback();
        }

        @Override
        public @NotNull ActionCallback invokeLater(@NotNull Runnable runnable, @NotNull Condition<?> condition) {
            runnable.run();
            return new ActionCallback();
        }

        @Override
        public @NotNull ActionCallback invokeLater(@NotNull Runnable runnable, @NotNull ModalityState modalityState) {
            runnable.run();
            return new ActionCallback();
        }

        @Override
        public @NotNull ActionCallback invokeLater(@NotNull Runnable runnable, @NotNull ModalityState modalityState,
                @NotNull Condition<?> condition) {
            runnable.run();
            return new ActionCallback();
        }
    }
}
