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

package net.groboclown.p4.server.connection;

/**
 * An extension to the {@link ServerConnectedController} that is used by the
 * {@link ClientExec} to announce when the server is connected or disconnected.
 * This allows for a better, more central location for handling the connection
 * information distribution.
 */
public interface ServerStatusController extends ServerConnectedController {
    /**
     * The server is now actually connected.
     *
     *  FIXME see if this should require a Project
     */
    void onConnected();

    /**
     * The server could not be reached.
     */
    void onDisconnected();

    /**
     * A configuration problem occurred.
     */
    void onConfigInvalid();
}
