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

package net.groboclown.p4.server.api;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 */
public abstract class ProjectConfigRegistry
        implements ProjectComponent, Disposable {
    public static final String COMPONENT_NAME = ProjectConfigRegistry.class.getName();

    private static final Logger LOG = Logger.getInstance(ProjectConfigRegistry.class);

    private final Project project;
    private final Map<ClientServerRef, ClientConfig> registeredConfigs = new HashMap<>();
    private boolean disposed = false;

    @NotNull
    public static ProjectConfigRegistry getInstance(@NotNull Project project) {
        return (ProjectConfigRegistry) project.getComponent(COMPONENT_NAME);
    }

    protected ProjectConfigRegistry(Project project) {
        this.project = project;
    }


    /**
     * Retrieve the client configuration information about the client server ref.  Even though the
     * connections are registered application-wide, individual projects must register themselves
     *
     * @param ref client reference
     * @return
     */
    @Nullable
    public ClientConfig getRegisteredClientConfig(@NotNull ClientServerRef ref) {
        synchronized (registeredConfigs) {
            return registeredConfigs.get(ref);
        }
    }

    /**
     * Registers the client configuration to the project and the application.  If a configuration with the same
     * client-server reference is already registered, then it will be removed.  If that configuration is the exact
     * same as the requested added configuration, then it will still be removed then re-added.
     *
     * @param config configuration to register
     */
    public void addClientConfig(@NotNull ClientConfig config) {
        checkDisposed();
        ClientServerRef ref = config.getClientServerRef();
        ClientConfig existing;
        synchronized (registeredConfigs) {
            existing = registeredConfigs.get(ref);
            registeredConfigs.put(ref, config);
        }
        if (existing != null) {
            ClientConfigRemovedMessage.reportClientConfigRemoved(project, existing);
            removeConfigFromApplication(project, config);
        }
        ClientConfigAddedMessage.reportClientConfigAdded(project, config);
        addConfigToApplication(project, config);
    }

    /**
     * Removes the client configuration registration with the given reference.  If it is registered, then
     * the appropriate messages will be sent out.
     *
     * @param ref the reference to de-register
     * @return true if it was registered, false if not.
     */
    public boolean removeClientConfig(@NotNull ClientServerRef ref) {
        checkDisposed();
        ClientConfig removed;
        synchronized (registeredConfigs) {
            removed = registeredConfigs.remove(ref);
        }
        boolean registered = removed != null;
        if (registered) {
            ClientConfigRemovedMessage.reportClientConfigRemoved(project, removed);
            removeConfigFromApplication(project, removed);
        }
        return registered;
    }

    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void dispose() {
        disposed = true;
        final Collection<ClientConfig> configs;
        synchronized (registeredConfigs) {
            // need a copy of the values, otherwise they'll be cleared when we
            // clear the registered configs.
            configs = new ArrayList<>(registeredConfigs.values());
            registeredConfigs.clear();
        }
        for (ClientConfig clientConfig : configs) {
            ClientConfigRemovedMessage.reportClientConfigRemoved(project, clientConfig);
            removeConfigFromApplication(project, clientConfig);
        }
    }

    @Override
    public void disposeComponent() {
        dispose();
    }

    protected abstract void addConfigToApplication(@NotNull Project project, @NotNull ClientConfig clientConfig);

    protected abstract void removeConfigFromApplication(@NotNull Project project, @NotNull ClientConfig clientConfig);

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    /** Throws an error if disposed */
    protected void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }
}
