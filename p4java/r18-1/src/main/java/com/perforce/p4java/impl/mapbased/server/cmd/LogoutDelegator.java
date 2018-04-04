package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ILogoutDelegator;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.LOGOUT;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Handles the 'p4 logout' command.
 */
public class LogoutDelegator extends BaseDelegator implements ILogoutDelegator {

    /**
     * Instantiates a new logout delegator.
     *
     * @param server
     *            the server
     */
    public LogoutDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public void logout()
            throws ConnectionException, RequestException, AccessException, ConfigException {
        try {
            logout(new LoginOptions());
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public void logout(final LoginOptions opts) throws P4JavaException {
        if (isBlank(server.getAuthTicket())) {
            // We're not logged in. Should probably make this an error, but
            // never mind...
            return;
        }

        execMapCmdList(LOGOUT, processParameters(opts, server), null);

        // We basically don't really care about the results (any errors have already been
        // thrown up the exception ladder); we just need to null out the ticket:
        server.setAuthTicket(null);
    }
}
