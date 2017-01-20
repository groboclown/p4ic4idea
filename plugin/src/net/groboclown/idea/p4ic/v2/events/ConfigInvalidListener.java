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
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.util.messages.Topic;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Project level topic.
 */
public interface ConfigInvalidListener {
    Topic<ConfigInvalidListener> TOPIC_NORMAL =
            new Topic<ConfigInvalidListener>("p4ic.config.invalid", ConfigInvalidListener.class);
    Topic<ConfigInvalidListener> TOPIC_P4SERVER =
            new Topic<ConfigInvalidListener>("p4ic.config.invalid.p4server", ConfigInvalidListener.class);
    Topic<ConfigInvalidListener> TOPIC_SERVERCONNECTION =
            new Topic<ConfigInvalidListener>("p4ic.config.invalid.p4server.serverconnection", ConfigInvalidListener.class);

    void configurationProblem(@NotNull Project project, @NotNull P4ProjectConfig config,
            @NotNull VcsConnectionProblem ex);
}
