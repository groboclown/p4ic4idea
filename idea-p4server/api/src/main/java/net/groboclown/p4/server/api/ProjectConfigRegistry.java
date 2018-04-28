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
import net.groboclown.p4.server.api.config.ClientConfigState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 */
public abstract class AbstractProjectConfigRegistry
        implements ProjectComponent, Disposable {
    public static final String COMPONENT_NAME = AbstractProjectConfigRegistry.class.getName();

    private static final Logger LOG = Logger.getInstance(AbstractProjectConfigRegistry.class);

    private boolean disposed = false;

    @NotNull
    public static AbstractProjectConfigRegistry getInstance(@NotNull Project project) {
        return (AbstractProjectConfigRegistry) project.getComponent(COMPONENT_NAME);
    }


    /**
     * Retrieve the client configuration information about the client server ref.  Even though the
     * connections are registered application-wide, individual projects must register themselves
     *
     * @param ref client reference
     * @return the client config state, or null if it isn't registered.
     */
    @Nullable
    public abstract ClientConfigState getRegisteredClientConfigState(@NotNull ClientServerRef ref);

    /**
     * Registers the client configuration to the project and the application.  If a configuration with the same
     * client-server reference is already registered, then it will be removed.  If that configuration is the exact
     * same as the requested added configuration, then it will still be removed then re-added.
     *
     * @param config configuration to register
     */
    public abstract void addClientConfig(@NotNull ClientConfig config);

    /**
     * Removes the client configuration registration with the given reference.  If it is registered, then
     * the appropriate messages will be sent out.
     *
     * @param ref the reference to de-register
     * @return true if it was registered, false if not.
     */
    public abstract boolean removeClientConfig(@NotNull ClientServerRef ref);

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
    }

    @Override
    public void disposeComponent() {
        dispose();
    }

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
