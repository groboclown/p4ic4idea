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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.messagebus.AbstractMessageEvent;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ProjectMessage;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Signals to the system that the cache completed an update to its file references.
 */
public class FileCacheUpdatedMessage extends ProjectMessage<FileCacheUpdatedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:File cache updated";
    private static final Topic<FileCacheUpdatedMessage.Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            FileCacheUpdatedMessage.Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class FileCacheUpdateEvent extends AbstractMessageEvent {
        private final Collection<VirtualFile> files;

        public FileCacheUpdateEvent(@NotNull Collection<VirtualFile> files) {
            this.files = Collections.unmodifiableCollection(new ArrayList<>(files));
        }

        public FileCacheUpdateEvent(@NotNull FilePath... files) {
            this.files = Collections.unmodifiableCollection(Arrays.stream(files)
                .map(FilePath::getVirtualFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        }

        public FileCacheUpdateEvent(@NotNull P4LocalFile... files) {
            this.files = Collections.unmodifiableCollection(Arrays.stream(files)
                .map(P4LocalFile::getFilePath)
                .map(FilePath::getVirtualFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        }

        public Collection<VirtualFile> getFiles() {
            return files;
        }
    }

    public interface Listener {
        void onFilesCacheUpdated(@NotNull FileCacheUpdateEvent event);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void onFilesCacheUpdated(@NotNull FileCacheUpdateEvent event) {
        }
    }

    public static Listener send(@NotNull Project project) {
        return getListener(project, TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull FileCacheUpdatedMessage.Listener listener) {
        addListener(client, TOPIC, listener, FileCacheUpdatedMessage.Listener.class, listenerOwner);
    }
}
