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

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.passwordSafe.ui.PasswordSafePromptDialog;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.ui.alerts.PasswordRequiredHandler;
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

        // This property key doesn't actually seem to do anything.
        // A real login is still required.
        // ret.setProperty(PropertyDefs.PASSWORD_KEY, new String(password));

        return ret;
    }

    @Override
    public void defaultAuthentication(@Nullable Project project, @NotNull IOptionsServer server, @NotNull ServerConfig config)
            throws P4JavaException {
        // Default login - use the user provided password from the config
        String password = config.getPlaintextPassword();

        if (password != null && password.length() > 0) {
            // If the password is blank, then there's no need for the
            // user to log in; in fact, that wil raise an error by Perforce
            try {
                server.login(password, new LoginOptions(false, true));
            } catch (AccessException ex) {
                if (ex.getServerMessage().hasMessageFragment("'login' not necessary")) {
                    // ignore login and keep going
                    LOG.info(config + ": User provided password, but  it is not necessary", ex);
                    // TODO tell the caller that the password should be forgotten
                } else {
                    throw ex;
                }
            }
        }
    }

    @Override
    public boolean forcedAuthentication(@Nullable Project project, @NotNull IOptionsServer server,
            @NotNull ServerConfig config, @NotNull AlertManager alerts) throws P4JavaException {
        try {
            LOG.debug("Asking PasswordSafe for the password");
            String password = PasswordSafe.getInstance().getPassword(project,
                    P4Vcs.class, config.getServiceName());
            if (password != null && password.length() > 0) {
                // If the password is blank, then there's no need for the
                // user to log in; in fact, that wil raise an error by Perforce
                try {
                    server.login(password, new LoginOptions(false, true));
                    LOG.debug("No issue logging in with stored password");
                } catch (AccessException e) {
                    LOG.debug("Stored password was bad; forgetting it");
                    PasswordSafe.getInstance().removePassword(project,
                            P4Vcs.class, config.getServiceName());
                    throw e;
                }
                return true;
            } else {
                LOG.debug("No password found");
                alerts.addCriticalError(new PasswordRequiredHandler(project, config), null);
            }
        } catch (PasswordSafeException e) {
            LOG.debug("PasswordSafe access caused an error", e);
            // FIXME have a better exception
            throw new ConnectionException(e);
        }
        return false;
    }

    @NotNull
    @Override
    public List<ConfigurationProblem> getConfigProblems(@NotNull final ServerConfig config) {
        // This config only uses the fields that are required in the
        // server config.  No additional checks are needed.
        return Collections.emptyList();
    }


    /**
     * Not really the best place for this method, but it centralizes the PasswordSafe
     * access.
     *
     * @return true if the user entered a password
     */
    public static boolean askPassword(@Nullable Project project, @NotNull final ServerConfig config) {
        return PasswordSafePromptDialog.askPassword(project, ModalityState.any(),
                P4Bundle.message("login.password.title"),
                P4Bundle.message("login.password.message", config.getServiceName(), config.getUsername()),
                P4Vcs.class, config.getServiceName(),
                true,
                P4Bundle.message("login.password.error")
                ) != null;
    }

}
