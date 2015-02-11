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
import net.groboclown.idea.p4ic.background.VcsFutureSetter;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A MessageBus handler that manages connection issues with the Perforce server
 * configuration.  It does not handle password or disconnect problems.
 */
public interface OnServerConfigurationProblem {
    public static final Topic<OnServerConfigurationProblem> TOPIC =
            new Topic<OnServerConfigurationProblem>("p4ic.remote.problem", OnServerConfigurationProblem.class);


    /**
     *
     * @param future returns true on a configuration change, or false if
     *               the config wasn't changed.
     * @param config the config to change.
     */
    public void onInvalidConfiguration(@NotNull VcsFutureSetter<Boolean> future,
        @Nullable ServerConfig config, @Nullable String message);


    public static class WithMessageBus implements OnServerConfigurationProblem {
        private final Project project;


        public WithMessageBus(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void onInvalidConfiguration(@NotNull VcsFutureSetter<Boolean> future, @NotNull ServerConfig config,
                @Nullable String message) {
            if (!project.isDisposed()) {
                project.getMessageBus().syncPublisher(TOPIC).onInvalidConfiguration(future, config, message);
            }
        }
    }
}
