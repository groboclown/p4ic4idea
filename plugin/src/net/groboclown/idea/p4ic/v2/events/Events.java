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
        ApplicationManager.getApplication().getMessageBus().syncPublisher(BaseConfigUpdatedListener.TOPIC_NORMAL).
                configUpdated(project, sources);
    }
    */

    // Unfortunately, there is very strict ordering to when the messages are sent.
    // The low-level ServerConnection objects are called first,
    // Then the P4Server objects, then everyone else.


    public static void registerAppBaseConfigUpdated(@NotNull MessageBusConnection appBus, @NotNull BaseConfigUpdatedListener listener) {
        appBus.subscribe(BaseConfigUpdatedListener.TOPIC_NORMAL, listener);
    }

    public static void registerP4ServerAppBaseConfigUpdated(@NotNull MessageBusConnection appBus,
            @NotNull BaseConfigUpdatedListener listener) {
        appBus.subscribe(BaseConfigUpdatedListener.TOPIC_P4SERVER, listener);
    }

    public static void registerServerConnectionAppBaseConfigUpdated(@NotNull MessageBusConnection appBus,
            @NotNull BaseConfigUpdatedListener listener) {
        appBus.subscribe(BaseConfigUpdatedListener.TOPIC_SERVERCONFIG, listener);
    }


    public static void configInvalid(@NotNull Project project, @NotNull P4Config config,
            @NotNull P4InvalidConfigException e)
            throws P4InvalidConfigException {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC_SERVERCONNECTION).
                configurationProblem(project, config, e);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC_P4SERVER).
                configurationProblem(project, config, e);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC_NORMAL).
                configurationProblem(project, config, e);
        throw e;
    }


    public static void handledConfigInvalid(@NotNull Project project, @NotNull P4Config config,
            @NotNull VcsConnectionProblem e) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC_SERVERCONNECTION).
                configurationProblem(project, config, e);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC_P4SERVER).
                configurationProblem(project, config, e);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC_NORMAL).
                configurationProblem(project, config, e);
    }


    public static void registerAppConfigInvalid(@NotNull MessageBusConnection appBus, @NotNull ConfigInvalidListener listener) {
        appBus.subscribe(ConfigInvalidListener.TOPIC_NORMAL, listener);
    }


    public static void registerP4ServerAppConfigInvalid(@NotNull MessageBusConnection appBus,
            @NotNull ConfigInvalidListener listener) {
        appBus.subscribe(ConfigInvalidListener.TOPIC_P4SERVER, listener);
    }


    public static void registerServerConnectionAppConfigInvalid(@NotNull MessageBusConnection appBus,
            @NotNull ConfigInvalidListener listener) {
        appBus.subscribe(ConfigInvalidListener.TOPIC_SERVERCONNECTION, listener);
    }

    // These states don't require the strict ordering.

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
