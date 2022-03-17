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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Indicates that a Vcs Root user configuration of the client parts was either added or changed.
 */
public class VcsRootClientPartsMessage
        extends ProjectMessage<VcsRootClientPartsMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:vcs root client parts";
    private static final Topic<VcsRootClientPartsMessage.Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            VcsRootClientPartsMessage.Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final VcsRootClientPartsMessage.Listener
            DEFAULT_LISTENER = new VcsRootClientPartsMessage.ListenerAdapter();

    /**
     * Event object describing the vcs root client part.
     */
    public static class VcsRootClientPartsRemovedEvent {
        private final VirtualFile vcsRoot;

        public VcsRootClientPartsRemovedEvent(@Nonnull VirtualFile vcsRoot) {
            this.vcsRoot = vcsRoot;
        }

        public VirtualFile getVcsRoot() {
            return vcsRoot;
        }
    }

    /**
     * Event object describing the updated client parts for the vcs root.
     */
    public static class VcsRootClientPartsUpdatedEvent {
        private final VirtualFile vcsRoot;
        private final List<ConfigPart> parts;

        public VcsRootClientPartsUpdatedEvent(@Nonnull VirtualFile vcsRoot, @Nonnull List<ConfigPart> parts) {
            this.vcsRoot = vcsRoot;
            this.parts = List.copyOf(parts);
        }

        @Nonnull
        public VirtualFile getVcsRoot() {
            return vcsRoot;
        }

        @Nonnull
        public List<ConfigPart> getParts() {
            return parts;
        }
    }


    public interface Listener {
        /** The VCS root's associated config parts have been removed. */
        void vcsRootClientPartsRemoved(@NotNull VcsRootClientPartsRemovedEvent event);

        /** The VCS root was either added or the associated parts with the root were updated. */
        void vcsRootUpdated(@NotNull VcsRootClientPartsUpdatedEvent event);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void vcsRootClientPartsRemoved(@Nonnull VcsRootClientPartsRemovedEvent event) {
        }

        @Override
        public void vcsRootUpdated(@Nonnull VcsRootClientPartsUpdatedEvent event) {
        }
    }

    public static void sendVcsRootClientPartsRemoved(@NotNull Project project, @Nonnull VirtualFile vcsRoot) {
        if (canSendMessage(project)) {
            getListener(project, TOPIC, DEFAULT_LISTENER).vcsRootClientPartsRemoved(
                    new VcsRootClientPartsRemovedEvent(vcsRoot));
        }
    }

    public static void sendVcsRootClientPartsUpdated(
            @NotNull Project project, @Nonnull VirtualFile vcsRoot, @Nonnull List<ConfigPart> parts) {
        if (canSendMessage(project)) {
            getListener(project, TOPIC, DEFAULT_LISTENER).vcsRootUpdated(
                    new VcsRootClientPartsUpdatedEvent(vcsRoot, parts));
        }
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull VcsRootClientPartsMessage.Listener listener) {
        addListener(client, TOPIC, listener, VcsRootClientPartsMessage.Listener.class, listenerOwner);
    }
}
