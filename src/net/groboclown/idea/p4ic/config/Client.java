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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Client {
    /**
     *
     *
     * @return server instance that connects to this client
     */
    @NotNull
    public ServerExecutor getServer() throws P4InvalidConfigException;

    /**
     *
     * @return user's client that will be used in the server connection
     */
    @NotNull
    public String getClientName();

    /**
     *
     * @return configuration used to connect to the server.
     */
    @NotNull
    public ServerConfig getConfig();

    /**
     *
     * @return local file root that covers this client connection.
     */
    @NotNull
    public List<VirtualFile> getRoots() throws P4InvalidConfigException;

    @NotNull
    public List<FilePath> getFilePathRoots() throws P4InvalidConfigException;

    /**
     *
     * @return true if the user selected to work disconnected from the server
     */
    public boolean isWorkingOffline();

    /**
     *
     * @return true if the user is working online with the server
     */
    public boolean isWorkingOnline();

    /**
     * Force a disconnect to this server.
     */
    public void forceDisconnect();

    /**
     * Called when the client is no longer needed.
     */
    public void dispose();
}
