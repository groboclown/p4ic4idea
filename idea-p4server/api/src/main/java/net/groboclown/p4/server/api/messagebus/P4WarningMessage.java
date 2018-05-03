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
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ClientError;
import com.perforce.p4java.exception.ConnectionException;
import org.jetbrains.annotations.NotNull;

public class P4WarningMessage extends ProjectMessage<P4WarningMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:Perforce server warnings";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);

    public interface Listener {
        /**
         *
         * @param e one of {@link ConnectionException} or {@link AccessException}
         */
        void disconnectCausedError(@NotNull Exception e);

        void charsetTranslationError(@NotNull ClientError e);
    }


    public static void sendDisconnectCausedError(@NotNull Project project, @NotNull ConnectionException e) {
        if (canSendMessage(project)) {
            project.getMessageBus().syncPublisher(TOPIC).disconnectCausedError(e);
        }
    }


    public static void sendDisconnectCausedError(@NotNull Project project, @NotNull AccessException e) {
        if (canSendMessage(project)) {
            project.getMessageBus().syncPublisher(TOPIC).disconnectCausedError(e);
        }
    }


    public static void sendCharsetTranslationError(@NotNull Project project, @NotNull ClientError e) {
        if (canSendMessage(project)) {
            project.getMessageBus().syncPublisher(TOPIC).charsetTranslationError(e);
        }
    }
}
