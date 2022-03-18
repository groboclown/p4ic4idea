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
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A stateless task runner for modules that need to inject on-startup behavior.
 */
public abstract class AbstractProjectStartup extends AbstractDisposable {
    public interface ProjectRegister {
        /**
         * Setup project level settings.
         * @param project project instance
         */
        void setupForProject(@NotNull Project project);
    }

    public interface ProjectListenerRegister {
        /**
         * Register project-level message bus event listeners.
         *
         * @param project project being registered.
         * @param projectBusClient message bus client.
         */
        void registerProjectListeners(@NotNull Project project,
                @NotNull MessageBusClient.ProjectClient projectBusClient);
    }


    private final ProjectRegister projectRegister;
    private final ProjectListenerRegister projectListenerRegister;

    protected AbstractProjectStartup(
            @Nullable final ProjectRegister projectRegister,
            @Nullable final ProjectListenerRegister projectListenerRegister,
            @Nullable final Disposable projectShutdown) {
        super(projectShutdown);
        this.projectRegister = projectRegister;
        this.projectListenerRegister = projectListenerRegister;
    }


    /**
     * Called each time a project loads.
     *
     * @param project project object.
     */
    public void onProjectStartup(@NotNull Project project) {
        if (this.projectRegister != null) {
            this.projectRegister.setupForProject(project);
        }
        if (this.projectListenerRegister != null) {
            final MessageBusClient.ProjectClient projectBusClient = MessageBusClient.forProject(project, this);
            this.projectListenerRegister.registerProjectListeners(project, projectBusClient);
        }
    }
}
