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

package net.groboclown.p4.server.impl.cache;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The state storage for actions, so that they can be easily cached to file and restored.
 * Needs to be usable by PersistentStateComponent.
 * <p>
 * These action implementations must
 */
public interface PendingAction {

    class State {
        String actionClassName;
        Map<String, String> data;
    }


    State getState();

    /**
     *
     * @return source id
     * @see PendingActionFactory#getSourceId(ClientConfig)
     */
    String getSourceId();

    String getActionId();

    boolean isClientAction();
    P4CommandRunner.ClientAction<?> getClientAction(@NotNull ClientConfig config);

    boolean isServerAction();
    P4CommandRunner.ServerAction<?> getServerAction(@NotNull ServerConfig config);
}
