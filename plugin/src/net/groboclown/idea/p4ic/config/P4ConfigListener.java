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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

/**
 * Project level topic.
 */
public interface P4ConfigListener {
    public static final Topic<P4ConfigListener> TOPIC =
            new Topic<P4ConfigListener>("p4ic.config.change", P4ConfigListener.class);


    public void configChanges(@NotNull Project project, @NotNull P4Config original, @NotNull P4Config config);


    public void configurationProblem(@NotNull Project project, @NotNull P4Config config,
                                     @NotNull P4InvalidConfigException ex);
}
