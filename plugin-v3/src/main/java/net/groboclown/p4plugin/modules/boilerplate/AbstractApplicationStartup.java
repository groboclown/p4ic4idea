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

package net.groboclown.p4plugin.modules.boilerplate;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A stateless task runner for modules that need to inject on-startup behavior.
 */
public class AbstractApplicationStartup extends AbstractDisposable {
    public interface ApplicationRegister {
        /**
         * Setup application level settings.
         * @param application
         */
        void setupForApplication(@NotNull Application application);
    }

    public interface ApplicationListenerRegister {
        /**
         * Register application-level message bus event listeners.
         *
         * @param application the application instance.
         * @param applicationBusClient message bus client.
         */
        void registerApplicationListeners(@NotNull Application application,
                @NotNull MessageBusClient.ApplicationClient applicationBusClient);
    }

    private final ApplicationRegister applicationRegister;
    private final ApplicationListenerRegister applicationListenerRegister;


    protected AbstractApplicationStartup(
            @Nullable ApplicationRegister applicationRegister,
            @Nullable ApplicationListenerRegister applicationListenerRegister,
            @Nullable Disposable applicationShutdown) {
        super(applicationShutdown);
        this.applicationRegister = applicationRegister;
        this.applicationListenerRegister = applicationListenerRegister;
    }


    /**
     * Called on application startup.
     */
    public void onApplicationStart() {
        final Application application = ApplicationManager.getApplication();
        if (application != null) {
            if (this.applicationRegister != null) {
                this.applicationRegister.setupForApplication(application);
            }
            if (this.applicationListenerRegister != null) {
                final MessageBusClient.ApplicationClient busClient = MessageBusClient.forApplication(this);
                this.applicationListenerRegister.registerApplicationListeners(application, busClient);
            }
        }
    }
}
