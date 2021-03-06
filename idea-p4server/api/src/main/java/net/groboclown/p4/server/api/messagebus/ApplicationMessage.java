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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Marker class for the application-level messages.  These messages should be limited to just messages
 * about the registered clients, which are owned application-wide.
 *
 * @param <L> listener type
 */
public abstract class ApplicationMessage<L> {
    @Nullable
    protected static <L> L getListener(@NotNull Topic<L> topic) {
        if (canSendMessage()) {
            return ApplicationManager.getApplication().getMessageBus().syncPublisher(topic);
        }
        return null;
    }

    @NotNull
    protected static <L> L getListener(@NotNull Topic<L> topic, @NotNull L defaultListener) {
        L listener = getListener(topic);
        if (listener == null) {
            listener = defaultListener;
        }
        return listener;
    }

    protected static boolean canSendMessage() {
        // Eventually, this might find a situation in which the message bus is unavailable.
        return true;
    }

    protected static <L> void addTopicListener(@NotNull MessageBusClient.ApplicationClient client,
            @NotNull Topic<L> topic, @NotNull L listener, @NotNull Class<? extends L> listenerClass,
            @NotNull Object listenerOwner) {
        client.add(topic, ListenerProxy.createProxy(listener, listenerClass, listenerOwner));
    }
}
