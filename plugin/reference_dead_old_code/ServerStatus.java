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

import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated should instead use a {@link ServerConfig} and {@link ServerConnectedController}.
 */
public interface ServerStatus {
    @NotNull
    ServerConfig getConfig();

    /**
     * @deprecated see ServerConnectionController
     */
    boolean isWorkingOffline();

    /**
     *
     * @deprecated see ServerConnectionController
     */
    boolean isWorkingOnline();

    /**
     * @return true if the caller should retry to reconnect, false if not.
     */
    boolean onDisconnect();

    /**
     * @deprecated see ServerConnectionController
     */
    void forceDisconnect();
}
