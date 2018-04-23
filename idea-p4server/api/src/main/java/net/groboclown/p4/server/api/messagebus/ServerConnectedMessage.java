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
import net.groboclown.p4.server.api.P4ServerName;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates that a server was successfully contacted without issue.  The server should be
 * considered online.  It's acceptable for a server to have multiple online messages sent
 * without any disconnect / error messages.  Even with this message, a user may still have
 * explicitly told the server to work offline, which means the server should still be considered
 * offline.
 */
public class ServerConnectedMessage
        extends ApplicationMessage<ServerConnectedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:server connected";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);


    public interface Listener {
        void serverConnected(@NotNull P4ServerName serverName);
    }


    public static void requestReconnectToClient(@NotNull P4ServerName serverName) {
        if (canSendMessage()) {
            getListener(TOPIC).serverConnected(serverName);
        }
    }

    public static void addListener(@NotNull MessageBusClient client, @NotNull Listener listener) {
        addListener(client, TOPIC, listener);
    }
}
