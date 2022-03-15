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

package net.groboclown.idea.extensions;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ClassLoaderUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.pico.DefaultPicoContainer;
import net.groboclown.idea.mock.MockApplication;
import net.groboclown.idea.mock.MockPasswordSafe;
import net.groboclown.idea.mock.MockVcsContextFactory;
import net.groboclown.idea.mock.SingleThreadedMessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Junit 5 extension for managing lightweight IDEA tests.  It must be declared as a field
 * with {@link org.junit.jupiter.api.extension.RegisterExtension} annotation.
 * <p>
 * This manages the state used by static calls into manager objects.
 */
public class IdeaLightweightExtension
        implements BeforeEachCallback, AfterEachCallback {

    private final List<ExtensionContext> contextStack = new LinkedList<>();

    public DisposableRegistry getDisposableParent() {
        return getDisposableParent(getTopContext());
    }

    public MockApplication getMockApplication() {
        return getApplication(getTopContext());
    }

    public FileTypeRegistry getMockFileTypeRegistry() {
        return getMockFileTypeRegistry(getTopContext());
    }

    public Project getMockProject() {
        return getMockProject(getTopContext());
    }

    public LocalFileSystem getMockLocalFilesystem() {
        return getMockLocalFilesystem(getTopContext());
    }

    public <T> void registerApplicationComponent(@NotNull Class<? super T> name, @NotNull T component) {
        getMockApplication().mockGetPicoContainer().registerComponent(service(name, component));
    }

    public <I> void registerApplicationService(@NotNull Class<? super I> interfaceClass, @NotNull I service) {
        getMockApplication().mockGetPicoContainer().
                registerComponent(service(interfaceClass, service));
    }

    public void registerProjectComponent(@NotNull String name, @NotNull ProjectComponent component) {
        when(getMockProject().getComponent(name)).thenReturn(component);
    }

    public <T> void registerProjectComponent(@NotNull Class<? super T> interfaceClass, @NotNull T component) {
        when(getMockProject().getComponent(interfaceClass)).thenReturn(component);
    }

    public <T> void registerProjectService(@NotNull Class<? super T> serviceClass, @NotNull T service) {
        ((DefaultPicoContainer) getMockProject().getPicoContainer()).
                registerComponent(service(serviceClass, service));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        contextStack.add(0, extensionContext);

        DisposableRegistry parent = new DisposableRegistry();
        getStore(extensionContext).put("parent", parent);

        final FileTypeRegistry fileTypeRegistry = mock(FileTypeRegistry.class);
        getStore(extensionContext).put("fileTypeRegistry", fileTypeRegistry);

        Application original = ApplicationManager.getApplication();
        getStore(extensionContext).put("original-application", original);

        MockApplication application = new MockApplication();
        ApplicationManager.setApplication(application, () -> fileTypeRegistry, parent);
        getStore(extensionContext).put("application", application);
        initializeApplication(application);

        Project project = mock(Project.class);
        when(project.isInitialized()).thenReturn(true);
        when(project.isDisposed()).thenReturn(false);
        getStore(extensionContext).put("project", project);
        initializeProject(project);

        LocalFileSystem lfs = mock(LocalFileSystem.class);
        getStore(extensionContext).put("local-filesystem", lfs);
        setupLocalFileSystem(lfs);
    }

    private void initializeApplication(MockApplication application) {
        // Service setup.  See ServiceManager
        MockPasswordSafe passwordSafe = new MockPasswordSafe();
        registerApplicationService(PasswordSafe.class, passwordSafe);

        MockVcsContextFactory vcsContextFactory = new MockVcsContextFactory();
        registerApplicationService(VcsContextFactory.class, vcsContextFactory);

        VirtualFileManager vfm = mock(VirtualFileManager.class);
        registerApplicationService(VirtualFileManager.class, vfm);

        ApplicationInfo appInfo = mock(ApplicationInfo.class);
        when(appInfo.getApiVersion()).thenReturn("IC-203.1.1");

        registerApplicationService(ApplicationInfo.class, appInfo);
    }

    private void setupLocalFileSystem(LocalFileSystem lfs) {
        // LocalFileSystem has an odd way to set up the underlying implementation.
        // We need to first get the LocalFileSystemHolder then set the underlying
        // ourInstance value.
        try {
            Class<?> cls = lfs.getClass().getClassLoader().loadClass(
                    LocalFileSystem.class.getName() + "$LocalFileSystemHolder");
            for (Field field: cls.getDeclaredFields()) {
                if (field.getType().equals(LocalFileSystem.class) &&
                        Modifier.isStatic(field.getModifiers())) {
                    assertTrue(field.trySetAccessible());
                    Field modifiersField = Field.class.getDeclaredField( "modifiers" );
                    assertTrue(modifiersField.trySetAccessible());
                    modifiersField.setInt( field, field.getModifiers() & ~Modifier.FINAL );
                    field.set(null, lfs);
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeProject(Project project) {
        MessageBus projectBus = new SingleThreadedMessageBus(null);
        when(project.getMessageBus()).thenReturn(projectBus);

        DefaultPicoContainer pico = new DefaultPicoContainer();
        when(project.getPicoContainer()).thenReturn(pico);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        Application app = (Application) getStore(extensionContext).get("original-application");
        if (app != null) {
            ApplicationManager.setApplication(
                    app,
                    new DisposableRegistry());
        }
        if (! contextStack.isEmpty()) {
            contextStack.remove(0);
        }
    }

    private Project getMockProject(ExtensionContext topContext) {
        return (Project) getStore(topContext).get("project");
    }

    private LocalFileSystem getMockLocalFilesystem(ExtensionContext topContext) {
        return (LocalFileSystem) getStore(topContext).get("local-filesystem");
    }

    private FileTypeRegistry getMockFileTypeRegistry(ExtensionContext topContext) {
        return (FileTypeRegistry) getStore(topContext).get("fileTypeRegistry");
    }

    private DisposableRegistry getDisposableParent(ExtensionContext context) {
        return (DisposableRegistry) getStore(context).get("parent");
    }

    private MockApplication getApplication(ExtensionContext context) {
        return (MockApplication) getStore(context).get("application");
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }

    private ExtensionContext getTopContext() {
        assert !contextStack.isEmpty() : "No context stack known";
        return contextStack.get(0);
    }


    private static class DisposableRegistry implements Disposable {
        private final List<Disposable> disposables = new ArrayList<>();

        synchronized void add(@NotNull Disposable d) {
            if (!disposables.contains(d)) {
                disposables.add(d);
            }
        }

        synchronized void remove(@NotNull Disposable d) {
            disposables.remove(d);
        }


        @Override
        synchronized public void dispose() {
            for (Disposable disposable : disposables) {
                disposable.dispose();
            }
        }
    }


    private static <T> ComponentAdapter mockService(@NotNull Class<T> interfaceType) {
        return service(interfaceType, mock(interfaceType));
    }


    private static <T> ComponentAdapter service(@NotNull Class<T> interfaceType, @NotNull T service) {
        return new ComAd<>(interfaceType, service);
    }


    private static class ComAd<T>
            implements ComponentAdapter {
        private final Class<T> serviceType;
        private final T serviceObj;

        private ComAd(Class<T> serviceType, T serviceObj) {
            this.serviceType = serviceType;
            this.serviceObj = serviceObj;
        }

        @Override
        public Object getComponentKey() {
            return serviceType.getName();
        }

        @Override
        public Class<?> getComponentImplementation() {
            return serviceObj.getClass();
        }

        @Override
        public Object getComponentInstance(PicoContainer picoContainer)
                throws PicoInitializationException, PicoIntrospectionException {
            return serviceObj;
        }

        // Removed in v>=211
        // @Override
        public void verify(PicoContainer picoContainer)
                throws PicoIntrospectionException {
            // do nothing
        }
    }

    private final static Answer<Object> IMMEDIATE_THREAD_RUNNER = new ImmediateRunner();

    private static class ImmediateRunner implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation)
                throws Throwable {
            final Object arg = invocation.getArgument(0);
            final Runnable runner;
            if (arg instanceof Runnable) {
                runner = (Runnable) arg;
            } else {
                runner = () -> {
                    try {
                        ((Callable<?>) arg).call();
                    } catch (Exception e) {
                        fail(e);
                    }
                };
            }
            runner.run();
            return null;
        }
    }
}
