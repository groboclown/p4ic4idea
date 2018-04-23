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

package net.groboclown.p4.server.api.config;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps the {@link ServerConfig} along with information that can change depending upon
 * the user requests and server connection results.  It is application-wide in scope.
 */
public interface ServerConfigState extends Disposable {
    @NotNull
    ServerConfig getServerConfig();

    boolean isOffline();

    boolean isOnline();

    boolean isServerConnectionProblem();

    boolean isUserWorkingOffline();

    boolean isDisposed();
}
