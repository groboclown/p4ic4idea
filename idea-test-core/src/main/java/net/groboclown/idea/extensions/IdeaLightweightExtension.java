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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import net.groboclown.idea.mock.SingleThreadedMessageBus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Junit 5 extension for managing lightweight IDEA tests.  It must be declared as a field
 * with {@link org.junit.jupiter.api.extension.RegisterExtension} annotation.
 * <p>
 * This manages the state used by static calls into manager objects.
 */
public class IdeaLightweightExtension
        implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private final List<ExtensionContext> contextStack = new LinkedList<>();

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext)
            throws Exception {
        contextStack.add(0, extensionContext);

        DisposableRegistry parent = new DisposableRegistry();
        getStore(extensionContext).put("parent", parent);

        final FileTypeRegistry fileTypeRegistry = mock(FileTypeRegistry.class);
        getStore(extensionContext).put("fileTypeRegistry", fileTypeRegistry);

        Application original = ApplicationManager.getApplication();
        getStore(extensionContext).put("original-application", original);

        Application application = mock(Application.class);
        ApplicationManager.setApplication(application, () -> fileTypeRegistry, parent);
        getStore(extensionContext).put("application", application);

        MessageBus bus = new SingleThreadedMessageBus(null);
        getStore(extensionContext).put("applicationMessageBus", bus);
        when(application.getMessageBus()).thenReturn(bus);

        Project project = mock(Project.class);
        when(project.isInitialized()).thenReturn(true);
        when(project.isDisposed()).thenReturn(false);
        getStore(extensionContext).put("project", project);

        MessageBus projectBus = new SingleThreadedMessageBus(null);
        when(project.getMessageBus()).thenReturn(projectBus);
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext)
            throws Exception {
        Application app = (Application) getStore(extensionContext).get("original-application");
        if (app != null) {
            ApplicationManager.setApplication(
                    app,
                    new DisposableRegistry());
        }
        contextStack.remove(0);
    }

    public DisposableRegistry getDisposableParent() {
        return getDisposableParent(getTopContext());
    }

    public Application getMockApplication() {
        return getApplication(getTopContext());
    }

    public MessageBus getApplicationMessageBus() {
        return getApplicationMessageBus(getTopContext());
    }

    public FileTypeRegistry getMockFileTypeRegistry() {
        return getMockFileTypeRegistry(getTopContext());
    }

    public Project getMockProject() {
        return getMockProject(getTopContext());
    }

    public void registerApplicationComponent(@NotNull String name, @NotNull ApplicationComponent component) {
        when(getMockApplication().getComponent(name)).thenReturn(component);
    }

    public void registerProjectComponent(@NotNull String name, @NotNull ProjectComponent component) {
        when(getMockProject().getComponent(name)).thenReturn(component);
    }

    private Project getMockProject(ExtensionContext topContext) {
        return (Project) getStore(topContext).get("project");
    }

    private FileTypeRegistry getMockFileTypeRegistry(ExtensionContext topContext) {
        return (FileTypeRegistry) getStore(topContext).get("fileTypeRegistry");
    }

    private MessageBus getApplicationMessageBus(ExtensionContext topContext) {
        return (MessageBus) getStore(topContext).get("applicationMessageBus");
    }

    private DisposableRegistry getDisposableParent(ExtensionContext context) {
        return (DisposableRegistry) getStore(context).get("parent");
    }

    private Application getApplication(ExtensionContext context) {
        return (Application) getStore(context).get("application");
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
}
