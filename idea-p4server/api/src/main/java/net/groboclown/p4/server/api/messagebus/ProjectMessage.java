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
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Marks a class as a project-level message; messages that are only passed to components within the same project.
 *
 * @param <L> listener type.
 */
public abstract class ProjectMessage<L> {
    @NotNull
    protected static <L> L getListener(@NotNull Project project, @NotNull Topic<L> topic) {
        return project.getMessageBus().syncPublisher(topic);
    }

    protected static boolean canSendMessage(@NotNull Project project) {
        return project.isInitialized() && !project.isDisposed();
    }

    protected static <L> void addListener(@NotNull MessageBusClient client, @NotNull Topic<L> topic, @NotNull L listener) {
        assert client.isProjectBus();
        client.add(topic, listener);
    }
}
