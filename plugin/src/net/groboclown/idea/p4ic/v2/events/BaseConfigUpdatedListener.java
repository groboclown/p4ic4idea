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
package net.groboclown.idea.p4ic.v2.events;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Project level topic.
 */
public interface BaseConfigUpdatedListener {
    Topic<BaseConfigUpdatedListener> TOPIC_NORMAL =
            new Topic<BaseConfigUpdatedListener>("p4ic.config.changed", BaseConfigUpdatedListener.class);
    Topic<BaseConfigUpdatedListener> TOPIC_P4SERVER =
            new Topic<BaseConfigUpdatedListener>("p4ic.config.changed-p4server", BaseConfigUpdatedListener.class);
    Topic<BaseConfigUpdatedListener> TOPIC_SERVERCONFIG =
            new Topic<BaseConfigUpdatedListener>("p4ic.config.changed-serverconfig", BaseConfigUpdatedListener.class);


    void configUpdated(@NotNull Project project, @NotNull P4ProjectConfig config);
}
