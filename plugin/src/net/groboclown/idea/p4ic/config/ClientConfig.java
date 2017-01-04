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

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Stores information regarding a server configuration and the specific client/workspace in that
 * server.
 */
public class ClientConfig {
    private final VirtualFile rootDir;
    private final ServerConfig serverConfig;
    private final String clientName;

    public ClientConfig(VirtualFile rootDir, ServerConfig serverConfig, String clientName) {
        this.rootDir = rootDir;
        this.serverConfig = serverConfig;
        this.clientName = clientName;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public String getClientName() {
        return clientName;
    }

    public VirtualFile getRootDir() {
        return rootDir;
    }
}
