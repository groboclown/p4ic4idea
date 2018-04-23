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

import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.ClientServerRef;
import org.jetbrains.annotations.NotNull;

/**
 * Application-scoped request, because the client-server connections are shared between
 * projects.
 */
public class ReconnectMessage extends ApplicationMessage<ReconnectMessage.Listener> {
    public static final String DISPLAY_NAME = "p4ic4idea:reconnect to server";
    public static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);


    public interface Listener {
        void reconnectToAllClients(boolean mayDisplayDialogs);

        void reconnectToClient(@NotNull ClientServerRef ref, boolean mayDisplayDialogs);
    }


    public static void requestReconnectToAllClients(boolean mayDisplayDialogs) {
        if (canSendMessage()) {
            getListener(TOPIC).reconnectToAllClients(mayDisplayDialogs);
        }
    }


    public static void requestReconnectToClient(@NotNull ClientServerRef ref, boolean mayDisplayDialogs) {
        if (canSendMessage()) {
            getListener(TOPIC).reconnectToClient(ref, mayDisplayDialogs);
        }
    }
}
