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

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.ServerStatus;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ServerStatusImpl
        implements ServerStatus {
    private static final Logger LOG = Logger.getInstance(ServerStatusImpl.class);

    private final ServerConfig config;
    private boolean disposed;

    // for user explicitly wanting to not try to connect.
    private boolean userWorkingOffline;

    private final Boolean serverCaseSensitive = null;

    private final boolean loaded = false;

    private boolean badConnection = false;
    private boolean badLogin = false;
    private boolean needsLogin = false;
    private boolean passwordUnnecessary = false;
    private boolean pendingActionsRequireResend = true;


    public ServerStatusImpl(@NotNull ServerConfig config) {
        this.config = config;
    }

    @Override
    @NotNull
    public ServerConfig getServerConfig() {
        return config;
    }

    @Override
    public boolean isOffline() {
        return badConnection || badLogin || needsLogin || userWorkingOffline;
    }

    @Override
    public boolean isOnline() {
        return !badConnection && !badLogin && !needsLogin && !userWorkingOffline;
    }

    @Override
    public boolean isServerConnectionProblem() {
        return badConnection || badLogin || needsLogin;
    }

    @Override
    public boolean isUserWorkingOffline() {
        return userWorkingOffline;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public boolean isPasswordUnnecessary() {
        return this.passwordUnnecessary;
    }

    @Override
    public boolean isLoginNeeded() {
        return needsLogin;
    }

    @Override
    public boolean isLoginBad() {
        return badLogin;
    }

    @Override
    public boolean isServerConnectionBad() {
        return badConnection;
    }

    @Override
    public boolean isLoadedFromServer() {
        return loaded;
    }

    @Override
    public boolean isPendingActionsListResendRequired() {
        return pendingActionsRequireResend;
    }

    @Override
    public void setPendingActionsListResendRequired(boolean required) {
        this.pendingActionsRequireResend = required;
    }

    @Nullable
    @Override
    public Boolean isCaseSensitive() {
        return serverCaseSensitive;
    }

    private void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }

    public void setUserOffline(boolean isOffline) {
        userWorkingOffline = isOffline;
    }

    /**
     * Mark the server as switching to connected state.
     */
    public void markServerConnected() {
        // Only affect the pending actions if it was already offline; that means they need to
        // be sent.
        if (! this.isOnline()) {
            this.pendingActionsRequireResend = true;
        }
        this.badConnection = false;

        // Mark as a login is still needed, but discard any concept that the previous login
        // was wrong.
        this.badLogin = false;
        this.needsLogin = true;
        // keep the "password unnecessary" flag state.

        // Don't touch the user offline flag.  Even if we are triggered about a valid connection,
        // the user still requested to be offline.
    }

    /**
     * Mark the server as switching to connected state.
     */
    public void markLoggedIn(boolean usedPassword) {
        // Only affect the pending actions if it was already offline; that means they need to
        // be sent.
        if (! this.isOnline()) {
            this.pendingActionsRequireResend = true;
        }
        this.badLogin = false;
        this.badConnection = false;
        this.needsLogin = false;
        this.passwordUnnecessary = !usedPassword;

        // Don't touch the user offline flag.  Even if we are triggered about a valid connection,
        // the user still requested to be offline.
    }

    /**
     * Mark the server as having failed the login process.
     */
    public void markLoginFailed() {
        this.badLogin = true;
        this.needsLogin = true;
    }

    /**
     * Mark the server as having its session expired.
     */
    public void markSessionExpired() {
        this.needsLogin = true;
    }

    /**
     * Mark the password as being reported by the server as bad.
     */
    public void markBadPassword() {
        this.needsLogin = true;
        this.badLogin = true;
    }

    /**
     * Mark that the password was specified, but the server does not need it.
     */
    public void markPasswordIsUnnecessary() {
        this.needsLogin = true;
        this.passwordUnnecessary = true;
    }

    public void markBadConnection() {
        this.needsLogin = true;
        this.badConnection = true;
    }

    /**
     * Set the state to indicate that a login retry is requested by the user.  This can be called when a retry is
     * explicitly requested, or if the password is changed.
     */
    public void requestLoginRetry() {
        // Only affect the pending actions if it was already offline; that means they need to
        // be sent.
        if (! this.isOnline()) {
            this.pendingActionsRequireResend = true;
        }
        this.needsLogin = true;
        this.badConnection = false;
        this.badLogin = false;

        // The user could have updated their account in a separate program, so this state
        // is updated to indicate the password should be tried again.
        this.passwordUnnecessary = false;
    }
}
