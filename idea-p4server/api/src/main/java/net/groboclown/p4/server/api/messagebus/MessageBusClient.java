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

package net.groboclown.p4.server.api.messagebus;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public class MessageBusClient {
    public static class ProjectClient {
        private final MessageBusConnection connection;
        private ProjectClient(MessageBusConnection connection) {
            this.connection = connection;
        }

        public <L> void add(@NotNull Topic<L> topic, @NotNull L listener) {
            connection.subscribe(topic, listener);
        }

        public void close() {
            connection.disconnect();
        }
    }

    public static class ApplicationClient {
        private final MessageBusConnection connection;
        private ApplicationClient(MessageBusConnection connection) {
            this.connection = connection;
        }

        public <L> void add(@NotNull Topic<L> topic, @NotNull L listener) {
            connection.subscribe(topic, listener);
        }

        public void close() {
            connection.disconnect();
        }
    }

    @NotNull
    public static ProjectClient forProject(@NotNull Project project, @NotNull Disposable owner) {
        return new ProjectClient(project.getMessageBus().connect(owner));
    }

    @NotNull
    public static ApplicationClient forApplication(@NotNull Disposable owner) {
        return new ApplicationClient(ApplicationManager.getApplication().getMessageBus().connect(owner));
    }
}
