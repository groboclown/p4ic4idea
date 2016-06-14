/**
 * 
 */
package com.perforce.p4java.server;

import com.perforce.p4java.admin.*;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.*;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.server.*;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * An extension of the basic IServer interface to provide Options
 * object-based method access to Perforce server functionality and
 * objects.<p>
 * 
 * Note that unless otherwise noted, individual method options objects
 * can be null; if they're null, the individual method Javadoc will spell
 * out what default options apply (if any) in that case.<p>
 * 
 * Note that in individual method Javadoc comments below, all method
 * "throws" clauses are assumed to throw the normal complement of
 * RequestException, ConnectionException, and AccessException with
 * their usual semantics unless otherwise noted. The three standard
 * P4JavaException classes and the broad causes for their being thrown
 * are:
 * <pre>
 * ConnectionException if the Perforce server is unreachable or is not
 * 				connected.
 * RequestException if the Perforce server encounters an error during
 * 				its processing of the request.
 * AccessException if the Perforce server denies access to the caller.
 * </pre>
 */

public interface IOptionsServer extends IServer {
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a list of maps.<p>
	 * 
	 * This method is intended for low-level commands in the spirit and format of the p4 command
	 * line interpreter, and offers a simple way to issue commands to the associated Perforce server
	 * without the overhead of the more abstract Java interfaces and methods.<p>
	 * 
	 * No guidance is given here on the format of the returned map; however, it produces the same
	 * output as the p4 command line interpreter in -G (Python map) mode.<p>
	 * 
	 * Note that this method does not allow you to set "usage" options for the command;
	 * these may be added later. Note also that although option arguments passed to this
	 * method must be in a form recognized by the p4 command line interpreter, that does
	 * <i>not</i> mean the method is being implemented by the interpreter -- the actual
	 * implementation depends on the options used to get the server object in the first
	 * place from the server factory.
	 * 
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inMap an optional map to be sent to the server as standard input, using the
	 * 				Python map format (-G) form. You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @return a non-null Java List of results; these results are as returned from issuing the command
	 * 				using the -G option with the p4 command line interpreter.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
									throws P4JavaException;
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a list of maps.<p>
	 * 
	 * This method is intended for low-level commands in the spirit and format of the p4 command
	 * line interpreter, and offers a simple way to issue commands to the associated Perforce server
	 * without the overhead of the more abstract Java interfaces and methods.<p>
	 * 
	 * No guidance is given here on the format of the returned map; however, it produces the same
	 * output as the p4 command line interpreter in -G (Python map) mode.<p>
	 * 
	 * Note that this method does not allow you to set "usage" options for the command;
	 * these may be added later. Note also that although option arguments passed to this
	 * method must be in a form recognized by the p4 command line interpreter, that does
	 * <i>not</i> mean the method is being implemented by the interpreter -- the actual
	 * implementation depends on the options used to get the server object in the first
	 * place from the server factory.
	 * 
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inMap an optional map to be sent to the server as standard input, using the
	 * 				Python map format (-G) form. You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @param filterCallback an optional filter callback to decide on skipping or keeping individual
	 * 				key/value pairs as part of the results map.
	 * @return a non-null Java List of results; these results are as returned from issuing the command
	 * 				using the -G option with the p4 command line interpreter.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs, Map<String, Object> inMap,
									IFilterCallback filterCallback) throws P4JavaException;

	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a list of maps.<p>
	 * 
	 * This method is intended for low-level commands in the spirit and format of the p4 command
	 * line interpreter, and offers a simple way to issue commands to the associated Perforce server
	 * without the overhead of the more abstract Java interfaces and methods.<p>
	 * 
	 * No guidance is given here on the format of the returned map; however, it produces the same
	 * output as the p4 command line interpreter in -G (Python map) mode.<p>
	 * 
	 * Note that this method does not allow you to set "usage" options for the command;
	 * these may be added later. Note also that although option arguments passed to this
	 * method must be in a form recognized by the p4 command line interpreter, that does
	 * <i>not</i> mean the method is being implemented by the interpreter -- the actual
	 * implementation depends on the options used to get the server object in the first
	 * place from the server factory.
	 * 
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inString an optional string to be sent to the server as standard input unchanged
	 * 				(this must be in the format expected by the server, typically as required
	 * 				when using the "-i" flag to the p4 command line app for the same command).
	 * 				You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @return a non-null Java Map of results; these results are as returned from issuing the command
	 * 				using the -G option with the p4 command line interpreter.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */
	List<Map<String, Object>> execInputStringMapCmdList(String cmdName, String[] cmdArgs, String inString)
									throws P4JavaException;
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a list of maps.<p>
	 * 
	 * This method is intended for low-level commands in the spirit and format of the p4 command
	 * line interpreter, and offers a simple way to issue commands to the associated Perforce server
	 * without the overhead of the more abstract Java interfaces and methods.<p>
	 * 
	 * No guidance is given here on the format of the returned map; however, it produces the same
	 * output as the p4 command line interpreter in -G (Python map) mode.<p>
	 * 
	 * Note that this method does not allow you to set "usage" options for the command;
	 * these may be added later. Note also that although option arguments passed to this
	 * method must be in a form recognized by the p4 command line interpreter, that does
	 * <i>not</i> mean the method is being implemented by the interpreter -- the actual
	 * implementation depends on the options used to get the server object in the first
	 * place from the server factory.
	 * 
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inString an optional string to be sent to the server as standard input unchanged
	 * 				(this must be in the format expected by the server, typically as required
	 * 				when using the "-i" flag to the p4 command line app for the same command).
	 * 				You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @param filterCallback an optional filter callback to decide on skipping or keeping individual
	 * 				key/value pairs as part of the results map.
	 * @return a non-null Java Map of results; these results are as returned from issuing the command
	 * 				using the -G option with the p4 command line interpreter.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */
	List<Map<String, Object>> execInputStringMapCmdList(String cmdName, String[] cmdArgs, String inString,
											IFilterCallback filterCallback) throws P4JavaException;

	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a list of maps
	 * without invoking any command callbacks.<p>
	 * 
	 * Basically equivalent to execMapCmd with temporary disabling of any ICommandCallback
	 * calls and / or listeners; this turns out to be useful for various reasons we won't go
	 * into here...<p>
	 *
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inMap an optional map to be sent to the server as standard input, using the
	 * 				Python map format (-G) form. You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @return a non-null Java Map of results; these results are as returned from issuing the command
	 * 				using the -G option with the p4 command line interpreter.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */
	List<Map<String, Object>> execQuietMapCmdList(String cmdName, String[] cmdArgs, Map<String,
									Object> inMap) throws P4JavaException;
	
	/**
	 * Issue a streaming map command to the Perforce server, using an optional string
	 * for any input expected by the server (such as label or job specs, etc.).<p>
	 * 
	 * Streaming commands allow users to get each result from a suitably-issued
	 * command as it comes in from the server, rather than waiting for the entire
	 * command method to complete (and getting the results back as a completed
	 * List or Map or whatever).<p>
	 * 
	 * The results are sent to the user using the IStreamingCallback handleResult
	 * method; see the IStreamingCallback Javadoc for details. The payload passed
	 * to handleResult is usually the raw map gathered together deep in the RPC
	 * protocol layer, and the user is assumed to have the knowledge and technology
	 * to be able to parse it and use it suitably in much the same way as a user
	 * unpacks or processes the results from the other low-level exec methods
	 * like execMapCommand.<p>
	 * 
	 * NOTE: 'streaming' here has nothing at all to do with Perforce 'streams', which
	 * are (or will be) implemented elsewhere.
	 * 
	 * @since 2013.1
 	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inString an optional string to be sent to the server as standard input unchanged
	 * 				(this must be in the format expected by the server, typically as required
	 * 				when using the "-i" flag to the p4 command line app for the same command).
	 * 				You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @param callback a non-null IStreamingCallback to be used to process the incoming
	 * 				results.
	 * @param key an opaque integer key that is passed to the IStreamingCallback callback
	 * 				methods to identify the action as being associated with this specific
	 * 				call.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
 	 */
	void execInputStringStreamingMapCmd(String cmdName, String[] cmdArgs, String inString,
											IStreamingCallback callback, int key) throws P4JavaException;

	/**
	 * Get the UsageOptions object associated with this server.
	 * 
	 * @return possibly-null UsageOptions object.
	 */
	UsageOptions getUsageOptions();
	
	/**
	 * Set the UsageOptions object associated with this server. Note
	 * that changing this object (or its contents) while a server is busy
	 * can cause issues.
	 * 
	 * @param opts non-null UsageOptions object to associate with this server.
	 * @return the current server.
	 */
	IOptionsServer setUsageOptions(UsageOptions opts);

	/**
	 * Set the server's Perforce authentication ticket for the specified user to
	 * the passed-in string.<p>
	 * 
	 * @since 2011.2
	 * @param userName non-null Perforce user name
	 * @param authTicket possibly-null Perforce authentication ticket
	 */
	void setAuthTicket(String userName, String authTicket);
	
	/**
	 * Return the Perforce authentication ticket for specified user.
	 * 
	 * @since 2011.2
	 * @param userName non-null Perforce user name
	 * @return possibly-null Perforce authentication ticket
	 */
	String getAuthTicket(String userName);

	/**
	 * Set the Perforce authentication tickets file path.
	 * 
	 * @since 2013.1
	 * @param ticketsFilePath non-null Perforce auth tickets file path
	 */
	void setTicketsFilePath(String ticketsFilePath);
	
	/**
	 * Return the Perforce authentication tickets file path.
	 * 
	 * @since 2013.1
	 * @return possibly-null Perforce auth tickets file path
	 */
	String getTicketsFilePath();

	/**
	 * Set the Perforce trust file path.
	 * 
	 * @since 2013.1
	 * @param trustFilePath non-null Perforce trust file path
	 */
	void setTrustFilePath(String trustFilePath);
	
	/**
	 * Return the Perforce trust file path.
	 * 
	 * @since 2013.1
	 * @return possibly-null Perforce trust file path
	 */
	String getTrustFilePath();

	/**
	 * Log the current user (if any) in to a Perforce server, optionally
	 * arranging to be logged in for all hosts.<p>
	 * 
	 * Attempts to log in to the underlying Perforce server. If successful,
	 * successive calls to server-side services will succeed until the session
	 * is terminated by the server or the user logs out.<p>
	 * 
	 * Behavior is undefined if the server's user name attribute is null (but
	 * will probably cause a NullPointerError with most implementations).<p>
	 * 
	 * Login will work with the Perforce SSO (single sign-on) scheme: in this
	 * case your password should be null, and the environment variable P4LOGINSSO
	 * should point to an executable SSO script as described in p4 help undoc (help
	 * for this is beyond the scope of this method doc, unfortunately, and the feature
	 * is not well tested here, but it "works" in general...).
	 * 
	 * @param password Perforce password; can be null if no password is needed (as
	 * 				in the case of SSO logins)
	 * @param opts if LoginOptions.allHosts is true, perform the equivalent
	 * 				of a "login -a". A null LoginOptions parameter is equivalent
	 * 				to no options being set.
	 * @throws P4JavaException if any error occurs in the processing of this
	 * 			method. A specific ConfigException is thrown if the p4tickets
	 * 			file could not be updated successfully.
	 */
	
	void login(String password, LoginOptions opts) throws P4JavaException;
	
	/**
	 * Log the current user (if any) in to a Perforce server using. If the ticket
	 * StringBuffer parameter is non-null, the auth ticket returned from the server
	 * will be appended to the passed-in ticket StringBuffer.<p>
	 * 
	 * Optionally, if the opts.isDontWriteTicket() is true ('login -p'), the ticket
	 * is not written to file; if opts.isAllHosts is true ('login -a'), the ticket
	 * is valid on all hosts; if opts.getHost() is non-null ('login -h'), the ticket
	 * is valid on the specified host.<p>
	 * 
	 * Note: if the passed-in ticket StringBuffer originally has content it will
	 * remain there. The auth ticket will only be appended to the buffer. If a
	 * null ticket StringBuffer is passed in, the auth ticket will not be appended
	 * to it. The normal use case should be to pass in a new ticket StringBuffer.
	 * 
	 * @since 2011.2
	 * @param password Perforce password; can be null if no password is needed (as
	 * 				in the case of SSO logins)
	 * @param ticket if the ticket StringBuffer parameter is non-null, the auth
	 * 				ticket that was returned by the login attempt is appended to
	 * 				the passed-in ticket StringBuffer.
	 * @param opts LoginOptions describing the associated options; if null,	no
	 * 				options are set.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	void login(String password, StringBuffer ticket, LoginOptions opts) throws P4JavaException;

	/**
	 * Log another user in to Perforce by obtaining a session ticket for that user.
	 * If the ticket StringBuffer parameter is non-null, the auth ticket returned
	 * from the server will be appended to the passed-in ticket StringBuffer.<p>
	 * 
	 * Optionally, if the opts.isDontWriteTicket() is true ('login -p'), the ticket
	 * is not written to file; if opts.isAllHosts is true ('login -a'), the ticket
	 * is valid on all hosts; if opts.getHost() is non-null ('login -h'), the ticket
	 * is valid on the specified host.<p>
	 * 
	 * Specifying a user as an argument requires 'super' access, which is granted
	 * by 'p4 protect'. In this case, login another user does not require a password,
	 * assuming that you (a 'super' user) had already been logged in.<p>
	 * 
	 * Note: if the passed-in ticket StringBuffer originally has content it will
	 * remain there. The auth ticket will only be appended to the buffer. If a
	 * null ticket StringBuffer is passed in, the auth ticket will not be appended
	 * to it. The normal use case should be to pass in a new ticket StringBuffer.
	 * 
	 * @since 2011.2
	 * @param user non-null Perforce user; login request is for this specified user.
	 * @param ticket if the ticket StringBuffer parameter is non-null, the auth
	 * 				ticket that was returned by the login attempt is appended to
	 * 				the passed-in ticket StringBuffer.
	 * @param opts LoginOptions describing the associated options; if null,	no
	 * 				options are set.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	void login(IUser user, StringBuffer ticket, LoginOptions opts) throws P4JavaException;
	
	/**
	 * Log the current Perforce user out of a Perforce server session.
	 * 
	 * @param opts currently ignored; can be null.
	 * @throws P4JavaException if any error occurs in the processing of this
	 * 			method.
	 */
	
	void logout(LoginOptions opts) throws P4JavaException;
	
	/**
	 * Change a user's password on the server. After a password is changed for a
	 * user, the user must login again with the new password. Specifying a username
	 * as an argument to this command requires 'super' access granted by 'p4 protect'<p>
	 * 
	 * Note: setting the 'newPassword' to null or empty will delete the password.
	 * 
	 * @since 2011.2
	 * @param oldPassword possibly-null or possibly-empty user's old password.
	 * 				If null or empty, it assumes the current password is not set.
	 * @param newPassword non-null and non-empty user's new password.
	 * @param userName possibly-null possibly-null name of the target user whose
	 * 				password will be changed to the new password. If null, the
	 * 				current user will be used. If non-null, this command requires
	 * 				'super' access granted by 'p4 protect'.
	 * @throws P4JavaException if any error occurs in the processing of this
	 * 			method.
	 */
	String changePassword(String oldPassword, String newPassword, String userName) throws P4JavaException;

	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and get the results as a stream.<p>
	 * 
	 * This method is intended for low-level commands in the spirit and format of the p4 command
	 * line interpreter, and offers a simple way to issue commands to the associated Perforce server
	 * without the overhead of the more abstract Java interfaces and methods.<p>
	 * 
	 * Note that this method is intended for things like getting file contents, and may have
	 * unpredictable results on commands not originally expected to return i/o streams.<p>
	 * 
	 * Note that this method does not allow you to set "usage" options for the command;
	 * these may be added later. Note also that although option arguments passed to this
	 * method must be in a form recognized by P4Java (as defined by the CmdSpec enum), that does
	 * <i>not</i> mean the method is being implemented by the interpreter -- the actual
	 * implementation depends on the options used to get the server object in the first
	 * place from the server factory.
	 * 
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inMap an optional map to be sent to the server as standard input, using the
	 * 				Python map format (-G) form. You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @return an InputStream on the command output. This will never be null, but it may be empty.
	 * 				You <i>must</i> properly close this stream after use or temporary files may
	 * 				be left lying around the VM's java.io.tmpdir area.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	InputStream execStreamCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap) throws P4JavaException;
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a stream.<p>
	 * 
	 * This method is intended for low-level commands in the spirit and format of the p4 command
	 * line interpreter, and offers a simple way to issue commands to the associated Perforce server
	 * without the overhead of the more abstract Java interfaces and methods.<p>
	 * 
	 * Note that this method is intended for things like getting file contents, and may have
	 * unpredictable results on commands not originally expected to return i/o streams.<p>
	 * 
	 * Note that this method does not allow you to set "usage" options for the command;
	 * these may be added later. Note also that although option arguments passed to this
	 * method must be in a form recognized by the p4 command line interpreter, that does
	 * <i>not</i> mean the method is being implemented by the interpreter -- the actual
	 * implementation depends on the options used to get the server object in the first
	 * place from the server factory.
	 * 
	 * @since 2013.1
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inString an optional string to be sent to the server as standard input unchanged
	 * 				(this must be in the format expected by the server, typically as required
	 * 				when using the "-i" flag to the p4 command line app for the same command).
	 * 				You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @return an InputStream on the command output. This will never be null, but it may be empty.
	 * 				You <i>must</i> properly close this stream after use or temporary files may
	 * 				be left lying around the VM's java.io.tmpdir area.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */
	
	InputStream execInputStringStreamCmd(String cmdName, String[] cmdArgs, String inString)
					throws P4JavaException;

	/**
	 * Get an individual depot by name. Note that this method will return a
	 * fake depot if you ask it for a non-existent depot, so it's not the most
	 * useful of operations.
	 * 
	 * @since 2011.1
	 * @param name non-null name of the depot to be retrieved.
	 * @return IDepot non-null object corresponding to the named depot if it exists and is
	 * 			retrievable; otherwise an IDepot object that looks real but does not, in
	 * 			fact, correspond to any known depot in the repository.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	IDepot getDepot(String name) throws P4JavaException;
	
	/**
	 * Create a new depot in the repository. You must be an admin for this operation
	 * to succeed.
	 * 
	 * @since 2011.1
	 * @param newDepot non-null IDepot object representing the depot to be created.
	 * @return possibly-null operation result message string from the Perforce server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String createDepot(IDepot newDepot) throws P4JavaException;
	
	/**
	 * Delete a named depot from the repository. You must be an admin for this operation
	 * to succeed.
	 * 
	 * @since 2011.1
	 * @param name non-null IDepot object representing the depot to be deleted
	 * @return possibly-null operation result message string from the Perforce server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String deleteDepot(String name) throws P4JavaException;
	
	/**
	 * List all Perforce depot files known to the Perforce server that conform to the
	 * passed-in wild-card file specification(s).<p>
	 * 
	 * If client file names are given as file spec arguments the current Perforce client
	 * view mapping is used to list the corresponding depot files, if the client and
	 * view exist (if not, the results are undefined).<p>
	 * 
	 * Normally, the head revision of each matching file is listed, but you can change
	 * this by specifying specific revisions or revision ranges. If the file spec argument
	 * includes a revision, then all files as of that revision are returned.  If the file spec
	 * argument has a revision range, then only files selected by that revision range
	 * are returned, and the highest revision in the range is used for each file.
	 * If GetDepotFilesOptions.allRevs is true, all revisions within the specific range,
	 * rather than just the highest revision in the range, are returned.<p>
	 * 
	 * See 'p4 help revisions' for help specifying revisions.<p>
	 * 
	 * Note that the IFileSpec objects returned will have null client and local
	 * path components.
	 * 
	 * @param fileSpecs a non-null list of one or more IFileSpecs to be used
	 * 				to qualify Perforce depot files
	 * @param opts GetDepotFilesOptions describing the associated options; if null,
	 * 				no options are set.
	 * @return a non-null (but possible empty) list of all qualifying depot files
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileSpec> getDepotFiles(List<IFileSpec> fileSpecs, GetDepotFilesOptions opts)
											throws P4JavaException;
	
	/**
	 * Return a list of all Perforce jobs with fix records associated with them,
     * along with the changelist number of the fix. Detailed semantics for this
     * method are given in the main Perforce documentation for the p4 command "fixes".<p>
     * 
     * Note that this method (unlike the main file list methods) throws an exception
	 * and stops at the first encountered error.
	 * 
	 * @param fileSpecs if given, restrict output to fixes associated with these files
	 * @param opts FixListOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return non-null but possibly empty list of qualifying IFix fixes.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFix> getFixes(List<IFileSpec> fileSpecs, GetFixesOptions opts)
										throws P4JavaException;
	
	/**
	 * Get list of matching lines in the specified file specs. This method
	 * implements the p4 grep command; for full semantics, see the separate
	 * p4 documentation and / or the GrepOptions Javadoc.
	 * 
	 * @param fileSpecs file specs to search for matching lines
	 * @param pattern non-null string pattern to be passed to the grep command
	 * @param options - Options to grep command
	 * @return - non-null but possibly empty list of file line matches
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	List<IFileLineMatch> getMatchingLines(List<IFileSpec> fileSpecs, String pattern,
								MatchingLinesOptions options) throws P4JavaException;
	
	/**
	 * Get list of matching lines in the specified file specs. This method
	 * implements the p4 grep command; for full semantics, see the separate
	 * p4 documentation and / or the GrepOptions Javadoc.<p>
	 * 
	 * This method allows the user to retrieve useful info and warning message
	 * lines the Perforce server may generate in response to things like
	 * encountering a too-long line, etc., by passing in a non-null infoLines
	 * parameter.<p>
	 *
	 * p4ic4idea changed the {@code infoLines} to be a list of IServerMessage
	 * instead of String.
	 * 
	 * @since 2011.1
	 * @param fileSpecs file specs to search for matching lines
	 * @param pattern non-null string pattern to be passed to the grep command
	 * @param infoLines if not null, any "info" lines returned from the server
	 * 				(i.e. warnings about exceeded line lengths, etc.) will be put
	 * 				into the passed-in list in the order they are received.
	 * @param options - Options to grep command
	 * @return - non-null but possibly empty list of file line matches
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileLineMatch> getMatchingLines(List<IFileSpec> fileSpecs, String pattern,
				List<IServerMessage> infoLines, MatchingLinesOptions options) throws P4JavaException;
	
	/**
	 * Create a new Perforce user on the Perforce server.
	 * 
	 * @param user non-null IUser defining the new user to be created.
	 * @param opts UpdateUserOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String createUser(IUser user, UpdateUserOptions opts) throws P4JavaException;
	
	/**
	 * Update a Perforce user on the Perforce server.
	 * 
	 * @param user non-null IUser defining the new user to be updated.
	 * @param opts UpdateUserOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String updateUser(IUser user, UpdateUserOptions opts) throws P4JavaException;
	
	/**
	 * Delete a named Perforce user from the Perforce server
	 * 
	 * @param userName non-null name of the user to be deleted.
	 * @param opts UpdateUserOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String deleteUser(String userName, UpdateUserOptions opts) throws P4JavaException;
	
	/**
	 * Completely renames a user, modifying all database records which mention
	 * the user.<p>
	 * 
	 * This includes all workspaces, labels, branches, streams, etc. which are
	 * owned by the user, all pending, shelved, and committed changes created by
	 * the user, any files that the user has opened or shelved, any fixes that
	 * the user made to jobs, any properties that apply to the user, any groups
	 * that the user is in, and the user record itself.<p>
	 * 
	 * The username is not changed in descriptive text fields (such as job
	 * descriptions, change descriptions, or workspace descriptions), only where
	 * it appears as the owner or user field of the database record.<p>
	 * 
	 * Protection table entries that apply to the user are updated only if the
	 * Name: field exactly matches the user name; if the Name: field contains
	 * wildcards, it is not modified.<p>
	 * 
	 * The only job field that is processed is attribute code 103. If you have
	 * included the username in other job fields they will have to be processed
	 * separately.<p>
	 * 
	 * The full semantics of this operation are found in the main 'p4 help'
	 * documentation.<p>
	 * 
	 * This method requires 'super' access granted by 'p4 protect'.
	 * 
	 * @since 2014.1
	 * @param oldUserName the old user name to be changed.
	 * @param newUserName the new user name to be changed to.
	 * @return non-null result message string from the reload operation.
	 * @throws P4JavaException
	 *             if an error occurs processing this method and its parameters.
	 */
	String renameUser(String oldUserName, String newUserName) throws P4JavaException;

	/**
	 * Get a list of Perforce users known to this Perforce server. Note that
	 * maxUsers and the user list are supposed to be mutually exclusive in
	 * usage, but this is not enforced by P4Java as the restriction doesn't
	 * make much sense and may be lifted in the Perforce server later.<p>
	 * 
	 * Note that this implementation differs a bit from the p4 command line
	 * version in that it simply doesn't return any output for unmatched users.
	 * 
	 * @param userList if non-null, restrict output to users matching the passed-in
	 * 			list of users.
	 * @param opts GetUsersOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return non-null (but possibly empty) list of non-null IUserSummary
	 * 			objects representing the underlying Perforce users (if any).
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IUserSummary> getUsers(List<String> userList, GetUsersOptions opts)
							throws P4JavaException;
	
	/**
	 * Get a list of Perforce user groups from the server.<p>
	 * 
	 * Note that the Perforce server considers it an error to have both indirect and
	 * displayValues parameters set true; this will cause the server to throw a
	 * RequestException with an appropriate usage message.
	 * 
	 * @param userOrGroupName if non-null, restrict the list to the specified group or username.
	 * @param opts GetUserGroupsOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return a non-null but possibly-empty list of qualifying groups.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IUserGroup> getUserGroups(String userOrGroupName, GetUserGroupsOptions opts)
							throws P4JavaException;
	
	/**
	 * Create a new Perforce user group on the Perforce server.
	 * 
	 * @param group group non-null IUserGroup to be created.
	 * @param opts UpdateUserGroupOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return possibly-null status message string as returned from the server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String createUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
							throws P4JavaException;
	
	/**
	 * Update a Perforce user group on the Perforce server.
	 * 
	 * @param group group non-null IUserGroup to be updated.
	 * @param opts UpdateUserGroupOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return possibly-null status message string as returned from the server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String updateUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
							throws P4JavaException;
	
	/**
	 * Delete a Perforce user group from the Perforce server.
	 * 
	 * @param group non-null group to be deleted.
	 * @param opts UpdateUserGroupOptions object describing optional parameters; if null, no
	 * 				options are set
	 * @return possibly-null status message string as returned from the server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String deleteUserGroup(IUserGroup group, UpdateUserGroupOptions opts)
							throws P4JavaException;
	
	/**
	 * Get a list of Perforce protection entries for the passed-in arguments.<p>
	 * 
	 * Note that the behavior of this method is unspecified when using clashing
	 * options (e.g. having both userName and groupName set non-null). Consult the
	 * main Perforce admin documentation for semantics and usage.<p>
	 * 
	 * Note that any annotations in the file paths will be ignored. The reason is
	 * the Perforce server 'protects' command requires a file list devoid of annotated
	 * revision specificity.
	 * 
	 * @param fileList if non-null, only those protection entries that apply to the
	 * 				specified files are displayed.
	 * @param opts GetProtectionEntriesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null but possibly empty list of protection entries.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IProtectionEntry> getProtectionEntries(List<IFileSpec> fileList, GetProtectionEntriesOptions opts)
							throws P4JavaException;
	
	/**
	 * Get an InputStream onto the entries of the Perforce protections table.<p>
	 * 
	 * @return a non-null but possibly empty InputStream onto the protections table's entries.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */

	InputStream getProtectionsTable() throws P4JavaException;

	/**
	 * Get a list of IClientSummary objects for all Perforce clients known to this Perforce
	 * server.<p>
	 * 
	 * Note that this method returns light-weight IClientSummary objects rather than full
	 * IClient objects; if you need the heavy-weight IClient objects, you should use getClient().
	 * 
	 * Note also that the returned IClient objects are not "complete", in the sense
	 * that implementations are free to leave certain attributes null for performance
	 * reasons. In general, at least the client's name, root, description, last modification
	 * time are guaranteed correct.
	 * 
	 * @param opts GetClientsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly empty) list of Client objects for Perforce clients
	 * 				known to this Perforce server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IClientSummary> getClients(GetClientsOptions opts) throws P4JavaException;
	
	/**
	 * Get a list of Perforce labels, optionally tied to a specific set of files.<p>
	 * 
	 * Note that the ILabel objects returned here do not have views associated with
	 * them (i.e. the getViewMapping() method will return an empty list. If you need
	 * to get the view mapping for a specific label, use the getLabel() method.
	 * 
	 * @param fileList if not null, limits its report to labels that contain those files
	 * @param opts GetLabelsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly-empty) list of qualifying Perforce labels
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<ILabelSummary> getLabels(List<IFileSpec> fileList, GetLabelsOptions opts)
									throws P4JavaException;
	
	/**
	 * Delete a named Perforce label from the Perforce server.
	 * 
	 * @param labelName non-null label name
	 * @param opts DeleteLabelOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String deleteLabel(String labelName, DeleteLabelOptions opts) throws P4JavaException;
	
	/**
	 * @param fileSpecs non-null list of files to be tagged.
	 * @param labelName non-null label name to use for the tagging.
	 * @param opts TagFilesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null (but possibly empty) list of affected file specs.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileSpec> tagFiles(List<IFileSpec> fileSpecs, String labelName, TagFilesOptions opts)
							throws P4JavaException;
	
	
	/**
	 * Get a list of all summary Perforce branch specs known to the Perforce server.<p>
	 * 
	 * Note that the IBranchSpecSummary objects returned here do not have branch
	 * view specs; you must call the getBranchSpec method on a specific branch to get
	 * valid view specs for a branch.
	 * 
	 * @param opts object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly-empty) list of IBranchSpecSummary objects.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IBranchSpecSummary> getBranchSpecs(GetBranchSpecsOptions opts) throws P4JavaException;
	
	/**
	 * Delete a named Perforce branch spec from the Perforce server.
	 * 
	 * @param branchSpecName non-null name of the branch spec to be deleted.
	 * @param opts DeleteBranchSpecOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String deleteBranchSpec(String branchSpecName, DeleteBranchSpecOptions opts)
																throws  P4JavaException;
	
	/**
	 * Get a template of a non-existent named Perforce client. This will only
	 * return an IClient for clients that don't exist unless the allowExistent
	 * parameter is set to true. This method is designed to be able to get the
	 * server returned default values it uses when a non-existent client is
	 * requested.
	 * 
	 * @param clientName non-null Perforce client name.
	 * @param opts GetClientTemplateOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return IClient representing the specified Perforce client template, or null if
	 *         no such client template.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	IClient getClientTemplate(String clientName, GetClientTemplateOptions opts)
																throws P4JavaException;
	
	/**
	 * Update an existing Perforce client on the current Perforce server. This client does
	 * not need to be the current client, and no association with the passed-in client is
	 * made by the server (i.e. it's not made the current client).
	 * 
	 * @since 2011.2
	 * @param client non-null IClient defining the Perforce client to be updated
	 * @param force if true, tell the server to attempt to force the update regardless of
	 * 				the consequences. You're on your own with this one...
	 * @return possibly-null operation result message string from the Perforce server
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String updateClient(IClient client, boolean force) throws P4JavaException;

	/**
	 * Update an existing Perforce client on the current Perforce server. This client does
	 * not need to be the current client, and no association with the passed-in client is
	 * made by the server (i.e. it's not made the current client).
	 * 
	 * @since 2011.2
	 * @param client non-null IClient defining the Perforce client to be updated
	 * @return possibly-null operation result message string from the Perforce server
	 * @param opts UpdateClientOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String updateClient(IClient client, UpdateClientOptions opts)
																throws P4JavaException;

	/**
	 * Delete a Perforce client from a Perforce server. The effects this has on the client
	 * and the server are not well-defined here, and you should probably consult the relevant
	 * Perforce documentation for your specific case. In any event, you can cause quite
	 * a lot of inconvenience (and maybe even damage) doing a forced delete without preparing
	 * properly for it, especially if the client is the server object's current client.
	 * 
	 * @param clientName non-null name of the client to be deleted from the server.
	 * @param opts DeleteClientOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return possibly-null operation result message string from the Perforce server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String deleteClient(String clientName, DeleteClientOptions opts)
																throws P4JavaException;
	
	/**
	 * Switch the target client spec's view without invoking the editor.
	 * With -t to switch to a view defined in another client spec. Switching
	 * views is not allowed in a client that has opened files. The -f flag can
	 * be used with -s to force switching with opened files. View switching has
	 * no effect on files in a client workspace until 'p4 sync' is run.
	 * 
	 * @since 2011.2
	 * @param templateClientName non-null name of the template client who's view
	 *				will be used for the target (or current) client to switched to.
	 * @param targetClientName possibly-null name of the target client whose view
	 * 				will be changed to the template client's view. If null, the
	 * 				current client will be used.
	 * @return possibly-null operation result message string from the Perforce server
	 * @param opts SwitchClientViewOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String switchClientView(String templateClientName, String targetClientName, SwitchClientViewOptions opts)
								throws P4JavaException;

	/**
	 * Switch the target client spec's view without invoking the editor.
	 * With -S to switch to the specified stream's view. Switching views is not
	 * allowed in a client that has opened files. The -f flag can be used with
	 * -s to force switching with opened files. View switching has no effect on
	 * files in a client workspace until 'p4 sync' is run.
	 * 
	 * @since 2011.2
	 * @param streamPath non-null stream's path in a stream depot, of the form //depotname/streamname
	 * 				who's view will be used for the target (or current) client to
	 * 				switched to.
	 * @param targetClientName possibly-null name of the target client whose view
	 * 				will be changed to the stream's view. If null, the current
	 * 				client will be used.
	 * @return possibly-null operation result message string from the Perforce server
	 * @param opts SwitchClientViewOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String switchStreamView(String streamPath, String targetClientName, SwitchClientViewOptions opts)
								throws P4JavaException;
	/**
	 * Get a list of revision annotations for the specified files.
	 * 
	 * @param fileSpecs non-null list of file specs to be annotated.
	 * @param opts GetFileAnnotationsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly-empty) list of IFileAnnotation objects representing
	 * 				version annotations for the passed-in file specs.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileAnnotation> getFileAnnotations(List<IFileSpec> fileSpecs, GetFileAnnotationsOptions opts)
												throws P4JavaException;
	
	/**
	 * Move a file already opened for edit or add (the fromFile) to the destination 
	 * file (the toFile). A file can be moved many times before it is submitted; 
	 * moving it back to its original location will reopen it for edit. The full
	 * semantics of this operation (which can be confusing) are found in the
	 * main 'p4 help' documentation.<p>
	 * 
	 * Note that this operation is not supported on servers earlier than 2009.1;
	 * any attempt to use this on earlier servers will result in a RequestException
	 * with a suitable message.<p>
	 * 
	 * Note also that the move command is special in that almost alone among Perforce
	 * file-based commands, it does not allow full filespecs with version specifiers;
	 * these are currently quietly stripped off in the move command implementation here,
	 * which may lead to unexpected behaviour if you pass in specific versions expecting
	 * them to be honoured.
	 * 
	 * @param fromFile the original file; must be already open for edit.
	 * @param toFile the target file.
	 * @param opts MoveFileOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return list of IFileSpec objects representing the results of this move.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileSpec> moveFile(IFileSpec fromFile, IFileSpec toFile, MoveFileOptions opts)
												throws P4JavaException;
	
	/**
	 * List any directories matching the passed-in file specifications.
	 * 
	 * @param fileSpecs non-null list of file specifications.
	 * @param opts GetDirectoriesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null but possibly empty list of qualifying directory file specs; only
	 * 					the getPath() path will be valid.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IFileSpec> getDirectories(List<IFileSpec> fileSpecs, GetDirectoriesOptions opts)
												throws P4JavaException;
	
	/**
	 * Get a list of Perforce changelist summary objects from the Perforce server.
	 * 
	 * @param fileSpecs if non-empty, limits the results to
	 * 				changelists that affect the specified files.  If the file specification
     *				includes a revision range, limits its results to
     * 				submitted changelists that affect those particular revisions
	 * @param opts GetChangelistsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null (but possibly empty) list of qualifying changelists.
	 * @throws P4JavaException if any error occurs in the processing of this method
	 */
	
	List<IChangelistSummary> getChangelists(List<IFileSpec> fileSpecs, GetChangelistsOptions opts)
												throws P4JavaException;
	
	/**
	 * Get a specific Perforce changelist from a Perforce server.<p>
	 * 
	 * Corresponds fairly well to the p4 command-line command "change -o", and (like
	 * "change -o") does <i>not</i> include the associated changelist files (if any)
	 * in the returned changelist object -- you must use getChangelistFiles (or similar)
	 * to properly populate the changelist for submission, for example.
	 * 
	 * @param id the Perforce changelist ID; if id is IChangelist.DEFAULT, get the default
	 * 					changelist for the current client (if available)
	 * @param opts ChangelistOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null IChangelist describing the changelist; if no such changelist,
	 * 			a RequestException is thrown.
	 * @throws P4JavaException if any error occurs in the processing of this method
	 */
	
	IChangelist getChangelist(int id, ChangelistOptions opts) throws P4JavaException;
	
	
	/**
	 * Delete a pending Perforce changelist. Throws a P4JavaException
	 * if the changelist was associated with opened files or was not a
	 * pending changelist.<p>
	 * 
	 * Note: any IChangelist object associated with the given changelist
	 * will no longer be valid after this operation, and using that object may
	 * cause undefined results or even global disaster -- you must ensure that
	 * the object is not used again improperly.
	 * 
	 * @param id the ID of the Perforce pending changelist to be deleted.
	 * @param opts ChangelistOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return possibly-null operation result message string from the Perforce server
	 * @throws P4JavaException if any error occurs in the processing of this method
	 */
	
	String deletePendingChangelist(int id, ChangelistOptions opts) throws P4JavaException;

	/**
	 * Get an InputStream onto the file diffs associated with a specific submitted
	 * changelist. This method (like the similar "p4 describe" command) will
	 * not return diffs for pending changelists.<p>
	 * 
	 * This is one of the guaranteed "live" method on this interface, and will
	 * return the diff output as it exists when called (rather than when the underlying
	 * implementation object was created). This can be an expensive method
	 * to evaluate, and can generate reams and reams (and reams) of output,
	 * so don't use it willy-nilly.<p>
	 * 
	 * Note that unlike the corresponding command-line command, which
	 * keeps going in the face of errors by moving on to the next file (or
	 * whatever), any errors encountered in this method will cause an exception
	 * from this method at the first error, so plan accordingly....
	 * 
	 * @param id the ID of the target changelist.
	 * @param opts GetChangelistDiffsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return InputStream onto the diff stream. Note that
	 *			while this stream will not be null, it may be empty.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	InputStream getChangelistDiffs(int id, GetChangelistDiffsOptions opts)
												throws P4JavaException;
	
	/**
	 * Return an InputStream onto the contents of one or more revisions of one or more
	 * Perforce depot file contents.<p>
	 * 
	 * If file is specified as a Perforce client workspace file name, the client view is used to
     * find the corresponding depot file. If a file argument has a revision, then all files as of that
	 * revision are streamed.  If a file argument has a revision range, then only files selected
	 * by that revision range are streamed, and the highest revision in the range is used for each file. 
	 * Normally, only the head revision is printed.<p>
	 * 
	 * The underlying input stream is not guaranteed to support mark() and skip() operations, and in
	 * some cases can be absolutely ginormously long it is also not guaranteed to be printable,
	 * and will be in the charset encoding stored in the Perforce server.<p>
	 * 
	 * You should close the InputStreamReader after use in order to release any underlying
	 * stream-related resources. Failure to do this may lead to the proliferation of
	 * temp files or long-term memory wastage or even leaks.<p>
	 * 
	 * Note that unlike the corresponding command-line command, which
	 * keeps going in the face of errors by moving on to the next file (or
	 * whatever), any errors encountered in this method will cause an exception
	 * from this method at the first error, so plan accordingly....
	 * 
	 * @param fileSpecs non-null list of depot or client file specs defining files to be streamed
	 * @param opts GetFileContentsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null but possibly-empty InputStream onto the file / revision contents.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	InputStream getFileContents(List<IFileSpec> fileSpecs, GetFileContentsOptions opts)
												throws P4JavaException;
	
	/**
	 * Get the revision history data for one or more Perforce files.<p>
	 * 
	 * @param fileSpecs filespecs to be processed; if null or empty,
	 * 			an empty Map is returned.
	 * @param opts GetRevisionHistoryOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null map of lists of revision data for qualifying files; the map is keyed
	 * 			by the IFileSpec of the associated file, meaning that errors are
	 * 			signaled using the normal IFileSpec getOpStatus() method.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(List<IFileSpec> fileSpecs,
								GetRevisionHistoryOptions opts) throws P4JavaException;
	
	/**
	 * Get a list of all users who have subscribed to review the named files.
	 * 
	 * Note that the returned IUserSummary objects will have null access
	 * and update dates associated with them.
	 * 
	 * @param fileSpecs if not null, use this list as the list of named files rather
	 * 				than all files.
	 * @param opts GetReviewsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null but possibly empty list of IUserSummary objects; note that
	 * 				these objects will have null update and access fields.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IUserSummary> getReviews(List<IFileSpec> fileSpecs, GetReviewsOptions opts)
												throws P4JavaException;
	
	/**
	 * Get a list of all submitted changelists equal or above a provided changelist
	 * number that have not been reviewed before.<p>
	 * 
	 * If only the 'changelistId' option is provided, return a list of changelists
	 * that have not been reviewed before, equal or above the specified changelist#.<p>
	 * 
	 * If only the 'counter' option is provided, return a list of changelists that
	 * have not been reviewed before, above the specified counter's changelist#.<p>
	 * 
	 * If both the 'changelistId' and 'counter' options are specified, 'p4 review'
	 * sets the counter to that changelist# and	produces no output. This functionality
	 * has been superceded by the 'p4 counter' command. The user must have permission
	 * to set counters.
	 * 
	 * @since 2012.2
	 * @param opts GetReviewChangelistsOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @return non-null but possibly empty list of IReviewChangelist objects; note
	 * 				that these objects will have null update and access fields.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IReviewChangelist> getReviewChangelists(GetReviewChangelistsOptions opts)
												throws P4JavaException;
	/**
	 * If one or more Perforce file specs is passed-in, return the opened / locked status
	 * of each file (if known) within an IFileSpec object; otherwise
	 * return a list of all files known to be open for this Perforce client workspace.<p>
	 * 
	 * @param fileSpecs if non-empty, determine the status of the specified
	 * 				files; otherwise return all qualifying files known to be open
	 * @param opts possibly-null OpenedFilesOptions object object specifying method options.
	 * @return non-null but possibly-empty list of qualifying open files. Not all fields
	 * 				in individual file specs will be valid or make sense to be accessed.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IFileSpec> getOpenedFiles(List<IFileSpec> fileSpecs, OpenedFilesOptions opts)
							throws P4JavaException;
	
	/**
	 * Return a list of everything Perforce knows about a set of Perforce files.<p>
	 * 
	 * This method is not intended for general use, and is not documented in detail here;
	 * consult the main Perforce fstat command documentation for detailed help.
	 * 
	 * This method can be a real server and bandwidth resource hog, and should be used as
	 * sparingly as possible; alternatively, try to use it with as narrow a set of file
	 * specs as possible.
	 * 
	 * @param fileSpecs non-null list of Perforce file specification(s).
	 * @param opts GetExtendedFilesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly empty) list of qualifying files and associated stat info.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> fileSpecs, GetExtendedFilesOptions opts)
							throws P4JavaException;
	
	/**
	 * Get a list of submitted integrations for the passed-in filespecs
	 * 
	 * @param fileSpecs if null or omitted, all qualifying depot files are used.
	 * @param opts GetSubmittedIntegrations object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null but possibly empty list of IFileSpec representing
	 * 			qualifying integrations.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs, GetSubmittedIntegrationsOptions opts)
							throws P4JavaException;
	
	/**
	 * Returns a list of changelists that have not been integrated from a set of source
	 * files to a set of target files.
	 * 
	 * @param fromFile if non-null, use this as the from-file specification.
	 * @param toFile if non-null, use this as the to-file specification.
	 * @param opts GetInterchangesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly empty) list of qualifying changelists. Note that
	 * 				the changelists returned here may not have all fields set (only
	 * 				description, ID, date, user, and client are known to be properly
	 * 				set by the server for this command)
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile, GetInterchangesOptions opts)
							throws P4JavaException;
	
	/**
	 * Returns a list of changelists that have not been integrated from a set of source
	 * files to a set of target files.<p>
	 * 
	 * Note that depending on the specific options passed-in the fromFileList can
	 * be null or one file spec; the toFileList can be null, one or more file specs.
	 * The full semantics of this operation are found in the main 'p4 help interchanges'
	 * documentation.<p>
	 * 
	 * @param branchSpecName if non-null and not empty, use this as the branch spec name.
	 * @param fromFileList if non-null and not empty, and biDirectional is true,
	 * 				use this as the from file list.
	 * @param toFileList if non-null and not empty, use this as the to file list.
	 * @param opts GetInterchangesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly empty) list of qualifying changelists. Note that
	 * 				the changelists returned here may not have all fields set (only
	 * 				description, ID, date, user, and client are known to be properly
	 * 				set by the server for this command)
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IChangelist> getInterchanges(String branchSpecName,
			List<IFileSpec> fromFileList, List<IFileSpec> toFileList, GetInterchangesOptions opts)
							throws P4JavaException;
	
	/**
	 * Return a list of Perforce jobs. Note that (as discussed in the IJob comments)
	 * Perforce jobs can have a wide variety of fields, formats, semantics, etc., and
	 * this method can return a list that may have to be unpacked at the map level by
	 * the consumer to make any sense of it.<p>
	 * 
	 * Note that this method (unlike the main file list methods) throws an exception
	 * and stops at the first encountered error.
	 * 
	 * @param fileSpecs if given, return only jobspecs affecting the given file(s).
	 * @param opts GetJobsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null (but possibly-empty) list of qualifying Perforce jobs.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IJob> getJobs(List<IFileSpec> fileSpecs, GetJobsOptions opts) throws P4JavaException;
	
	/**
	 * Mark each named job as being fixed by the changelist number given with changeListId.
	 * 
	 * @param jobIds non-null non-empty list of affected job IDs.
	 * @param changelistId changelist ID for affected changelist.
	 * @param opts FixJobsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return list of affected fixes.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IFix> fixJobs(List<String> jobIds, int changelistId, FixJobsOptions opts)
							throws P4JavaException;
	
	/**
	 * Run diff on the Perforce server of two files in the depot.<p>
	 * 
	 * With a branch view, fromFile and toFile are optional; fromFile limits
	 * the scope of the source file set, and toFile limits the scope of the
	 * target. If only one file argument is given, it is assumed to be toFile.<p>
	 * 
	 * This method corresponds closely to the standard diff2 command, and that
	 * command's documentation should be consulted for the overall and detailed
	 * semantics.<p>
	 * 
	 * As with other streams-based IServer methods, callers should ensure that
	 * the stream returned here is always explicitly closed after use; if not
	 * closed, the stream's associated temporary files managed by P4Java
	 * (if they exist) may not be properly deleted.
	 * 
	 * @param fromFile (optional, with a branch view) source file IFileSpec 
	 * @param toFile (optional, with a branch view) target file IFileSpec
	 * @param branchSpecName optional branch spec name
	 * @param opts GetFileDiffsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null but possibly empty InputStream of diffs and headers
	 * 				as returned from the server.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	InputStream getFileDiffsStream(IFileSpec fromFile, IFileSpec toFile, String branchSpecName,
							GetFileDiffsOptions opts) throws P4JavaException;
	
	/**
	 * Run diff on the Perforce server of two files in the depot.<p>
	 * 
	 * With a branch view, fromFile and toFile are optional; fromFile limits
	 * the scope of the source file set, and toFile limits the scope of the
	 * target. If only one file argument is given, it is assumed to be toFile.<p>
	 * 
	 * This method corresponds closely to the standard diff2 command, and that
	 * command's documentation should be consulted for the overall and detailed
	 * semantics.<p>
	 * 
	 * @param fromFile (optional, with a branch view) source file IFileSpec 
	 * @param toFile (optional, with a branch view) target file IFileSpec
	 * @param branchSpecName optional branch spec name
	 * @param opts GetFileDiffsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null but possibly empty array of file diffs
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IFileDiff> getFileDiffs(IFileSpec fromFile, IFileSpec toFile, String branchSpecName,
							GetFileDiffsOptions opts) throws P4JavaException;
	
	/**
	 * Get the value of a named Perforce counter from the Perforce server. Note that this
	 * method will return a zero string (i.e. "0") if the named counter doesn't exist (rather
	 * than throw an exception); use getCounters to see if a counter actually exists before
	 * you use it.<p>
	 * 
	 * Note that despite their name, counters can be any value, not just a number; hence
	 * the string return value here.
	 * 
	 * @since 2012.2
	 * @param counterName non-null counter name.
	 * @return non-null (but possibly empty or useless) counter value associated
	 * 				with counterName.
	 * @param opts CounterOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String getCounter(String counterName, CounterOptions opts) throws P4JavaException;

	/**
	 * Get a map of the Perforce server's counters.
	 * 
	 * @since 2012.2
	 * @return a non-null (but possibly empty) map of counters.
	 * @param opts CounterOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 * 
	 * @deprecated As of release 2013.1, replaced by {@link #getCounters(com.perforce.p4java.option.server.GetCountersOptions)}
 	 */
	@Deprecated
	Map<String, String> getCounters(CounterOptions opts) throws P4JavaException;
	
	/**
	 * Get a map of the Perforce server's counters.
	 * 
	 * @since 2013.1
	 * @return a non-null (but possibly empty) map of counters.
	 * @param opts GetCountersOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	Map<String, String> getCounters(GetCountersOptions opts) throws P4JavaException;

	/**
	 * Create, set or delete a counter on a Perforce server. This method can be used to
	 * create, set, increment, or delete a counter according to the specific options
	 * set in the associated options object. Note that the increment operation does not
	 * work on servers earlier than 10.1, and that the return value is <i>never</i> guaranteed
	 * to be non-null -- use with caution.
	 * 
	 * @param counterName non-null counter name.
	 * @param value value the counter should be set to; can be null if the set operation
	 * 				is an increment.
	 * @param opts CounterOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return possibly-null current (post-set, post-increment) value; may be zero if the
	 * 				operation was a delete; may not be reliable for pre 10.1 servers.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	String setCounter(String counterName, String value, CounterOptions opts) throws P4JavaException;
	
	/**
	 * Get a list of exported journal or checkpoint records (admin / superuser command).<p>
	 * 
	 * See the main p4 export command documentation for full semantics and usage details.<p>
	 * 
	 * Note that the 'skip*' options in ExportRecordsOptions are specific to
	 * P4Java only; they are not Perforce command options. These options are for
	 * field handling rules in the lower layers of P4Java. The field rules are
	 * for identifying the fields that should skip charset translation of their
	 * values; leaving their values as bytes instead of converting them to
	 * strings. Please see ExportRecordsOptions for usage details.
	 * 
	 * @param opts ExportRecordsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null but possibly empty list of maps representing exported
	 * 				journal or checkpoint records.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<Map<String, Object>> getExportRecords(ExportRecordsOptions opts) throws P4JavaException;
	
	/**
	 * Get each exported journal or checkpoint record (admin / superuser command)
	 * as it comes in from the server, rather than waiting for the entire command
	 * to complete.<p>
	 * 
	 * The results are sent to the user using the IStreamingCallback handleResult
	 * method; see the IStreamingCallback Javadoc for details. The payload passed
	 * to handleResult is usually the raw map gathered together deep in the RPC
	 * protocol layer, and the user is assumed to have the knowledge and technology
	 * to be able to parse it and use it suitably in much the same way as a user
	 * unpacks or processes the results from the other low-level exec methods
	 * like execMapCommand.<p>
	 * 
	 * See the main p4 export command documentation for full semantics and usage details.<p>
	 * 
	 * Note that the 'skip*' options in ExportRecordsOptions are specific to
	 * P4Java only; they are not Perforce command options. These options are for
	 * field handling rules in the lower layers of P4Java. The field rules are
	 * for identifying the fields that should skip charset translation of their
	 * values; leaving their values as bytes instead of converting them to
	 * strings. Please see ExportRecordsOptions for usage details.
	 * 
	 * @since 2012.3
	 * @param opts ExportRecordsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @param callback a non-null IStreamingCallback to be used to process the incoming
	 * 				results.
	 * @param key an opaque integer key that is passed to the IStreamingCallback callback
	 * 				methods to identify the action as being associated with this specific
	 * 				call.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	void getStreamingExportRecords(ExportRecordsOptions opts, IStreamingCallback callback,
							int key) throws P4JavaException;

	/**
	 * Set file attributes on one or more files (unsupported). See the main Perforce documentation
	 * for an explanation of file attributes, which are potentially complex and difficult to use
	 * efficiently. Attributes can currently only be retrieved using the getExtendedFiles (fstat)
	 * operation.<p>
	 * 
	 * Note that this method only accepts String attribute values; if the attribute is intended
	 * to be binary, use the setHexValue setter on the associated SetFileAttributesOptions
	 * object and hexify the value, or, alternatively, use the stream version of this method.
	 * String input this way will be converted to bytes for the attributes before being sent to
	 * the Perforce server using the prevailing character set. If this is a problem, use hex
	 * encoding or the stream variant of this method<p>
	 * 
	 * Note that attributes can only be removed from a file by setting the appropriate value of the
	 * name / value pair passed-in through the attributes map to null.<p>
	 * 
	 * Note that the filespecs returned by this method, if valid, contain only the depot path
	 * and version information; no other field can be assumed to be valid. Note also that,
	 * while the p4 command line executable returns a list of results that amounts to the cross
	 * product of files and attributes, this method never returns more than one result for each
	 * file affected.
	 * 
	 * @since 2011.1
	 * @param opts SetFileAttributesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @param attributes a non-null Map of attribute name / value pairs; if any value is null,
	 * 				that attribute is removed.
	 * @param files non-null list of files to be affected
	 * @return non-null but possibly empty list of filespec results for the operation.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IFileSpec> setFileAttributes(List<IFileSpec> files, Map<String, String> attributes,
							SetFileAttributesOptions opts) throws P4JavaException;
	
	/**
	 * Set a file attribute on one or more files using the passed-in input stream as the source
	 * for the attribute's value (unsupported). See the main Perforce documentation
	 * for an explanation of file attributes, which are potentially complex and difficult to use
	 * efficiently. Attributes can currently only be retrieved using the getExtendedFiles (fstat)
	 * operation.<p>
	 * 
	 * This method is intended to allow for unmediated binary definitions of file attribute
	 * contents, and is typically used for things like thumbnails that are too big to be
	 * conveniently handled using hex conversion with the strings-based version of this
	 * method. Absolutely no interpretation is done on the stream -- it's bytes all the
	 * way... there is also no hard limit to the size of the stream that contains the attribute
	 * value, but the consequences on both the enclosing app and the associated Perforce server
	 * of too-large attributes may be severe. Typical 8K thumbnails are no problem at all, but
	 * something in the megabyte range or larger might be problematic at both ends.<p>
	 * 
	 * Note that this method will leave the passed-in stream open, but (in general) the stream's
	 * read pointer will be at the end of the stream when this method returns. You are responsible
	 * for closing the stream if necessary after the call; you are also responsible for ensuring
	 * that the read pointer is where you want it to be in the stream (i.e. where you want the
	 * method to start reading the attribute value from) when you pass in the stream. I/O errors
	 * while reading the stream will be logged, but otherwise generally ignored -- you must check
	 * the actual results of this operation yourself.<p>
	 * 
	 * Note that the server currently only supports setting file attributes using a stream for
	 * one filespec at a time, but for reasons of symmetry you must pass in a list of (one)
	 * filespec. Note that this doesn't necessarily mean only one <i>file</i> is affected in
	 * the depot, just that only one file <i>spec</i> is used to specify the affected file(s).<p> 
	 * 
	 * Note that attributes can only be removed from a file by setting the appropriate value of the
	 * name / value pair passed-in through the attributes map to null.<p>
	 * 
	 * Note that the filespecs returned by this method, if valid, contain only the depot path
	 * and version information; no other field can be assumed to be valid. Note also that,
	 * while the p4 command line executable returns a list of results that amounts to the cross
	 * product of files and attributes, this method never returns more than one result for each
	 * file affected.
	 * 
	 * @since 2011.1
	 * @param opts SetFileAttributesOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @param attributeName the non-null name of the attribute to be set.
	 * @param inStream non-null InputStream ready for reading the attribute value from.
	 * @param files  non-null list of files to be affected.
	 * @return non-null but possibly empty list of filespec results for the operation.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<IFileSpec> setFileAttributes(List<IFileSpec> files, String attributeName,
				InputStream inStream, SetFileAttributesOptions opts) throws P4JavaException;
	
	/**
	 * Show server configuration values. See the main Perforce documentation for the
	 * details of this admin command, but note that only one of serverName or variableName
	 * should be non-null (they can both be null, which means ignore them both). If they're both
	 * null, serverName currently takes precedence, but that's not guaranteed.<p>
	 * 
	 * Note: you must be an admin or super user for this command to work.
	 * 
	 * @since 2011.1
	 * @param serverName if not null, only show values associated with the named server; if
	 * 				equals ServerConfigurationValue.ALL_SERVERS, show values associated
	 * 				with all participating servers.
	 * @param variableName if not null, only show the value of this named config variable.
	 * @return non-null (but possibly-empty) list of qualifying ServerConfigurationValue objects.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	List<ServerConfigurationValue> showServerConfiguration(String serverName, String variableName)
												throws P4JavaException;
	
	/**
	 * Set or unset a specific names server configuration variable. Config variables
	 * are unset by passing in a null value parameter.<p>
	 * 
	 * Expected variable name formats are as specified in the main Perforce documentation:
	 * [servername + #] variablename -- but this is not enforced by P4Java itself.<p>
	 * 
	 * Note: you must be an admin or super user for this command to work.
	 * 
	 * @since 2011.1
	 * @param name non-null config variable name.
	 * @param value if null, unset the named variable; otherwise, set it to the passed-in
	 * 			string value.
	 * @return possibly-null operation status string returned by the server in response to
	 * 			this set / unset attempt.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String setServerConfigurationValue(String name, String value) throws P4JavaException;

	/**
	 * Get a list of disk space information about the current availability of
	 * disk space on the server. This command requires that the user be an
	 * operator or have 'super' access granted by 'p4 protect'.
	 * <p>
	 * 
	 * If no arguments are specified, disk space information for all relevant
	 * file systems is displayed; otherwise the output is restricted to the
	 * named filesystem(s).
	 * <p>
	 * 
	 * filesystems: P4ROOT | P4JOURNAL | P4LOG | TEMP | journalPrefix | depot
	 * 
	 * See the main 'p4 diskspace' command documentation for full semantics and
	 * usage details.
	 * 
	 * @since 2011.2
	 * @param filesystems
	 *            if not null, specify a list of Perforce named filesystem(s).
	 * @return non-null but possibly empty list of disk space information.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IDiskSpace> getDiskSpace(List<String> filesystems)	throws P4JavaException;

	/**
	 * Create or replace the protections table data on the Perforce server with
	 * these new protection entries.<p>
	 * 
	 * Each entry in the table contains a protection mode, a group/user	indicator,
	 * the group/user name, client host ID and a depot file path pattern. Users
	 * receive the highest privilege that is granted on any entry.
	 * 
	 * Warning: this will overwrite the existing protections table data.
	 * 
	 * @since 2011.2
	 * @param entryList non-null list of protection entries.
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	String createProtectionEntries(List<IProtectionEntry> entryList) throws P4JavaException;
	
	/**
	 * Replace the protections table data on the Perforce server with these new
	 * protection entries.<p>
	 * 
	 * Each entry in the table contains a protection mode, a group/user	indicator,
	 * the group/user name, client host ID and a depot file path pattern. Users
	 * receive the highest privilege that is granted on any entry.<p>
	 * 
	 * Warning: this will overwrite the existing protections table data.
	 * 
	 * @since 2011.2
	 * @param entryList non-null list of protection entries.
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	String updateProtectionEntries(List<IProtectionEntry> entryList) throws P4JavaException;

	/**
	 * Obliterate permanently remove files and their history from the server. <p>
	 * 
	 * Obliterate retrieves the disk space used by the obliterated files in the
	 * archive and clears the files from the metadata that is maintained by the
	 * server. Files in client workspaces are not physically affected, but they
	 * are no longer under Perforce control. <p>
	 * 
	 * By default, obliterate displays a preview of the results. To execute the
	 * operation, you must specify the -y flag (opts.executeObliterate). Obliterate
	 * requires 'admin' access, which is granted by 'p4 protect'. <p>
	 * 
	 * The "obliterate" command returns an IOblterateResult for each file passed
	 * into the command. Each IObliterateResult object contains a summary of various
	 * types of records deleted (or added) and a non-null list of returned filespecs
	 * have the equivalent of purgeFile and purgeRev output in the depotPath and
	 * endRevision fileds of the associated filespecs, and that no other file spec
	 * fields are valid. Sometimes, the server doesn't return any "purgeFile" and
	 * "purgeRev" values. <p>
	 * 
	 * Note: error and info messages are stored in filespec objects. <p>
	 * 
	 * @since 2011.2
	 * @param fileSpecs non-null list of files to be obliterated
	 * @param opts possibly-null ObliterateFilesOptions object specifying method options.
	 * @return a non-null list of IObliterateResult objects containing the records purged.
	 * @throws P4JavaException if an error occurs processing this method and its parameters
	 */
	
	List<IObliterateResult> obliterateFiles(List<IFileSpec> fileSpecs, ObliterateFilesOptions opts)
												throws P4JavaException;

	/**
	 * Get a list of all summary Perforce streams known to the Perforce server.<p>
	 * 
	 * Note that the IStreamSummary objects returned here do not have stream paths.
	 * You must call the getStream method on a specific stream to get valid paths
	 * for a stream.
	 * 
	 * @since 2011.2
	 * @param streamPaths if specified, the list of streams is limited to those
	 * 				matching the supplied list of stream paths, of the form //depotname/streamname
	 * @param opts object describing optional parameters; if null,
	 * 				no options are set.
	 * @return non-null (but possibly-empty) list of IStreamSummary objects.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	List<IStreamSummary> getStreams(List<String> streamPaths, GetStreamsOptions opts) throws P4JavaException;
	
	/**
	 * Create a new stream in the repository.
	 * 
	 * @since 2011.2
	 * @param stream non-null IStream object representing the stream to be created.
	 * @return possibly-null operation result message string from the Perforce server.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String createStream(IStream stream) throws P4JavaException;
	
	/**
	 * Get an individual stream by stream path. Note that this method will return a
	 * fake stream if you ask it for a non-existent stream, so it's not the most
	 * useful of operations.
	 * 
	 * @since 2011.2
	 * @param streamPath non-null stream's path in a stream depot, of the form //depotname/streamname
	 * @return IStream non-null object corresponding to the named stream if it exists and is
	 * 			retrievable; otherwise an IStream object that looks real but does not, in
	 * 			fact, correspond to any known stream in the repository.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	IStream getStream(String streamPath) throws P4JavaException;

	/**
	 * Get an individual stream by stream path. Note that this method will return a
	 * fake stream if you ask it for a non-existent stream, so it's not the most
	 * useful of operations.
	 * 
	 * @since 2012.1
	 * @param streamPath non-null stream's path in a stream depot, of the form //depotname/streamname
	 * @param opts GetStreamOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @return IStream non-null object corresponding to the named stream if it exists and is
	 * 			retrievable; otherwise an IStream object that looks real but does not, in
	 * 			fact, correspond to any known stream in the repository.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	IStream getStream(String streamPath, GetStreamOptions opts) throws P4JavaException;

	/**
	 * Update a Perforce stream spec on the Perforce server.
	 * 
	 * @since 2011.2
	 * @param stream non-null stream spec to be updated.
	 * @param opts StreamOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	String updateStream(IStream stream, StreamOptions opts) throws P4JavaException;
	
	/**
	 * Delete a Perforce stream spec from the Perforce server.
	 * 
	 * @since 2011.2
	 * @param streamPath non-null stream's path in a stream depot, of the form //depotname/streamname
	 * @param opts StreamOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	String deleteStream(String streamPath, StreamOptions opts) throws P4JavaException;

	/**
	 * Get a specific named Perforce branch spec from the Perforce server.<p>
	 * 
	 * Note that since the Perforce server usually interprets asking for a non-existent
	 * branch spec as equivalent to asking for a template for a new branch spec,
	 * you will normally always get back a result here. It is best to first use
	 * the getBranchSpecList method to see if the branch spec exists, then
	 * use this method to retrieve a specific branch spec once you know it exists.
	 * 
	 * @since 2011.2
	 * @param name non-null Perforce branch name.
	 * @param opts GetBranchSpecOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @return potentially-null IBranchSpec for the named Perforce branch spec.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	
	IBranchSpec getBranchSpec(String name, GetBranchSpecOptions opts)
																throws P4JavaException;

	/**
	 * Get a stream's cached integration status with respect to its parent. If
	 * the cache is stale, either because newer changes have been submitted or
	 * the stream's branch view has changed, 'p4 istat' checks for pending
	 * integrations and updates the cache before showing status. <p>
	 * 
	 * Pending integrations are shown only if they are expected by the stream;
	 * that is, only if they are warranted by the stream's type	and its
	 * fromParent/toParent flow options. (See 'p4 help stream'.) <p>
	 * 
	 * @since 2011.2
	 * @param stream the stream's path in a stream depot, of the form //depotname/streamname.
	 * @param opts StreamIntegrationStatusOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @return potentially-null IStreamIntegrationStatus object representing the
	 * 				stream's cached integration status with respect to its parent.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	IStreamIntegrationStatus getStreamIntegrationStatus(String stream, StreamIntegrationStatusOptions opts)
																throws P4JavaException;

	/**
	 * Get the last block(s) of the errorLog and the offset required to get the
	 * next block when it becomes available. <p>
	 * 
	 * The data is returned in the tagged field 'data', in blocks of the size
	 * specified by the blocksize parameter. The 'offset' field contains the start
	 * of the next block, which can be used with -s to request the next batch of
	 * error log data. <p>
	
	 * Note that this command requires that the user be an operator or have 'super'
	 * access, which is granted by 'p4 protect'.
	 * 
	 * @since 2011.2
	 * @param opts LogTailOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @return possibly-null ILogTail object representing outputs of the error log.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	ILogTail getLogTail(LogTailOptions opts) throws P4JavaException;
	
	/**
	 * Gets the error/fatal message from the passed-in Perforce command results
	 * map. If no error/fatal message found in the results map it returns null. </p>
	 * 
	 * Note that the minimum severity code is MessageSeverityCode.E_FAILED. Therefore,
	 * only message with severity code >= MessageSeverityCode.E_FAILED will be returned. <p>
	 * 
     * RPC impl errors come across the wire as a map in the form usually like this:
     * <pre>
     * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
     * func=client-Message, user=nouser, code0=822483067
     * </pre>
     * 
     * Note that the code0 entry will be used to get the severity level; the fmt0
     * entry contains the message. <p>
	 *
	 * Updated for p4ic4idea to return an IServerMessage instead of a String.
	 * <p>
	 *
	 * @since 2011.2
	 * @param map Perforce command results map
	 * @return possibly-null error/fatal string
	 */
	IServerMessage getErrorStr(Map<String, Object> map);
	
	/**
	 * Gets the info/warning/error/fatal message from the passed-in Perforce
	 * command results map. If no info/warning/error/fatal message found in the
	 * results map it returns null. </p>
	 * 
	 * Note that the minimum severity code is MessageSeverityCode.E_INFO. Therefore,
	 * only message with severity code >= MessageSeverityCode.E_INFO will be returned. <p>
	 * 
     * RPC impl errors come across the wire as a map in the form usually like this:
     * <pre>
     * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
     * func=client-Message, user=nouser, code0=822483067
     * </pre>
     * 
     * Note that the code0 entry will be used to get the severity level; the fmt0
     * entry contains the message. <p>
	 *
	 * Updated by p4ic4idea to return a generic message that includes the input data
	 * that constructed the message, so that customized localization can occur.<p>
	 * 
	 * @since 2011.2
	 * @param map Perforce command results map
	 * @return possibly-null info/warning/error/fatal string
	 */
	IServerMessage getErrorOrInfoStr(Map<String, Object> map);
	
	/**
	 * Gets the info message from the passed-in Perforce command results map.
	 * If no info message found in the results map it returns null. </p>
	 * 
	 * Note that the severity code is MessageSeverityCode.E_INFO. Therefore, only
	 * message with severity code = MessageSeverityCode.E_INFO will be returned. <p>
	 * 
     * RPC impl errors come across the wire as a map in the form usually like this:
     * <pre>
     * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
     * func=client-Message, user=nouser, code0=822483067
     * </pre>
     * 
     * Note that the code0 entry will be used to get the severity level; the fmt0
     * entry contains the message. <p>
	 * 
	 * @since 2011.2
	 * @param map Perforce command results map
	 * @return possibly-null info string
	 */
	String getInfoStr(Map<String, Object> map);
	
	/**
	 * Return the fingerprint for the Perforce SSL connection.<p>
	 * 
	 * Note that this fingerprint is generated from the connection, it may not
	 * be the same as the one (if any) stored in the trust file.
	 * 
	 * @since 2012.1
	 * @return possibly-null fingerprint for the Perforce SSL connection.
	 * @throws P4JavaException if an error occurs processing this method and its
	 * 				parameters.
	 */
	String getTrust() throws P4JavaException;

	/**
	 * Approve and add the fingerprint for the Perforce SSL connection. The
	 * fingerprint or replacement will be stored in the trust file. If the
	 * attribute TrustOptions.isReplacement() is true, then the replacement
	 * fingerprint will be stored. Otherwise, the normal fingerprint is stored.<p>
	 * 
	 * Note that an exception would be thrown if there is an identity change
	 * detected. If you want to trust the new key use the 'force' option.
	 *
	 * @since 2012.1
	 * @param opts TrustOptions object describing optional parameters; if null,
	 *             no options are set.
	 * @return non-null result message string from the trust operation; this may
	 * include the fingerprint for the Perforce server public key.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	String addTrust(TrustOptions opts) throws P4JavaException;

	/**
	 * Approve and add the specified fingerprint for the Perforce SSL
	 * connection. The fingerprint will be stored in the trust file.
	 *
	 * @since 2012.1
	 * @param fingerprintValue non-null fingerprint value to be added.
	 * @return non-null result message string from the trust operation; this may
	 * include the fingerprint for the Perforce server public key.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	String addTrust(String fingerprintValue) throws P4JavaException;

	/**
	 * Approve and add the specified fingerprint or replacement for the Perforce
	 * SSL connection. The fingerprint or replacement will be stored in the trust
	 * file. If the attribute TrustOptions.isReplacement() is true, then the
	 * replacement fingerprint will be stored. Otherwise, the normal fingerprint
	 * is stored.
	 *
	 * @since 2015.1
	 * @param fingerprintValue non-null fingerprint value to be added.
	 * @param opts             TrustOptions object describing optional parameters; if null,
	 *                         no options are set.
	 * @return non-null result message string from the trust operation; this may
	 * include the fingerprint for the Perforce server public key.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	String addTrust(String fingerprintValue, TrustOptions opts) throws P4JavaException;

	/**
	 * Remove the fingerprint for the Perforce SSL connection. The fingerprint
	 * will removed from the trust file.
	 *
	 * @since 2012.1
	 * @return non-null result message string from the trust operation; this may
	 * include the fingerprint for the Perforce server public key.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	String removeTrust() throws P4JavaException;

	/**
	 * Remove the fingerprint or replacement for the Perforce SSL connection. The
	 * fingerprint or replacement will removed from the trust file. If the attribute
	 * TrustOptions.isReplacement() is true, then the replacement fingerprint will
	 * be removed. Otherwise the normal fingerprint is removed.
	 *
	 * @since 2015.1
	 * @param opts TrustOptions object describing optional parameters; if null,
	 *             no options are set.
	 * @return non-null result message string from the trust operation; this may
	 * include the fingerprint for the Perforce server public key.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	String removeTrust(TrustOptions opts) throws P4JavaException;

	/**
	 * List all fingerprints in the trust file.
	 *
	 * @since 2012.1
	 * @return non-null list of known fingerprints in the trust file.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	List<Fingerprint> getTrusts() throws P4JavaException;

	/**
	 * List all fingerprints or replacements in the trust file. If the attribute
	 * TrustOptions.isReplacement() is true, then replacement fingerprints will
	 * be returned. Otherwise, normal fingerprints are returned.
	 *
	 * @since 2015.1
	 * @param opts TrustOptions object describing optional parameters; if null,
	 *             no options are set.
	 * @return non-null list of fingerprints in the trust file.
	 * @throws P4JavaException if an error occurs processing this method and its
	 *                         parameters.
	 */
	List<Fingerprint> getTrusts(TrustOptions opts) throws P4JavaException;

	/**
	 * Return a list of Perforce server processes active on the Perforce server.
	 * 
	 * @since 2012.2
	 * @param opts GetServerProcessesOptions object describing optional parameters;
	 * 				if null, no options are set.
	 * @return non-null but possibly-empty list of IServerProcess objects
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IServerProcess> getServerProcesses(GetServerProcessesOptions opts) throws P4JavaException;
	
	
	/**
	 * Duplicate revisions with integration history (unsupported).<p>
	 * 
	 * Duplicate revisions as if they always existed. All aspects of the source
	 * revisions are mirrored to the target revisions, including changelist
	 * number, date, attributes, and contents. The target revision must not
	 * already exist and the target file must not be opened (for any operation)
	 * on any client.<p>
	 * 
	 * Note that integration records are duplicated as well. 'p4 duplicate'
	 * followed by a 'p4 obliterate' (of the source revisions) is in effect a
	 * deep rename operation, with any source revision in client workspace or
	 * labels forgotten. The full semantics of this operation are found in the
	 * main 'p4 help duplicate' documentation.
	 * 
	 * @since 2012.2
	 * @param fromFile non-null source file.
	 * @param toFile non-null target file.
	 * @param opts possibly-null CopyFilesOptions object specifying method options.
	 * @return non-null but possibly empty list of duplicated file info/error messages.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IFileSpec> duplicateRevisions(IFileSpec fromFile, IFileSpec toFile,
						DuplicateRevisionsOptions opts) throws P4JavaException;
	
	/**
	 * Unloads a client or label to the unload depot.
	 * <p>
	 * 
	 * Note that by default, users can only unload their own clients or labels.
	 * The -f flag requires 'admin' access, which is granted by 'p4 protect'.
	 * The full semantics of this operation are found in the main 'p4 help
	 * unload' documentation.
	 * 
	 * @since 2012.3
	 * @param opts
	 *            possibly-null UnloadOptions object specifying method options.
	 * @return non-null result message string from the unload operation.
	 * @throws P4JavaException
	 *             if an error occurs processing this method and its parameters.
	 */
	String unload(UnloadOptions opts) throws P4JavaException;

	/**
	 * Reload an unloaded client or label.
	 * <p>
	 * 
	 * Note that by default, users can only unload their own clients or labels.
	 * The -f flag requires 'admin' access, which is granted by 'p4 protect'.
	 * The full semantics of this operation are found in the main 'p4 help
	 * unload' documentation.
	 * 
	 * @since 2012.3
	 * @param opts
	 *            possibly-null ReloadOptions object specifying method options.
	 * @return non-null result message string from the reload operation.
	 * @throws P4JavaException
	 *             if an error occurs processing this method and its parameters.
	 */
	String reload(ReloadOptions opts) throws P4JavaException;

	/**
	 * Get a map of the Perforce server's keys.
	 * 
	 * @since 2013.1
	 * @return a non-null (but possibly empty) map of keys.
	 * @param opts GetKeysOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	Map<String, String> getKeys(GetKeysOptions opts) throws P4JavaException;

	/**
	 * Create, set or delete a key on a Perforce server. This method can be used to
	 * create, set, increment, or delete a key according to the specific options
	 * set in the associated options object.
	 * 
	 * @since 2013.1
	 * @param keyName non-null key name.
	 * @param value value the key should be set to; can be null if the set operation
	 * 				is an increment.
	 * @param opts KeyOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return possibly-null current (post-set, post-increment) value; may be empty if the
	 * 				operation was a delete.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String setKey(String keyName, String value, KeyOptions opts) throws P4JavaException;

	/**
	 * Get the value of a named Perforce key from the Perforce server. Note that this
	 * method will return a zero string (i.e. "0") if the named key doesn't exist (rather
	 * than throw an exception); use getKeys to see if a key actually exists before
	 * you use it.
	 * 
	 * @since 2013.1
	 * @param keyName non-null key name.
	 * @return non-null (but possibly zero, if non-existing) key value associated
	 * 				with keyName.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String getKey(String keyName) throws P4JavaException;

	/**
	 * Delete a key on a Perforce server.
	 * 
	 * @since 2013.1
	 * @param keyName non-null key name.
	 * @return non-null result message string (empty) from the delete operation.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String deleteKey(String keyName) throws P4JavaException;
	
	/**
	 * Search for jobs that contain the specified words in the search engine's index.<p>
	 * 
	 * Note that this is an 'undoc' Perforce command.<p>
	 * 
	 * See also 'p4 help index'.
	 * 
	 * @since 2013.1
	 * @param words non-null words to be searched.
	 * @param opts SearchJobsOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null (but possibly-empty) list of job IDs.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<String> searchJobs(String words, SearchJobsOptions opts) throws P4JavaException;

	/**
	 * Gets a list of one or more property values from the Perforce server.<p>
	 * 
	 * The -A flag require that the user have 'admin' access granted by 'p4 protect'.<p>
	 * 
	 * Note that specifying the -n flag when using the -l flag substantially
	 * improves the performance of this command.
	 * 
	 * @since 2013.1
	 * @param opts GetPropertyOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return a non-null (but possibly empty) list of property values.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IProperty> getProperty(GetPropertyOptions opts) throws P4JavaException;

	/**
	 * Updates a property value in the Perforce server, or adds the property value
	 * to the Perforce server if it is not yet there.<p>
	 * 
	 * This method require that the user have 'admin' access granted by 'p4 protect'.
	 * 
	 * @since 2013.1
	 * @param name non-null property name.
	 * @param value property value.
	 * @param opts PropertyOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null result message string from the set (add/update) operation.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String setProperty(String name, String value, PropertyOptions opts) throws P4JavaException;

	/**
	 * Deletes a property value from the Perforce server.<p>
	 * 
	 * This method require that the user have 'admin' access granted by 'p4 protect'.
	 * 
	 * @since 2013.1
	 * @param name non-null property name.
	 * @param opts PropertyOptions object describing optional parameters; if null, no
	 * 				options are set.
	 * @return non-null result message string from the delete operation.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String deleteProperty(String name, PropertyOptions opts) throws P4JavaException;

	/**
	 * Gets a list of file sizes for one or more files in the depot.<p>
	 * 
	 * For specified file specification, get the depot file name, revision, file
	 * count and file size. If you use client syntax for the file specification,
	 * the view mapping is used to list the corresponding depot files.
	 * 
	 * @since 2013.2
	 * @param fileSpecs filespecs to be processed; if null or empty, an empty list
	 * 				is returned.
	 * @param opts GetFileSizesOptions object describing optional parameters; if null,
	 * 				no options are set.
	 * @return a non-null (but possibly empty) list of file sizes.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	List<IFileSize> getFileSizes(List<IFileSpec> fileSpecs,	GetFileSizesOptions opts) throws P4JavaException;

	/**
	 * Turns on/off journal-wait. The client application can specify "noWait"
	 * replication when using a forwarding replica or an edge server.<p>
	 * 
	 * Note that this method uses a deep undoc 'p4 journalwait [-i]' command.<p>
	 * 
	 * @since 2013.2
	 * @param opts JournalWaitOptions object describing optional parameters;
	 *              if null, no options are set.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	void journalWait(JournalWaitOptions opts) throws P4JavaException;

	/**
	 * Get an InputStream onto the entries of the Perforce triggers table.<p>
	 * 
	 * This method require that the user have 'super' access granted by 'p4 protect'.
	 * 
	 * @since 2014.1
	 * @return a non-null but possibly empty InputStream onto the triggers table's entries.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	InputStream getTriggersTable() throws P4JavaException;

	/**
	 * Get a list of Perforce trigger entries.<p>
	 * 
	 * This method require that the user have 'super' access granted by 'p4 protect'.
	 * 
	 * @since 2014.1
	 * @return non-null but possibly empty list of trigger entries.
	 * @throws P4JavaException if any error occurs in the processing of this method.
	 */
	List<ITriggerEntry> getTriggerEntries() throws P4JavaException;
	
	/**
	 * Create or replace the triggers table data on the Perforce server with
	 * these new trigger entries.<p>
	 * 
	 * This method require that the user have 'super' access granted by 'p4 protect'.<p>
	 * 
	 * Warning: this will overwrite the existing triggers table data.
	 * 
	 * @since 2014.1
	 * @param entryList non-null list of trigger entries.
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String createTriggerEntries(List<ITriggerEntry> entryList) throws P4JavaException;
	
	/**
	 * Replace the triggers table data on the Perforce server with these new
	 * triggers entries.<p>
	 * 
	 * This method require that the user have 'super' access granted by 'p4 protect'.<p>
	 * 
	 * Warning: this will overwrite the existing triggers table data.
	 * 
	 * @since 2014.1
	 * @param entryList non-null list of trigger entries.
	 * @return possibly-null status message string as returned from the server
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	String updateTriggerEntries(List<ITriggerEntry> entryList) throws P4JavaException;
	
	/**
	 * Get a list of shelved files associated with a Perforce pending changelist.<p>
	 * 
	 * @since 2014.1
	 * @param changelistId numeric pending changelist identifier
	 * @return non-null (but possibly empty) list of shelved files associated with the pending changelist.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IFileSpec> getShelvedFiles(int changelistId) throws P4JavaException;

	/**
	 * Verify that the server archives are intact.<p>
	 * 
	 * This method require that the user be an operator or have 'admin' access,
	 * which is granted by 'p4 protect'.
	 * 
	 * @since 2014.1
	 * @param fileSpecs filespecs to be processed; if null or empty, an empty list
	 * 				is returned.
	 * @param opts VerifyFilesOptions object describing optional parameters; if null,
	 * 				no options are set.
	 * @return non-null (but possibly empty) list of files with revision-specific
	 * 				information and an MD5 digest of the revision's contents.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	List<IExtendedFileSpec> verifyFiles(List<IFileSpec> fileSpecs,	VerifyFilesOptions opts) throws P4JavaException;
}
