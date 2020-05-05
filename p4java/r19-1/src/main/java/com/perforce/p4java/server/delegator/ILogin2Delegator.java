package com.perforce.p4java.server.delegator;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.Login2Options;

import java.util.List;
import java.util.Map;

public interface ILogin2Delegator {

	/**
	 * Generic access method for Login2, to bypass the non-interactive clients methods.
	 *
	 * @param opts
	 *             Login2Options
	 * @param user
	 *             non-null Perforce user; login request is for this specified
	 *             user, requires 'super' permission.
	 *
	 * @return     A list of result map objects
	 *
	 * @throws P4JavaException
	 *             if any errors occur during the processing of this command.
	 */
	List<Map<String, Object>> login2(Login2Options opts, String user) throws P4JavaException;

	/**
	 * Return a string indicating the current 2fa login status; corresponds to the
	 * 'p4 login2 -s' command. The resulting string should be interpreted by the
	 * caller, but is typically something like:
	 *
	 *             User bob1 on host 127.0.0.1: validated
	 *
	 * @return     non-null, but possibly-empty.  Interpretation of this string is up to the caller.
	 *
	 * @throws P4JavaException
	 *             if any errors occur during the processing of this command.
	 */
	String getLogin2Status() throws P4JavaException;

	/**
	 * Return a string indicating the current 2fa login status; corresponds to the
	 * 'p4 login2 -s' command. The resulting string should be interpreted by the
	 * caller, but is typically something like:
	 *
	 *             User bob1 on host 127.0.0.1: validated
	 *
	 * @param user
	 *             Specifying a username requires 'super' access, which is granted by 'p4 protect'.
	 *
	 * @return     non-null, but possibly-empty.  Interpretation of this string is up to the caller.
	 *
	 * @throws P4JavaException
	 *             if any errors occur during the processing of this command.
	 */
	String getLogin2Status(IUser user) throws P4JavaException;

	/**
	 * For non-interactive clients.
	 *
	 * The first stage 'list-methods'; will report the list of available second factor authentication
	 * methods for the given user.
	 *
	 * @return     A key value pair of second factor method IDs and their descriptions
	 *
	 * @throws P4JavaException
	 *             if any errors occur during the processing of this command.
	 */
	Map<String, String> login2ListMethods() throws P4JavaException;

	/**
	 * For non-interactive clients.
	 *
	 * The second stage 'init-auth' must specify a method (-m flag) to initiate the authentication
	 * with the second factor authentication provider.
	 *
	 * @param method
	 *             Second factor authentication method, chosen from the list provided by 'list-methods'.
	 *
	 * @return     Prompt from the second factor authentication method (e.g. a key, password, answer, etc...)
	 *
	 * @throws P4JavaException
	 *              if any errors occur during the processing of this command.
	 */
	String login2InitAuth(String method) throws P4JavaException;

	/**
	 * For non-interactive clients.
	 *
	 * The final step is 'check-auth', which will either prompt for a OTP or request the authorization
	 * status from the second factor authentication provider, depending on the type of authentication
	 * method selected.  The -p flag may be provided at the 'init-auth' stage.
	 *
	 * If a host and/or user is being specified, the appropriate arguments must be provided at each stage.
	 *
	 * @param auth
	 *             The answer to the second factor authorization prompt.
	 * @param persist
	 *             Persist the second factor authorization even	after the user's ticket has expired.
	 * @return
	 *             Verification message from second factor authorization.
	 *
	 * @throws P4JavaException
	 *             if any errors occur during the processing of this command.
	 */
	String login2CheckAuth(String auth, boolean persist) throws P4JavaException;

	/**
	 * Validate user second factor authentication.
	 *
	 * Specifying a username as an argument to 'p4 login2' requires 'super' access, which is granted
	 * by 'p4 protect'.  In this case, 'p4 login2' skips the second factor authentication process and
	 * immediately marks the user as validated for the current host.
	 *
	 * @param user
	 *             Specifying a username requires 'super' access, which is granted by 'p4 protect'.
	 * @param opts
	 *             Login2Options
	 * @return
	 *             Verification message from second factor authorization.
	 *
	 * @throws P4JavaException
	 *             if any errors occur during the processing of this command.
	 */
	String login2(IUser user, Login2Options opts) throws P4JavaException;
}
