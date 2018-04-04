package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.LoginOptions;

/**
 * Inteface for 'p4 logout'.
 */
public interface ILogoutDelegator {

    /**
     * Log the current Perforce user out of a Perforce server session.
     * <p>
     *
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     * @throws ConfigException
     *             if the p4tickets file could not be updated successfully
     */
    void logout() throws ConnectionException, RequestException, AccessException, ConfigException;

    /**
     * Log the current Perforce user out of a Perforce server session.
     *
     * @param opts
     *            currently ignored; can be null.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    void logout(LoginOptions opts) throws P4JavaException;
}
