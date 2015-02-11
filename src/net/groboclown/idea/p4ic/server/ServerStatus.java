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
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

public interface ServerStatus {
    ServerExecutor getExecutorForClient(@NotNull Project project, @NotNull String clientName);

    void changeClientName(@NotNull String oldClientName, @NotNull String newCLientName);

    void removeClient(@NotNull String clientName);

    @NotNull
    public ServerConfig getConfig();

    public boolean isWorkingOffline();

    public boolean isWorkingOnline();

    public void onReconnect();

    /**
     * @return true if the caller should retry to reconnect, false if not.
     */
    public boolean onDisconnect();

    void forceDisconnect();
}
