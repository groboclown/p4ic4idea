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
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import net.groboclown.idea.p4ic.server.exceptions.LoginRequiresPasswordException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.PasswordManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Does not look at the P4CONFIG.  Uses a password for authentication.
 */
public class ClientPasswordConnectionHandler extends ConnectionHandler {
    private static final Logger LOG = Logger.getInstance(ClientPasswordConnectionHandler.class);

    public static ClientPasswordConnectionHandler INSTANCE = new ClientPasswordConnectionHandler();

    // TODO this is a poor work-around for bugs #105 and #107
    // Essentially, a loop starts occurring where the initial
    // login attempt triggers a false authentication request.
    // This work-around keeps track of the servers that have
    // already had their authentication called (in a weak
    // map, to hopefully keep the memory leaks low), and will
    // make an attempt to validate whether it really is
    // in need of authentication or not.

    // There is a gap in functionality here, when a
    // user who isn't authorized to act upon a part of the
    // registry gets a correct unauthorized error, but the
    // check that we make ("getUser") passes without issue.
    // That could be alleviated, possibly, by keeping a record
    // of the number of invalid requests.
    private Map<IOptionsServer, Throwable> authenticatedServers = new WeakHashMap<IOptionsServer, Throwable>();

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
    private AccessException authenticate(@Nullable Project project, @NotNull IOptionsServer server,
            @NotNull ServerConfig config, boolean force)
            throws P4JavaException {
        if (authenticatedServers.containsKey(server))
        {
            LOG.info(authenticatedServers.get(server));
            AccessException ex = testLogin(server, config.getUsername());
            if (ex == null)
            {
                LOG.warn("Already authenticated server correctly!");
                return null;
            }
            LOG.warn("login attempts have been wrong; try again", ex);
        }

        // Default login - use the simple password
        final String password;
        try {
            password = PasswordManager.getInstance().getPassword(project, config, force);
        } catch (PasswordAccessedWrongException ex) {
            // The current thread is in a wrong state, which prevents
            // the password from being accessed.  Do not forget the password.
            LOG.debug("can't access password yet", ex);
            throw ex;
        }

        if (password != null) {
            // If the password is blank, then there's no need for the
            // user to log in; in fact, that wil raise an error by Perforce
            try {
                server.login(password, new LoginOptions(false, true));
                LOG.debug("No issue logging in with stored password");
                return testLogin(server, config.getUsername());
            } catch (AccessException ex) {
                LOG.info("Stored password was bad; forgetting it", ex);
                PasswordManager.getInstance().forgetPassword(project, config);
                if (ex.getServerMessage().hasMessageFragment("'login' not necessary")) {
                    // ignore login and keep going
                    LOG.info(config + ": User provided password, but it is not necessary", ex);
                    return null;
                } else {
                    return ex;
                }
            }
        } else {
            AccessException ex = testLogin(server, config.getUsername());
            if (ex != null)
            {
                LOG.info("Stored password was bad; forgetting it", ex);
                PasswordManager.getInstance().forgetPassword(project, config);
                return new LoginRequiresPasswordException(ex);
            }
            return null;
        }
    }


    private AccessException testLogin(final IOptionsServer server, final String username)
            throws ConnectionException, RequestException {
        try {
            // Perform an operation that should succeed, and only fail if the
            // login is wrong.
            server.getUser(username);
            if (!authenticatedServers.containsKey(server)) {
                authenticatedServers.put(server, new Throwable());
            }
            return null;
        } catch (AccessException ex) {
            authenticatedServers.remove(server);
            return ex;
        }

    }

}
