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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A stateless task runner for background
 */
public abstract class AbstractProjectBackgroundStartup extends AbstractProjectStartup
        implements StartupActivity.Background {


    protected AbstractProjectBackgroundStartup(
            @Nullable ProjectRegister projectRegister,
            @Nullable ProjectListenerRegister projectListenerRegister,
            @Nullable Disposable projectShutdown) {
        super(projectRegister, projectListenerRegister, projectShutdown);
    }

    @Override
    public void runActivity(@NotNull Project project) {
        onProjectStartup(project);
    }
}
