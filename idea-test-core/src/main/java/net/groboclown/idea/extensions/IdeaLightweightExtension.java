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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.pico.DefaultPicoContainer;
import net.groboclown.idea.mock.FieldHelper;
import net.groboclown.idea.mock.MockPasswordSafe;
import net.groboclown.idea.mock.MockVcsContextFactory;
import net.groboclown.idea.mock.P4icMockApplication;
import net.groboclown.idea.mock.P4icMockProject;
import net.groboclown.idea.mock.SingleThreadedMessageBus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.PicoContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
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

    public P4icMockApplication getMockApplication() {
        return getApplication(getTopContext());
    }

    public FileTypeRegistry getMockFileTypeRegistry() {
        return getMockFileTypeRegistry(getTopContext());
    }

    public P4icMockProject getMockProject() {
        return getMockProject(getTopContext());
    }

    public DefaultPicoContainer getProjectPicoContainer() {
        return getProjectPicoContainer(getTopContext());
    }

    public LocalFileSystem getMockLocalFilesystem() {
        return getMockLocalFilesystem(getTopContext());
    }

    public <T> void registerApplicationComponent(@NotNull Class<? super T> name, @NotNull T component) {
        getMockApplication().getPicoContainer().
                registerComponentImplementation(component, name);
    }

    public <I> void registerApplicationService(@NotNull Class<? super I> interfaceClass, @NotNull I service) {
        getMockApplication().getPicoContainer().registerComponentInstance(interfaceClass, service);
        getMockApplication().getPicoContainer().registerComponentInstance(interfaceClass.getName(), service);
    }

    public <T> void registerProjectComponent(@NotNull Class<? super T> interfaceClass, @NotNull T component) {
        getMockProject().getPicoContainer().registerComponentInstance(interfaceClass, component);
        getMockProject().getPicoContainer().registerComponentInstance(interfaceClass.getName(), component);
    }

    public <T> void registerProjectService(@NotNull Class<? super T> serviceClass, @NotNull T service) {
        getMockProject().getPicoContainer().registerComponentInstance(serviceClass, service);
        getMockProject().getPicoContainer().registerComponentInstance(serviceClass.getName(), service);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        contextStack.add(0, extensionContext);
        final DefaultPicoContainer container = new DefaultPicoContainer();
        getStore(extensionContext).put("pico", container);

        DisposableRegistry parent = new DisposableRegistry();
        getStore(extensionContext).put("parent", parent);

        final FileTypeRegistry fileTypeRegistry = mock(FileTypeRegistry.class);
        getStore(extensionContext).put("fileTypeRegistry", fileTypeRegistry);

        Application original = ApplicationManager.getApplication();
        getStore(extensionContext).put("original-application", original);

        P4icMockApplication application = P4icMockApplication.setUp(parent);
        getStore(extensionContext).put("application", application);
        initializeApplication(application);

        P4icMockProject project = new P4icMockProject(
                container, new SingleThreadedMessageBus(null), parent);
        getStore(extensionContext).put("project", project);

        LocalFileSystem lfs = mock(LocalFileSystem.class);
        getStore(extensionContext).put("local-filesystem", lfs);
        setupLocalFileSystem(lfs);
    }

    private void initializeApplication(P4icMockApplication application) {
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
        FieldHelper.setTypedStaticField(
                LocalFileSystem.class.getName() + "$LocalFileSystemHolder",
                LocalFileSystem.class,
                lfs);
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

    private P4icMockProject getMockProject(ExtensionContext topContext) {
        return (P4icMockProject) getStore(topContext).get("project");
    }

    private DefaultPicoContainer getProjectPicoContainer(ExtensionContext topContext) {
        return (DefaultPicoContainer) getStore(topContext).get("pico");
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

    private P4icMockApplication getApplication(ExtensionContext context) {
        return (P4icMockApplication) getStore(context).get("application");
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

    static final class TestComponentAdapter implements ComponentAdapter {
        private final Object componentKey;
        private final Object componentInstance;

        public TestComponentAdapter(@NotNull Object componentKey, @NotNull Object componentInstance) {
            this.componentKey = componentKey;
            this.componentInstance = componentInstance;
        }

        @Override
        public Object getComponentInstance(PicoContainer container) {
            return this.componentInstance;
        }

        @Override
        public Object getComponentKey() {
            return this.componentKey;
        }

        @Override
        public Class<?> getComponentImplementation() {
            return this.componentInstance.getClass();
        }
    }

}
