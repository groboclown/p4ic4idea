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

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Marks a class as a project-level message; messages that are only passed to components within the same project.
 *
 * @param <L> listener type.
 */
public abstract class ProjectMessage<L> {
    @Nullable
    protected static <L> L getListener(@NotNull Project project, @NotNull Topic<L> topic) {
        if (canSendMessage(project)) {
            return project.getMessageBus().syncPublisher(topic);
        }
        return null;
    }

    @NotNull
    protected static <L> L getListener(@NotNull Project project, @NotNull Topic<L> topic, @NotNull L defaultListener) {
        L listener = getListener(project, topic);
        if (listener == null) {
            listener = defaultListener;
        }
        return listener;
    }

    static boolean canSendMessage(@NotNull Project project) {
        return project.isInitialized() && !project.isDisposed();
    }

    protected static <L> void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Topic<L> topic, @NotNull L listener, @NotNull Class<? extends L> listenerClass,
            @NotNull Object listenerOwner) {
        client.add(topic, ListenerProxy.createProxy(listener, listenerClass, listenerOwner));
    }
}
