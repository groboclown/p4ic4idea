package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.LoginOptions;

import javax.annotation.Nonnull;

/**
 * Interface for 'p4 login'.
 */
public interface ILoginDelegator {
    /**
     * Return a string indicating the current login status; corresponds to the
     * p4 login -s command. The resulting string should be interpreted by the
     * caller, but is typically something like "User p4jtestsuper ticket expires
     * in 9 hours 42 minutes." or "'login' not necessary, no password set for
     * this user." or "Perforce password (P4PASSWD) invalid or unset." or
     * "Access for user 'p4jtestinvaliduser' has not been enabled by 'p4
     * protect'", etc.
     *
     * @return non-null, but possibly-empty ticket / login status string.
     *         Interpretation of this string is up to the caller.
     * @throws P4JavaException
     *             if any errors occur during the processing of this command.
     */
    String getLoginStatus() throws P4JavaException;

    /**
     * Convenience method for login(password, false).
     *
     * @param password
     *            Perforce password; can be null if no password is needed (as in
     *            the case of SSO logins)
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
    void login(String password)
            throws ConnectionException, RequestException, AccessException, ConfigException;

    /**
     * Log the current user (if any) in to a Perforce server, optionally
     * arranging to be logged in for all hosts.
     * <p>
     *
     * Attempts to log in to the underlying Perforce server. If successful,
     * successive calls to server-side services will succeed until the session
     * is terminated by the server or the user logs out.
     * <p>
     *
     * Behaviour is undefined if the server's user name attribute is null (but
     * will probably cause a NullPointerError with most implementations).
     * <p>
     *
     * Login will work with the Perforce SSO (single sign-on) scheme: in this
     * case your password should be null, and the environment variable
     * P4LOGINSSO should point to an executable SSO script as described in p4
     * help undoc (help for this is beyond the scope of this method doc,
     * unfortunately, and the feature is not well tested here, but it "works" in
     * general...).
     *
     * @param password
     *            Perforce password; can be null if no password is needed (as in
     *            the case of SSO logins)
     * @param allHosts
     *            if true, perform the equivalent of a "login -a"
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
    void login(String password, boolean allHosts)
            throws ConnectionException, RequestException, AccessException, ConfigException;

    /**
     * Log the current user (if any) in to a Perforce server, optionally
     * arranging to be logged in for all hosts.
     * <p>
     *
     * Attempts to log in to the underlying Perforce server. If successful,
     * successive calls to server-side services will succeed until the session
     * is terminated by the server or the user logs out.
     * <p>
     *
     * Behavior is undefined if the server's user name attribute is null (but
     * will probably cause a NullPointerError with most implementations).
     * <p>
     *
     * Login will work with the Perforce SSO (single sign-on) scheme: in this
     * case your password should be null, and the environment variable
     * P4LOGINSSO should point to an executable SSO script as described in p4
     * help undoc (help for this is beyond the scope of this method doc,
     * unfortunately, and the feature is not well tested here, but it "works" in
     * general...).
     *
     * @param password
     *            Perforce password; can be null if no password is needed (as in
     *            the case of SSO logins)
     * @param opts
     *            if LoginOptions.allHosts is true, perform the equivalent of a
     *            "login -a". A null LoginOptions parameter is equivalent to no
     *            options being set.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method. A
     *             specific ConfigException is thrown if the p4tickets file
     *             could not be updated successfully.
     */
    void login(String password, LoginOptions opts) throws P4JavaException;

    /**
     * Log the current user (if any) in to a Perforce server using. If the
     * ticket StringBuffer parameter is non-null, the auth ticket returned from
     * the server will be appended to the passed-in ticket StringBuffer.
     * <p>
     *
     * Optionally, if the opts.isDontWriteTicket() is true ('login -p'), the
     * ticket is not written to file; if opts.isAllHosts is true ('login -a'),
     * the ticket is valid on all hosts; if opts.getHost() is non-null ('login
     * -h'), the ticket is valid on the specified host.
     * <p>
     *
     * Note: if the passed-in ticket StringBuffer originally has content it will
     * remain there. The auth ticket will only be appended to the buffer. If a
     * null ticket StringBuffer is passed in, the auth ticket will not be
     * appended to it. The normal use case should be to pass in a new ticket
     * StringBuffer.
     *
     * @param password
     *            Perforce actuallPassword; can be null if no actuallPassword is
     *            needed (as in the case of SSO logins)
     * @param ticket
     *            if the ticket StringBuffer parameter is non-null, the auth
     *            ticket that was returned by the login attempt is appended to
     *            the passed-in ticket StringBuffer.
     * @param opts
     *            LoginOptions describing the associated options; if null, no
     *            options are set.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.2
     */
    void login(String password, StringBuffer ticket, LoginOptions opts)
            throws P4JavaException;

    /**
     * Log another user in to Perforce by obtaining a session ticket for that
     * user. If the ticket StringBuffer parameter is non-null, the auth ticket
     * returned from the server will be appended to the passed-in ticket
     * StringBuffer.
     * <p>
     *
     * Optionally, if the opts.isDontWriteTicket() is true ('login -p'), the
     * ticket is not written to file; if opts.isAllHosts is true ('login -a'),
     * the ticket is valid on all hosts; if opts.getHost() is non-null ('login
     * -h'), the ticket is valid on the specified host.
     * <p>
     *
     * Specifying a user as an argument requires 'super' access, which is
     * granted by 'p4 protect'. In this case, login another user does not
     * require a password, assuming that you (a 'super' user) had already been
     * logged in.
     * <p>
     *
     * Note: if the passed-in ticket StringBuffer originally has content it will
     * remain there. The auth ticket will only be appended to the buffer. If a
     * null ticket StringBuffer is passed in, the auth ticket will not be
     * appended to it. The normal use case should be to pass in a new ticket
     * StringBuffer.
     *
     * @param user
     *            non-null Perforce user; login request is for this specified
     *            user.
     * @param ticket
     *            if the ticket StringBuffer parameter is non-null, the auth
     *            ticket that was returned by the login attempt is appended to
     *            the passed-in ticket StringBuffer.
     * @param opts
     *            LoginOptions describing the associated options; if null, no
     *            options are set.
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.2
     */
    void login(@Nonnull IUser user, StringBuffer ticket, LoginOptions opts)
            throws P4JavaException;
    
    /**
     * Special case handling of the "-p" flag for the "p4 login" command. The -p
     * flag displays the ticket, but does not store it on the client machine.
     *
     * @param cmd
     *            the cmd
     * @param cmdArgs
     *            the cmd args
     * @return true, if is dont write ticket
     */
    boolean isDontWriteTicket(String cmd, String[] cmdArgs);
}
