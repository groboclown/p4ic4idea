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

package net.groboclown.idea.p4ic.v2.server.connection;


import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Maintains the state of the server connection
 */
public interface ServerConnectedController {
    boolean isWorkingOnline();
    boolean isWorkingOffline();
    boolean isAutoOffline();
    boolean isValid();

    /**
     * Tell the server connection to go offline.
     */
    void disconnect();

    /**
     * Ask the server connection to go online.  The implementation may immediately
     * attempt to reconnect, or it may just set a status flag to "online", and perform
     * the actual connection attempt later.
     *
     * @param project the project attempting to connect, so that errors can be reported to
     *                the active project.
     */
    void connect(@NotNull Project project);
}
