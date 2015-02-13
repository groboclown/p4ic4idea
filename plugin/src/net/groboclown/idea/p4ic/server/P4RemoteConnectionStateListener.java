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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to when the state of a server changes to connected or disconnected.
 * The user cannot intervene to change the connection state.
 */
public interface P4RemoteConnectionStateListener {
    public static final Topic<P4RemoteConnectionStateListener> TOPIC =
            new Topic<P4RemoteConnectionStateListener>("p4ic.remote.status", P4RemoteConnectionStateListener.class);


    public void onPerforceServerDisconnected(@NotNull ServerConfig config);

    public void onPerforceServerConnected(@NotNull ServerConfig config);


    public static class MessageBusListener implements P4RemoteConnectionStateListener {
        @NotNull
        private final Project project;

        public MessageBusListener(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void onPerforceServerDisconnected(@NotNull ServerConfig config) {
            if (project.isDisposed()) {
                return;
            }
            project.getMessageBus().syncPublisher(TOPIC).onPerforceServerDisconnected(config);
        }

        @Override
        public void onPerforceServerConnected(@NotNull ServerConfig config) {
            if (project.isDisposed()) {
                return;
            }
            project.getMessageBus().syncPublisher(TOPIC).onPerforceServerConnected(config);
        }
    }
}
