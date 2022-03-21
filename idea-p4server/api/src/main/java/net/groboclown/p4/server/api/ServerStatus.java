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
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps the {@link ServerConfig} along with information that can change depending upon
 * the user requests and server connection results.  It is limited to project scope.
 * The status does not maintain any cached state.
 */
public interface ServerStatus
        extends Disposable {
    @NotNull
    ServerConfig getServerConfig();

    boolean isOffline();

    boolean isOnline();

    boolean isServerConnectionProblem();

    boolean isUserWorkingOffline();

    boolean isDisposed();

    /**
     * @return true if the server does not require a password to be sent.
     */
    boolean isPasswordUnnecessary();

    /**
     *
     * @return true if the connection has not been logged in yet, or if the active session has expired.  This may
     *      trigger password requests.  Changing the stored password should also reset this.
     */
    boolean isLoginNeeded();

    /**
     *
     * @return true if the current login information is known to be bad.  This is different from a problem
     *      to connect to the server due to network issues.  Changing the stored password should reset this.
     */
    boolean isLoginBad();

    /**
     *
     * @return true if the last request to the server caused a bad connection.
     */
    boolean isServerConnectionBad();

    /**
     *
     * @return true if the details about the server configuration state
     *      have been loaded from the server (or from a cached copy of
     *      the data from the server), or false if the state has no
     *      information about the server configuration.
     */
    boolean isLoadedFromServer();

    /**
     *
     * @return true if the pending actions list requires a resend to the server.
     *      By allowing this to be false, it prevents the code from retrying to
     *      send when the state should not allow it.
     */
    boolean isPendingActionsListResendRequired();

    void setPendingActionsListResendRequired(boolean required);

    /**
     *
     * @return true if the server is known to be case-sensitive, false if not,
     *      and null if the state is not known.
     */
    @Nullable
    Boolean isCaseSensitive();
}
