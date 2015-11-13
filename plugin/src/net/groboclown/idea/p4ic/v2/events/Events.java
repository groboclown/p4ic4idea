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

package net.groboclown.idea.p4ic.v2.events;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

public final class Events {
    private Events() {
        // utility class
    }


    /* Should only be called by the {@link P4ConfigProject}
    public static void baseConfigUpdated(@NotNull Project project, @NotNull List<ProjectConfigSource> sources) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(BaseConfigUpdatedListener.TOPIC).
                configUpdated(project, sources);
    }
    */


    public static void appBaseConfigUpdated(@NotNull MessageBusConnection appBus, @NotNull BaseConfigUpdatedListener listener) {
        appBus.subscribe(BaseConfigUpdatedListener.TOPIC, listener);
    }


    public static void configInvalid(@NotNull Project project, @NotNull P4Config config,
            @NotNull P4InvalidConfigException e)
            throws P4InvalidConfigException {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC).
                configurationProblem(project, config, e);
        throw e;
    }


    public static void handledConfigInvalid(@NotNull Project project, @NotNull P4Config config,
            @NotNull VcsConnectionProblem e) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC).
                configurationProblem(project, config, e);
    }


    public static void appConfigInvalid(@NotNull MessageBusConnection appBus, @NotNull ConfigInvalidListener listener) {
        appBus.subscribe(ConfigInvalidListener.TOPIC, listener);
    }


    public static void serverConnected(@NotNull ServerConfig config) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ServerConnectionStateListener.TOPIC).
                connected(config);
    }


    public static void serverDisconnected(@NotNull ServerConfig config) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ServerConnectionStateListener.TOPIC).
                disconnected(config);
    }


    public static void appServerConnectionState(@NotNull MessageBusConnection appBus, @NotNull ServerConnectionStateListener listener) {
        appBus.subscribe(ServerConnectionStateListener.TOPIC, listener);
    }
}
