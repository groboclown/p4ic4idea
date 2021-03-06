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

package net.groboclown.p4.server.api;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A layer on top of a {@link ServerStatus} to include the client connection
 * details.  It is limited to project scope, and is the only client configuration
 * for a VCS root.
 * <p>
 * Due to the underlying mechanisms for data storage, the root may store state
 * information insofar as it relates to the client workspace information.
 */
public interface ClientConfigRoot
        extends Disposable, ServerStatus {
    @NotNull
    ClientConfig getClientConfig();

    /**
     *
     * @return true if the details about the client configuration state
     *      have been loaded from the server (or from a cached copy of
     *      the data from the server), or false if the state has no
     *      information about the server configuration.
     */
    boolean isLoadedFromServer();

    /**
     *
     * @return the root of the client work directory, or null if the
     *      client hasn't been loaded yet.
     */
    @Nullable
    VirtualFile getClientRootDir();

    /**
     *
     * @return the project VCS root directory.
     */
    @NotNull
    VirtualFile getProjectVcsRootDir();

    boolean isDisposed();
}
