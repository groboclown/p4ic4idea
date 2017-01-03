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
package net.groboclown.idea.p4ic.server.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.PasswordManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Does not look at the P4CONFIG.  Uses a password for authentication.
 */
public class ClientPasswordConnectionHandler extends ConnectionHandler {
    private static final Logger LOG = Logger.getInstance(ClientPasswordConnectionHandler.class);

    public static ClientPasswordConnectionHandler INSTANCE = new ClientPasswordConnectionHandler();

    ClientPasswordConnectionHandler() {
        // stateless utility class
    }

    @Override
    public Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName) {
        Properties ret = initializeConnectionProperties(config);
        ret.setProperty(PropertyDefs.USER_NAME_KEY, config.getUsername());
        if (clientName != null) {
            ret.setProperty(PropertyDefs.CLIENT_NAME_KEY, clientName);
        }

        // We will explicitly log in when ready.
        // This key is set to null to mean false.  Note that any other
        // value means true (even the text "false").
        ret.remove(PropertyDefs.AUTO_LOGIN_KEY);

        // Turning this flag on means that the server connection
        // will attempt to get the client object without having
        // logged in first, so we want it turned off.
        // This key is set to null to mean false.  Note that any other
        // value means true (even the text "false").
        ret.remove(PropertyDefs.AUTO_CONNECT_KEY);


        // This property key doesn't actually seem to do anything.
        // A real login is still required.
        // ret.setProperty(PropertyDefs.PASSWORD_KEY, new String(password));

        return ret;
    }

    @Override
    public void defaultAuthentication(@Nullable Project project, @NotNull IOptionsServer server, @NotNull ServerConfig config)
            throws P4JavaException {
        final AccessException ex = authenticate(project, server, config, false);
        if (ex != null) {
            throw ex;
        }
    }


    @Override
    public boolean forcedAuthentication(@Nullable Project project, @NotNull IOptionsServer server,
            @NotNull ServerConfig config, @NotNull AlertManager alerts) throws P4JavaException {
        final AccessException ex = authenticate(project, server, config, true);
        if (ex != null) {
            LOG.info(ex);
        }
        return ex == null;
    }

    @NotNull
    @Override
    public List<ConfigurationProblem> getConfigProblems(@NotNull final ServerConfig config) {
        // This config only uses the fields that are required in the
        // server config.  No additional checks are needed.
        return Collections.emptyList();
    }


    @Nullable
    private AccessException authenticate(@Nullable Project project, @NotNull final IOptionsServer server,
            @NotNull ServerConfig config, boolean force)
            throws P4JavaException {
        // Default login - use the simple password
        final OneUseString password;
        try {
            password = PasswordManager.getInstance().getPassword(project, config, force);
        } catch (PasswordAccessedWrongException ex) {
            // The current thread is in a wrong state, which prevents
            // the password from being accessed.  Do not forget the password.
            LOG.debug("can't access password yet", ex);
            throw ex;
        }

        try {
            password.use(new OneUseString.WithStringThrows<Void, P4JavaException>() {
                @Override
                public Void with(@Nullable char[] value) throws P4JavaException {
                    // If the password is blank, then there's no need for the
                    // user to log in; in fact, that wil raise an error by Perforce
                    if (value != null && value.length > 0) {
                        server.login(new String(value));
                        LOG.debug("No issue logging in with stored password");
                    }
                    return null;
                }
            });
            return null;
        } catch (AccessException ex) {
            // This seems to happen on a rare occasion on the
            // first attempt at logging in.  Don't automatically
            // forget the password.
            if (LOG.isDebugEnabled()) {
                LOG.debug("login failed", ex);
            }
            if (ex.getServerMessage().hasMessageFragment("'login' not necessary")) {
                // ignore login and keep going
                PasswordManager.getInstance().forgetPassword(project, config);
                LOG.info(config + ": User provided password, but it is not necessary", ex);
                return null;
            }
            if (force) {
                // Under a forced circumstance, forget the
                // password.
                PasswordManager.getInstance().forgetPassword(project, config);
                LOG.info("Stored password was bad; forgetting it", ex);
                return ex;
            }
            LOG.info("P4 server reported bad password, but this can be a timing issue.  Allowing it on the first pass", ex);
            return null;
        }
    }
}
