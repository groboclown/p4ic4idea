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

package net.groboclown.p4.server.api.cache.messagebus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.messagebus.ApplicationMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Cache update message that is broadcast across the application.  Event topic
 * objects must directly reference {@link TopicListener}, rather than a subclass,
 * in order to have the
 * {@link #abstractAddListener(MessageBusClient.ApplicationClient, Topic, String, AbstractCacheUpdateEvent.Visitor)}
 * work right.
 * <p>
 * If the message listener has more than one method, then each method should have its own
 * topic, adding a listener should add the listener to both topics, and sending the method
 * should send it to just the correlated topic.
 *
 * @param <E> event message sent
 */
public class AbstractCacheMessage<E extends AbstractCacheUpdateEvent<E>>
        extends ApplicationMessage<AbstractCacheMessage.TopicListener<E>> {
    /**
     * Used by messages as the topic listening type.
     *
     * @param <E>
     */
    public interface TopicListener<E extends AbstractCacheUpdateEvent<E>> {
        void onEvent(@NotNull E event);
    }

    /**
     * The actual event listener that the topic subscribes to.
     *
     * @param <E>
     */
    private static class VisitEventListener<E extends AbstractCacheUpdateEvent<E>> implements TopicListener<E> {
        private final String cacheId;
        private final AbstractCacheUpdateEvent.Visitor<E> visitor;

        protected VisitEventListener(@NotNull String cacheId, @NotNull AbstractCacheUpdateEvent.Visitor<E> visitor) {
            this.cacheId = cacheId;
            this.visitor = visitor;
        }

        @Override
        public void onEvent(@NotNull E event) {
            event.visit(cacheId, visitor);
        }
    }

    protected static <E extends AbstractCacheUpdateEvent<E>> void abstractAddListener(
            @NotNull MessageBusClient.ApplicationClient client, @NotNull Topic<TopicListener<E>> topic,
            @NotNull String cacheId,
            @NotNull AbstractCacheUpdateEvent.Visitor<E> visitor) {
        addTopicListener(client, topic, new VisitEventListener<>(cacheId, visitor));
    }

    protected static <E extends AbstractCacheUpdateEvent<E>> void abstractSendEvent(
            @NotNull Topic<? extends TopicListener<E>> topic, @NotNull E event) {
        if (canSendMessage()) {
            ApplicationManager.getApplication().getMessageBus().syncPublisher(topic).onEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <E extends AbstractCacheUpdateEvent<E>> Topic<TopicListener<E>> createTopic(@NonNls @NotNull String displayName) {
        return new Topic(displayName,
                TopicListener.class,
                Topic.BroadcastDirection.TO_CHILDREN);
    }
}
