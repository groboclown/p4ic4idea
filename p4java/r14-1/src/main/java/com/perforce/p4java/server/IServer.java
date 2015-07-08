/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.server;

import java.io.InputStream;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IProgressCallback;
import com.perforce.p4java.server.callback.ISSOCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * Provides an interface onto a Perforce SCM server.<p>
 * 
 * This is the main interface for Perforce services that are typically Perforce client
 * workspace-independent, or that affect entire Perforce depots or servers. Some of these
 * services are also available through various client, job, changelist, etc., interfaces
 * methods, but in general, most Perforce services are always available through methods
 * on this interface as well.<p>
 * 
 * IServer interfaces for specific Perforce servers are issued by the P4Javs server factory
 * class, ServerFactory; the factory can return interfaces that use a small variety
 * of communication protocols to access the Perforce server.
 * 
 * @see com.perforce.p4java.server.ServerFactory
 */

public interface IServer {
	
	/**
	 * Return the Java properties associated with this server. The Properties
	 * returned here are the actual properties used in the server and can be updated
	 * through this method (i.e. the object is not just a copy). The interpretation
	 * of the individual Properties are implementation-specific and not discussed
	 * here.
	 * 
	 * @return Properties object; may be empty but will not be null.
	 */
	
	Properties getProperties();

	/**
	 * Register a P4Java command callback with this Perforce server.<p>
	 * 
	 * See the ICommandCallback javadocs for callback semantics. Note that only
	 * one command callback can be active and registered for a given server at any
	 * one time.
	 * 
	 * @param callback ICommandCallback object to be registered; if null, command
	 * 			callbacks are disabled.
	 * @return the previous command callback, if it existed; null otherwise
	 */
	
	ICommandCallback registerCallback(ICommandCallback callback);
	
	/**
	 * Register a P4Java command progress callback with this Perforce server.<p>
	 * 
	 * See the IProgressCallback javadocs for callback semantics. Note that only
	 * one progress callback can be active and registered for a given server at any
	 * one time.
	 * 
	 * @param callback IProgressCallback object to be registered; if null, progress
	 * 			callbacks are disabled.
	 * @return the previous progress callback, if it existed; null otherwise
	 */
	
	IProgressCallback registerProgressCallback(IProgressCallback callback);
	
	/**
	 * Register a Perforce Single Sign On (SSO) callback and key for this server.<p>
	 * 
	 * See the ISSOCallback Javadoc comments for an explanation of the SSO
	 * callback feature; note that only one SSO callback can be active and
	 * registered for a given P4Jserver object at any one time.<p>
	 * 
	 * Note that SSO callbacks work only with the (default) pure Java (RPC)
	 * protocol implementation.
	 * 
	 * @param callback ISSOCallback object to be registered; if null, SSO
	 * 			callbacks are disabled.
	 * @param ssoKey opaque string to be passed untouched to the callback; can
	 * 			be null, in which case null is passed in to the callback
	 * @return the previous SSO callback, if it existed; null otherwise
	 */
	
	ISSOCallback registerSSOCallback(ISSOCallback callback, String ssoKey);

	/**
	 * Return the current status of this server object.
	 * 
	 * @return non-null ServerStatus representing the server status.
	 */
	
	ServerStatus getStatus();
	
	/**
	 * Set the Perforce server's charset to the passed-in charset name. The semantics
	 * of this are described in the full Perforce documentation, but note that odd things
	 * will happen if the named charset isn't recognized by both the JVM and the Perforce server
	 * (i.e. "utf8" works fine, but bizarre variants may not). What constitutes a good
	 * charset name, and whether or not the server recognises it, is somewhat fraught and
	 * may involve retrieving the unicode counter and using the (printed) list of recognised
	 * charsets.
	 * 
	 * @param charsetName charset name; if null, resets the charset to "no charset".
	 * @return true if the attempt to set the charset name succeeded; false otherwise. False
	 * 			will only be returned if the JVM doesn't support the charset. (an exception
	 * 			will be thrown if the server doesn't recognize it).
	 * @throws UnsupportedCharsetException if the Perforce server doesn't
	 * 			support or recognize the charset name.
	 */
	
	boolean setCharsetName(String charsetName) throws UnsupportedCharsetException;
	
	/**
	 * Get the current charset name for the server connection. May be null, in which
	 * case there is no associated character set.
	 * 
	 * @return charset name associated with this server; may be null.
	 */
	
	String getCharsetName();
	
	/**
	 * Get the Perforce version number of the Perforce server associated with this
	 * IServer object, if any. This will be in the form 20092 or 20073 (corresponding
	 * to 2009.2 and 2007.3 respectively), but the version number will not be available
	 * if you're not actually connected to a Perforce server.
	 * 
	 * @return positive integer version number or -1 if not known or unavailable.
	 */
	
	int getServerVersionNumber();
	
	/**
	 * Returns whether the Perforce server associated with this IServer object
	 * is case sensitive.
	 * 
	 * @return - true if case sensitive, false if case insensitive.
	 */
	boolean isCaseSensitive();
	
	/**
	 * Return true if the underlying Perforce server supports Unicode (and is connected).
	 * In this context "supporting unicode" simply means that the method was able to contact the
	 * server and retrieve a "true" unicode-enabled status using the info command.<p>
	 * 
	 * @return true iff the underlying server supports Unicode.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	boolean supportsUnicode() throws ConnectionException,
					RequestException, AccessException;
	
	/**
	 * Return true IFF the underlying Perforce server supports the new 2009.1
	 * and later "smart move" command. Note that this returns whether the
	 * server can support moves only at the time the server is first created;
	 * it's entirely possible for the underlying server to change versions, etc.,
	 * under the user in the meanitme or over time. In any case, if you do try
	 * to run a move command on such a server, the results will be safe, if not
	 * entirely what you expected.  As of 2010.2 it also possible for the server
	 * to be configured to disable the move command, in which case this function
	 * will return false.
	 * 
	 * @return true iff the server supports the smart move command.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	boolean supportsSmartMove()  throws ConnectionException,
					RequestException, AccessException;
	
	/**
	 * Return an array of strings representing "known" charsets (e.g. "utf8" or
	 * "utf32le".<p>
	 * 
	 * Note that in this context, "known" simply means that Perforce servers supported by
	 * this API can potentially recognize the charset name and (hopefully) act accordingly.<p>
	 * 
	 * Charset support in Perforce is described in more detail in the main p4
	 * command documentation; in summary, although the list returned here is
	 * comprehensive and quite impressive, unless the Perforce server is actually
	 * primed to cope with Unicode (which, by default, they're not), the only
	 * charset listed here that will work is "none"; furthermore, actual charset
	 * support is somewhat idiosyncratic -- please refer to specific documentation
	 * for guidance with this. You probably need to use this method in conjunction
	 * with the supportsUnicode() method above.
	 * 
	 * @return a non-null array of strings representing lower-case charset names
	 * 			known to the server.
	 */

	String[] getKnownCharsets();
	
	/**
	 * Set the Perforce user name to be used with this server. This does not perform
	 * any login or checking, just associates a user name with this session. Once
	 * set, the user name is used with all commands where it makes sense.<p>
	 * 
	 * Note that the auth ticket (if available) for this user will also be set
	 * to this server instance.
	 * 
	 * @param userName Perforce user name; can be null, which is interpreted
	 * 			as "don't associate a user name with this server".
	 */
	
	void setUserName(String userName);
	
	/**
	 * Set the server's Perforce authentication ticket to the passed-in string.
	 * If the string is null, auth tickets won't be used when talking to the
	 * associated Perforce server; otherwise, the auth ticket will be used to
	 * authenticate against the Perforce server for each call to the server.<p>
	 * 
	 * No checking is performed on the passed-in ticket, and any changes to
	 * existing tickets can cause authentication failures, so you should
	 * ensure the passed-in ticket is valid and makes sense for the current
	 * context.
	 * 
	 * @param authTicket possibly-null Perforce authentication ticket
	 */
	
	void setAuthTicket(String authTicket);
	
	/**
	 * Return the current Perforce authentication ticket being used by
	 * this server, if any. This ticket is not always guaranteed to be
	 * currently valid, so reuse should be done carefully.
	 * 
	 * @return possibly-null Perforce authentication ticket
	 */
	
	String getAuthTicket();
	
	/**
	 * Set the Perforce server's idea of each command's working directory.
	 * This affects all commands on this server from this point on, and
	 * the passed-in path should be both absolute and valid, otherwise
	 * strange errors may appear from the server. If dirPath is null,
	 * the Java VM's actual current working directory is used instead
	 * (which is almost always a safe option unless you're using Perforce
	 * alt roots).<p>
	 * 
	 * Note: no checking is done at call time for correctness (or otherwise)
	 * of the passed-in path.
	 * 
	 * @param dirPath absolute path of directory to be used, or null
	 */
	void setWorkingDirectory(String dirPath);
	
	/**
	 * Get the underlying server's notion of the current working directory.
	 * If this method returns null, the server is using the JVM's current
	 * working directory, typically available as the System user.dir
	 * property.
	 *  
	 * @return current working directory path, or null if not set
	 */
	String getWorkingDirectory();
	
	/**
	 * Return the user name currently associated with this server, if any.
	 * User names are set using the setUserName method.
	 * 
	 * @return the user name currently associated with this server, if any;
	 * 			null otherwise.
	 */
	
	String getUserName();
	
	/**
	 * Connect to the Perforce server associated with this server object.<p>
	 * 
	 * This method's detailed semantics depend on the underlying transport
	 * implementation, but in general, it's intended to be called before
	 * any attempt is made to issue a command to the associated Perforce
	 * server. It's also intended to be called after any (intentional or
	 * accidental) disconnect.<p>
	 * 
	 * Note that certain implementations may try to request a client, etc.,
	 * on connection (in response to property values passed in through the
	 * URL, etc.), which may cause a RequestException to be generated.
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws AccessException if the Perforce server denies access to the caller 
	 * @throws RequestException  if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws ConfigException if local I/O exception occurs
	 */
	
	void connect() throws ConnectionException, AccessException, RequestException, ConfigException;
	
	/**
	 * Return true iff and the server object is connected to the associated
	 * Perforce server.<p>
	 * 
	 * The meaning of "connected" is generally dependent on the underlying
	 * transport layer, but in general, if the server is not connected,
	 * issuing server commands to the associated Perforce server will fail
	 * with a connection exception.
	 * 
	 * @return - true iff connected, false otherwise
	 */
	
	boolean isConnected();
	
	/**
	 * Disconnect from this Perforce server. Does not affect the current
	 * IServer's current user, password, or client settings, but if you
	 * later reconnect to the same Perforce server, you may also need to re-login.<p>
	 * 
	 * This command should be run at the point at which this server is not
	 * going to be used any more, and attempts to disconnect from the associated
	 * server. "Disconnect" here has different meanings according to the underlying
	 * transport mechanism, but in practice it will mean that attempting to use this
	 * server object to issue Perforce commands will fail, usually with a
	 * ConnectionException exception.
	 */
	
	void disconnect() throws ConnectionException, AccessException;
	
	/**
	 * Log the current user (if any) in to a Perforce server, optionally
	 * arranging to be logged in for all hosts.<p>
	 * 
	 * Attempts to log in to the underlying Perforce server. If successful,
	 * successive calls to server-side services will succeed until the session
	 * is terminated by the server or the user logs out.<p>
	 * 
	 * Behaviour is undefined if the server's user name attribute is null (but
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
	 * @param allHosts if true, perform the equivalent of a "login -a"
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 * @throws ConfigException if the p4tickets file could not be updated successfully
	 */

	void login(String password, boolean allHosts) throws ConnectionException, RequestException, 
										AccessException, ConfigException;
	
	/**
	 * Convenience method for login(password, false).
	 * 
	 * @param password Perforce password; can be null if no password is needed (as
	 * 				in the case of SSO logins)
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 * @throws ConfigException if the p4tickets file could not be updated successfully
	 */
	
	void login(String password) throws ConnectionException, RequestException, 
										AccessException, ConfigException;

	/**
	 * Return a string indicating the current login status; corresponds
	 * to the p4 login -s command. The resulting string should be interpreted
	 * by the caller, but is typically something like "User p4jtestsuper ticket
	 * expires in 9 hours 42 minutes." or "'login' not necessary, no password set
	 * for this user." or "Perforce password (P4PASSWD) invalid or unset." or
	 * "Access for user 'p4jtestinvaliduser' has not been enabled by 'p4 protect'",
	 * etc.
	 * 
	 * @return non-null, but possibly-empty ticket / login status string.
	 * 			Interpretation of this string is up to the caller.
	 * @throws P4JavaException if any errors occur during the processing of
	 * 			this command.
	 */
	
	String getLoginStatus() throws P4JavaException;
	
	/**
	 * Log the current Perforce user out of a Perforce server session.<p>
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 * @throws ConfigException if the p4tickets file could not be updated successfully
	 */
	
	void logout() throws ConnectionException, RequestException, AccessException, ConfigException;
	
	/**
	 * Return a snapshot set of data on the Perforce server associated with
	 * this server interface. If the server has been disconnected, this method
	 * will throw a suitable ConnectionException.
	 * 
	 * @return non-null IServerInfo interface.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	IServerInfo getServerInfo() throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of all Perforce depots known to this Perforce server.
	 * 
	 * @return non-null (but possibly empty) list of non-null IDepot
	 * 			objects representing the underlying Perforce depots.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	List<IDepot> getDepots() throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the user details of a specific Perforce user from the Perforce server.
	 * 
	 * @param userName if null, get the current user details, otherwise use the
	 * 				passed-in user name.
	 * @return IUser details for the user, or null if no such user is known. 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	IUser getUser(String userName) throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Create a new Perforce user on the Perforce server.
	 * 
	 * @param user non-null IUser defining the new user to be created.
	 * @param force if true, force the creation of any named user; requires admin
	 * 				privileges,
	 * @return possibly-null status message string as returned from the server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	String createUser(IUser user, boolean force)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update a Perforce user on the Perforce server.
	 * 
	 * @param user non-null IUser defining the user to be updated
	 * @param force if true, force update for users other than the caller. Requires
	 * 				super user / admin privileges (enforced by the server).
	 * @return possibly-null status message string as returned from the server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	String updateUser(IUser user, boolean force)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a named Perforce user from the Perforce server.
	 * 
	 * @param userName non-null name of the user to be deleted.
	 * @param force if true, force deletion for users other than the caller. Requires
	 * 				super user / admin privileges (enforced by the server).
	 * @return possibly-null status message string as returned from the server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String deleteUser(String userName, boolean force)
						throws ConnectionException, RequestException, AccessException;
	
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
	 * @param maxUsers if positive, only return the first maxUsers users.
	 * @return non-null (but possibly empty) list of non-null IUserSummary
	 * 			objects representing the underlying Perforce users (if any).
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	List<IUserSummary> getUsers(List<String> userList, int maxUsers)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of Perforce user groups from the server.<p>
	 * 
	 * Note that the Perforce server considers it an error to have both indirect and
	 * displayValues parameters set true; this will cause the server to throw a
	 * RequestException with an appropriate usage message.
	 * 
	 * @param userOrGroupName if non-null, restrict the list to the specified group or username.
	 * @param indirect if true, also displays groups that the specified user or group belongs
	 * 			to indirectly via subgroups.
	 * @param displayValues if true, display the MaxResults, MaxScanRows, MaxLockTime, 
	 * 			and Timeout values for the named group.
	 * @param maxGroups if > 0, display only the first m results.
	 * @return a non-zero but possibly-empty list of qualifying groups.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request.
	 * @throws AccessException if the Perforce server denies access to the caller.
	 */
	
	List<IUserGroup> getUserGroups(String userOrGroupName, boolean indirect, boolean displayValues,
							int maxGroups)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the named Perforce user group. Note that since the Perforce server usually
	 * interprets asking for a non-existent group as equivalent to asking for a template
	 * for a new user group, you will normally always get back a result here. It is
	 * best to first use the getUserGroups method to see if the group exists, then
	 * use this method to retrieve a specific group once you know it exists.
	 * 
	 * @param name non-null group name.
	 * @return IUserGroup representing the named user group if it exists on the server;
	 * 				null otherwise (but see note in main comments above).
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request.
	 * @throws AccessException if the Perforce server denies access to the caller.
	 */
	
	IUserGroup getUserGroup(String name) 
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Create a new Perforce user group on the Perforce server.
	 * 
	 * @param group non-null IUserGroup to be created.
	 * @return possibly-null status message string as returned from the server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request.
	 * @throws AccessException if the Perforce server denies access to the caller.
	 */
	
	String createUserGroup(IUserGroup group)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update a Perforce user group on the Perforce server.
	 * 
	 * @param group non-null user group to be updated.
	 * @param updateIfOwner if true, allows a user without 'super'
	 * 				access to modify the group only if that user is an
	 * 				'owner' of that group.
	 * @return possibly-null status message string as returned from the server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request.
	 * @throws AccessException if the Perforce server denies access to the caller.
	 */
	
	String updateUserGroup(IUserGroup group, boolean updateIfOwner)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a Perforce user group from the Perforce server.
	 * 
	 * @param group non-null group to be deleted.
	 * @return possibly-null status message string as returned from the server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request.
	 * @throws AccessException if the Perforce server denies access to the caller.
	 */
	
	String deleteUserGroup(IUserGroup group)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of Perforce protection entries for the passed-in arguments.<p>
	 * 
	 * Note that the behavior of this method is unspecified when using clashing
	 * options (e.g. having both userName and groupName set non-null). Consult the
	 * main Perforce admin documentation for semantics and usage.<p>
	 * 
	 * Note that the annotations in the file paths will be dropped. The reason is
	 * the Perforce server 'protects' command requires a file list devoid of annotated
	 * revision specificity.
	 * 
	 * @param allUsers if true, protection lines for all users are displayed.
	 * @param hostName only protection entries that apply to the given host (IP address)
	 * 				are displayed.
	 * @param userName protection lines Perforce user "userName" are displayed.
	 * @param groupName protection lines for Perforce group "groupName" are displayed.
	 * @param fileList if non-null, only those protection entries that apply to the
	 * 				specified files are displayed.
	 * @return non-null but possibly empty list of protection entries.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request.
	 * @throws AccessException if the Perforce server denies access to the caller.
	 */
	
	List<IProtectionEntry> getProtectionEntries(boolean allUsers, String hostName,
										String userName, String groupName,
										List<IFileSpec> fileList)
						throws ConnectionException, RequestException, AccessException;

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
	 * @param userName if not null, restrict listings to clients owned by the user 'userName'
	 * @param queryString if not null, limits output to clients whose name matches the query
	 * 				pattern passed-in.
	 * 				Note this option does not work for earlier Perforce servers.
	 * @param maxResults if > 0, restrict output to the first maxResults results.
	 * @return non-null (but possibly empty) list of Client objects for Perforce clients
	 * 				known to this Perforce server.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	List<IClientSummary> getClients(String userName, String queryString, int maxResults)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of Perforce labels, optionally tied to a specific set of files.<p>
	 * 
	 * Note that the ILabel objects returned here do not have views associated with
	 * them (i.e. the getViewMapping() method will return an empty list. If you need
	 * to get the view mapping for a specific label, use the getLabel() method.
	 * 
	 * @param user if non-null, limit labels to those owned by the named user
	 * @param maxLabels if larger than zero, return only the first maxLabels
	 * 				(or fewer) qualifying labels
	 * @param nameFilter if not null, limits output to labels whose name matches
	 *				the nameFilter pattern, e.g. -e 'svr-dev-rel*'
	 * @param fileList if not null, limits its report to labels that contain those files
	 * @return non-null (but possibly-empty) list of qualifying Perforce labels
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	List<ILabelSummary> getLabels(String user, int maxLabels, String nameFilter,
											List<IFileSpec> fileList)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a specific named Perforce label.<p>
	 * 
	 * Unlike the getLabelList method, the getViewMapping method on the returned
	 * label will be valid. Note though that changes to the returned label or its
	 * view will not be reflected on to the server unless the updateLabel method
	 * is called with the label as an argument.
	 * 
	 * @param labelName non-null label name
	 * @return ILabel representing the associated Perforce label, or null if no
	 * 				such label exists on the server.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	ILabel getLabel(String labelName)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Create a new Perforce label in the Perforce server.
	 * 
	 * @param label non-null ILabel to be saved
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String createLabel(ILabel label)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update an existing Perforce label in the Perforce server.
	 * 
	 * @param label non-null ILabel to be updated
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String updateLabel(ILabel label)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a named Perforce label from the Perforce server.
	 * 
	 * @param labelName non-null label name
	 * @param force if true, forces the deletion of any label; normally labels
	 *			can only be deleted by their owner
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	String deleteLabel(String labelName, boolean force)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Tag files with a Perforce label.
	 * 
	 * @param fileSpecs non-null list of files to be tagged.
	 * @param labelName non-null label name to use for the tagging.
	 * @param listOnly if true, don't do the actual tag, just return the list of files that
	 * 				would have been tagged.
	 * @param delete if true, delete the label tag from the files.
	 * @return a non-null (but possibly empty) list of affected file specs
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	List<IFileSpec> tagFiles(List<IFileSpec> fileSpecs, String labelName,
								boolean listOnly, boolean delete)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of all summary Perforce branch specs known to the Perforce server.<p>
	 * 
	 * Note that the IBranchSpecSummary objects returned here do not have branch
	 * view specs; you must call the getBranchSpec method on a specific branch to get
	 * valid view specs for a branch.
	 * 
	 * @param userName if non-null, limit qualifying branches to those owned by the named user.
	 * @param nameFilter if non-null, limits output to branches whose name matches
	 * 			the nameFilter pattern.
	 * @param maxReturns if greater than zero, limit output to the first maxReturns
	 * 			number of branches.
	 * @return non-null (but possibly-empty) list of IBranchSpecSummary objects.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IBranchSpecSummary> getBranchSpecs(String userName, String nameFilter, int maxReturns)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a specific named Perforce branch spec from the Perforce server.<p>
	 * Note that since the Perforce server usually interprets asking for a non-existent
	 * branch spec as equivalent to asking for a template for a new branch spec,
	 * you will normally always get back a result here. It is best to first use
	 * the getBranchSpecList method to see if the branch spec exists, then
	 * use this method to retrieve a specific branch spec once you know it exists.
	 * 
	 * @param name non-null branch name
	 * @return potentially-null IBranchSpec for the named Perforce branch spec.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	IBranchSpec getBranchSpec(String name) 
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Create a new Perforce branch spec on the Perforce server.
	 * 
	 * @param branchSpec non-null branch spec to be created.
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String createBranchSpec(IBranchSpec branchSpec)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update a Perforce branch spec on the Perforce server.
	 * 
	 * @param branchSpec non-null branch spec to be updated.
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String updateBranchSpec(IBranchSpec branchSpec)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a named Perforce branch spec from the Perforce server.
	 * 
	 * @param branchSpecName non-null branch spec name
	 * @param force if true, forces the deletion of any branch; normally branches
	 *			can only be deleted by their owner
	 * @return non-null result message string from the Perforce server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	String deleteBranchSpec(String branchSpecName, boolean force)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return the Perforce client currently associated with this Perforce server, if any.
	 * 
	 * @return IClient representing the current client, or null if no client
	 * 				associated with this server.
	 */
	
	IClient getCurrentClient();
	
	/**
	 * Set the Perforce client associated with this server.
	 * 
	 * @param client
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	void setCurrentClient(IClient client)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get an IClient object for a specific named Perforce client.<p>
	 * 
	 * Note that (unfortunately) some implementations cannot detect
	 * a non-existent client at this stage, and using the client returned in such circumstances
	 * may cause errors down the road.
	 * 
	 * @param clientName non-null Perforce client name.
	 * @return IClient representing the specified Perforce client, or null if no such client.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	
	IClient getClient(String clientName)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Convenience method for getClient(clientSummary.getName()).
	 * 
	 * @param clientSummary non-null Perforce client summary object.
	 * @return IClient representing the specified Perforce client, or null if no such client.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	IClient getClient(IClientSummary clientSummary)
						throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a template of a non-existent named Perforce client. This will only
	 * return an IClient for clients that don't exist. This method is designed
	 * to be able to get the server returned default values it uses when a
	 * non-existent client is requested.
	 * 
	 * @param clientName
	 *            non-null Perforce client name.
	 * @return IClient representing the specified Perforce client, or null if
	 *         no such client.
	 * @throws ConnectionException
	 *             if the Perforce server is unreachable or is not connected.
	 * @throws RequestException
	 *             if the Perforce server encounters an error during its
	 *             processing of the request
	 * @throws AccessException
	 *             if the Perforce server denies access to the caller
	 */
	IClient getClientTemplate(String clientName)
			throws ConnectionException, RequestException,
			AccessException;
	
	/**
	 * Get a template of a non-existent named Perforce client. This will only
	 * return an IClient for clients that don't exist unless the allowExistent
	 * parameter is set to true. This method is designed to be able to get the
	 * server returned default values it uses when a non-existent client is
	 * requested.
	 * 
	 * @param clientName
	 *            non-null Perforce client name.
	 * @param allowExistent
	 *            - true to return a client even if it exists
	 * @return IClient representing the specified Perforce client, or null if
	 *         no such client.
	 * @throws ConnectionException
	 *             if the Perforce server is unreachable or is not connected.
	 * @throws RequestException
	 *             if the Perforce server encounters an error during its
	 *             processing of the request
	 * @throws AccessException
	 *             if the Perforce server denies access to the caller
	 */
	IClient getClientTemplate(String clientName, boolean allowExistent)
			throws ConnectionException, RequestException,
			AccessException;
	
	/**
	 * Attempt to create a new Perforce client (a.k.a. "workspace") in the
	 * Perforce server. The client should be fetched via
	 * {@link #getClient(String)} after this call in order to obtain the full
	 * client spec as the server may fill in defaults for missing fields in the
	 * specified newClient. This method will return a server status message and
	 * will throw an exception if the client was not created for various
	 * reasons. Note that the server status message may have form trigger
	 * output appended or prepended to it.
	 * 
	 * @return non-null result message string from the Perforce server
	 * @param newClient
	 *            non-null IClient defining the new Perforce client to be
	 *            created.
	 * @throws ConnectionException
	 *             if the Perforce server is unreachable or is not connected.
	 * @throws RequestException
	 *             if the Perforce server encounters an error during its
	 *             processing of the request
	 * @throws AccessException
	 *             if the Perforce server denies access to the caller
	 */
	String createClient(IClient newClient)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update an existing Perforce client on the current Perforce server. This client does
	 * not need to be the current client, and no association with the passed-in client is
	 * made by the server (i.e. it's not made the current client).
	 * 
	 * @param client non-null IClient defining the Perforce client to be updated
	 * @return possibly-null operation result message string from the Perforce server
 	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String updateClient(IClient client)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a Perforce client from a Perforce server. The effects this has on the client
	 * and the server are not well-defined here, and you should probably consult the relevant
	 * Perforce documentation for your specific case. In any event, you can cause quite
	 * a lot of inconvenience (and maybe even damage) doing a forced delete without preparing
	 * properly for it, especially if the client is the current client.
	 * 
	 * @param clientName non-null name of the client to be deleted from the server.
	 * @return possibly-null operation result message string from the Perforce server
	 * @param force if true, tell the server to attempt to force the delete regardless of
	 * 				the consequences. You're on your own with this one...
 	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String deleteClient(String clientName, boolean force) 
						throws ConnectionException, RequestException, AccessException;

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
	 * If allRevs is true, all revisions within the specific range, rather than just
	 * the highest revision in the range, are returned.<p>
	 * 
	 * See 'p4 help revisions' for help specifying revisions.<p>
	 * 
	 * Note that the IFileSpec objects returned will have null client and local
	 * path components.
	 * 
	 * @param fileSpecs a non-null list of one or more IFileSpecs to be used
	 * 				to qualify Perforce depot files
	 * @param allRevs if true, list all revisions of qualifying files.
	 * @return a non-null (but possible empty) list of all qualifying depot files
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> getDepotFiles(List<IFileSpec> fileSpecs, boolean allRevs)
								throws ConnectionException, AccessException;
	
	/**
	 * Get a list of revision annotations for the specified files.
	 * 
	 * @param fileSpecs non-null list of file specs to be annotated
	 * @param wsOpts DiffType describing the white space option to be used; if null,
	 * 				use default (no options), otherwise must be one of the whitespace
	 * 				options defined by the isWsOption method on DiffType.
	 * @param allResults if true, include both deleted files and lines no longer present
	 *				at the head revision
	 * @param useChangeNumbers if true, annotate with change numbers rather than
	 * 				revision numbers with each line
	 * @param followBranches if true, follow branches.
	 * @return non-null (but possibly-empty) list of IFileAnnotation objects representing
	 * 				version annotations for the passed-in file specs.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileAnnotation> getFileAnnotations(List<IFileSpec> fileSpecs, DiffType wsOpts,
					boolean allResults, boolean useChangeNumbers, boolean followBranches)
								throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Move a file already opened for edit or add (the fromFile) to the destination 
	 * file (the toFile). A file can be moved many times before it is submitted; 
	 * moving it back to its original location will reopen it for edit. The full
	 * semantics of this operation (which can be confusing) are found in the
	 * main 'p4 help' documentation.<p>
	 * 
	 * Note that this operation is not supported on servers earlier than 2009.1;
	 * any attempt to use this on earlier servers will result in a RequestException
	 * with a suitable message. Similarly, not all underlying IServer implementations
	 * will work with this either, and will also result in a suitable RequestException.<p>
	 * 
	 * Note also that the move command is special in that almost alone among Perforce
	 * file-based commands, it does not allow full filespecs with version specifiers;
	 * these are currently quietly stripped off in the move command implementation here,
	 * which may lead to unexpected behaviour if you pass in specific versions expecting
	 * them to be honoured.
	 * 
	 * @param changelistId if not IChangelist.UNKNOWN, the files are opened in the numbered
	 *			pending changelist instead of the 'default' changelist.
	 * @param listOnly if true, don't actually perform the move, just return what would
	 * 				happen if the move was performed
	 * @param noClientMove if true, bypasses the client file rename. This option can be
	 * 				used to tell the server that the user has already renamed a file on
	 * 				the client. The use of this option can confuse the server if you
	 * 				are wrong about the client's contents. Only works for 2009.2 and later
	 * 				servers; earlier servers will produce a RequestException if you set
	 * 				this true.
	 * @param fileType if not null, the file is reopened as that filetype.
	 * @param fromFile the original file; must be already open for edit.
	 * @param toFile the target file.
	 * @return list of IFileSpec objects representing the results of this move
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	List<IFileSpec> moveFile(int changelistId, boolean listOnly, boolean noClientMove,
								String fileType, IFileSpec fromFile,IFileSpec toFile)
							throws ConnectionException, RequestException, AccessException;
	
	/**
	 * List any directories matching the passed-in file specifications.
	 * 
	 * @param fileSpecs non-null list of file specifications
	 * @param clientOnly if true, limit the returns to directories that are mapped in
	 * 				the current Perforce client workspace
	 * @param deletedOnly if true, includes directories with only deleted files.
	 * @param haveListOnly if true, lists directories of files on the 'have' list.
	 * @return non-null but possibly empty list of qualifying directory file specs; only
	 * 					the getPath() path will be valid.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> getDirectories(List<IFileSpec> fileSpecs, boolean clientOnly, boolean deletedOnly,
													boolean haveListOnly)
									throws ConnectionException,  AccessException;
	
	/**
	 * An omnibus method to get a list of Perforce changelists from a server using zero or more
	 * qualifiers (note that convenience methods also exists, especially on the IClient
	 * interface).<p>
	 * 
	 * Note that if both submittedOnly and pendingOnly are true, the results are
	 * implementation-defined.
	 * 
	 * @param maxMostRecent if positive, restrict the list to the maxMostRecent
	 * 				most recent changelists.
	 * 				Implementations are free to ignore this parameter if necessary
	 * 				(and return all qualifying results).
	 * @param fileSpecs if non-empty, limits the results to
					changelists that affect the specified files.  If the file specification
        			includes a revision range, limits its results to
        			submitted changelists that affect those particular revisions.
	 * @param clientName if non-null, restrict the results to changelists associated
	 * 				with the given client.
	 * @param userName if non-null, restrict the results to changelists associated
	 * 				with the given user name.
	 * @param includeIntegrated if true, also include any changelists integrated into the
     *   			specified files (if any).
	 * @param type if non-null, restrict the results to the specified changelist type
     * @param longDesc if true, produce a non-truncated long version of the description
	 * @return a non-null (but possibly empty) list of qualifying changelists.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IChangelistSummary> getChangelists(int maxMostRecent, List<IFileSpec> fileSpecs, String clientName,
			String userName, boolean includeIntegrated,
						IChangelist.Type type, boolean longDesc) 
				throws ConnectionException, RequestException, AccessException;
	
	/**
	 * An omnibus method to get a list of Perforce changelists from a server using zero or more
	 * qualifiers (note that convenience methods also exists, especially on the IClient
	 * interface).<p>
	 * 
	 * Note that if both submittedOnly and pendingOnly are true, the results are
	 * implementation-defined.
	 * 
	 * @param maxMostRecent if positive, restrict the list to the maxMostRecent
	 * 				most recent changelists.
	 * 				Implementations are free to ignore this parameter if necessary
	 * 				(and return all qualifying results).
	 * @param fileSpecs if non-empty, limits the results to
					changelists that affect the specified files.  If the file specification
        			includes a revision range, limits its results to
        			submitted changelists that affect those particular revisions.
	 * @param clientName if non-null, restrict the results to changelists associated
	 * 				with the given client.
	 * @param userName if non-null, restrict the results to changelists associated
	 * 				with the given user name.
	 * @param includeIntegrated if true, also include any changelists integrated into the
     *   			specified files (if any).
     * @param longDesc if true, produce a non-truncated long version of the description
	 * @param submittedOnly if true, restrict the results to submitted changelists only.
	 * @param pendingOnly if true, restrict the results to pending changelists only.
	 * @return a non-null (but possibly empty) list of qualifying changelist summary objects.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IChangelistSummary> getChangelists(int maxMostRecent, List<IFileSpec> fileSpecs, String clientName,
							String userName, boolean includeIntegrated,
										boolean submittedOnly, boolean pendingOnly, boolean longDesc) 
								throws ConnectionException, RequestException, AccessException;
	
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
	 * @return non-null IChangelist describing the changelist; if no such changelist,
	 * 			a RequestException is thrown.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	IChangelist getChangelist(int id) throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a pending Perforce changelist. Throws a RequestException
	 * if the changelist was associated with opened files or was not a
	 * pending changelist.<p>
	 * 
	 * Note: any IChangelist object associated with the given changelist
	 * will no longer be valid after this operation, and using that object may
	 * cause undefined results or even global disaster -- you must ensure that
	 * the object is not used again improperly.
	 * 
	 * @param id the ID of the Perforce pending changelist to be deleted.
	 * @return possibly-null operation result message string from the Perforce server
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String deletePendingChangelist(int id) throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of the Perforce depot files associated with a Perforce changelist.<p>
	 * 
	 * The IFileSpec objects returned are not guaranteed to have any fields
	 * except depot path, version, and action valid.<p>
	 * 
	 * Changelists that are pending will not have files visible through this method;
	 * you should use the client openedFiles method for retrieving files in that situation.
	 * 
	 * @param id numeric changelist identifier
	 * @return non-null (but possibly empty) list of files associated with the changelist.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> getChangelistFiles(int id)
								throws ConnectionException, RequestException, AccessException;
	
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
	 * @param id the ID of the target changelist
	 * @param diffType if non-null, describes which type of diff to perform.
	 * @return InputStream onto the diff stream. Note that
	 *			while this stream will not be null, it may be empty
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	InputStream getChangelistDiffs(int id, DiffType diffType)
								throws ConnectionException, RequestException, AccessException;
	
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
	 * @param id the ID of the target changelist
	 * @param options DescribeOptions behavioural options for method.
	 * @return InputStream onto the diff stream. Note that
	 *			while this stream will not be null, it may be empty
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	InputStream getChangelistDiffsStream(int id, DescribeOptions options)
								throws ConnectionException, RequestException, AccessException;
	
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
	 * @param allrevs if true, streams all revisions within the specific range, rather
	 * 					than just the highest revision in the range
	 * @param noHeaderLine if true, suppresses the initial line that displays the file name
	 * 				and revision for each file / revision contents
	 * @return a non-null but possibly-empty InputStream onto the file / revision contents
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	InputStream getFileContents(List<IFileSpec> fileSpecs, boolean allrevs, boolean noHeaderLine)
								throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the revision history data for one or more Perforce files.<p>
	 * 
	 * Behavior is undefined if both longOutput and truncatedLongOutput are true. If both
	 * are false, a short form of the description (prepared by the server) is returned.
	 * 
	 * @param fileSpecs filespecs to be processed; if null or empty,
	 * 			an empty Map is returned.
	 * @param maxRevs if positive, return at most maxRev revisions for each file.
	 * @param includeInherited if true, causes inherited file history to be returned as well.
	 * @param longOutput if true, return the full descriptions associated with each revision
	 * @param truncatedLongOutput if true, return only the first 250 characters of each description.
	 * @return a non-null map of lists of revision data for qualifying files; the map is keyed
	 * 			by the IFileSpec of the associated file, meaning that errors are
	 * 			signaled using the normal IFileSpec getOpStatus() method.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(List<IFileSpec> fileSpecs,
			int maxRevs, boolean contentHistory, boolean includeInherited, boolean longOutput,
					boolean truncatedLongOutput) throws ConnectionException, AccessException;
	
	/**
	 * Get a list of all users who have subscribed to review the named files,
	 * the files in the numbered changelist, or all files by default.<p>
	 * 
	 * Note that the returned IUserSummary objects will have null access
	 * and update dates associated with them.
	 * 
	 * @param changelistId if not IChangelist.UNKNOWN, use this changelist ID.
	 * @param fileSpecs if not null, use this list as the list of named files rather
	 * 				than all files.
	 * @return non-null but possibly empty list of IUserSummary objects; note that
	 * 				these objects will have null update and access fields.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	List<IUserSummary> getReviews(int changelistId, List<IFileSpec> fileSpecs)
			throws ConnectionException, RequestException, AccessException;
	
	/**
	 * If one or more Perforce file specs is passed-in, return the opened / locked status
	 * of each file (if known) within an IFileSpec object; otherwise
	 * return a list of all files known to be open for this Perforce client workspace.<p>
	 * 
	 * The returned list can be modified with the other arguments as described below.
	 * 
	 * @param fileSpecs if non-empty, determine the status of the specified
	 * 				files; otherwise return all qualifying files known to be open
	 * @param allClients if true, return results for all known clients rather than the
	 * 				current client (if any).
	 * @param clientName if non-null, return results for the named client only.
	 * @param maxFiles if positive, return only the first maxFiles qualifying files.
	 * @param changeListId if positive, return only files associated with the given
	 * 				changelist ID; if IChangelist.DEFAULT, retrieve files open
	 * 				associated with the default changelist.
	 * @return non-null but possibly-empty list of qualifying open files. Not all fields
	 * 				in individual file specs will be valid or make sense to be accessed.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> getOpenedFiles(List<IFileSpec> fileSpecs, boolean allClients, String clientName,
											int maxFiles, int changeListId)
							throws ConnectionException, AccessException;
	
	/**
	 * Return a list of everything Perforce knows about a possibly very large set of
	 * Perforce files.<p>
	 * 
	 * This method is not intended for general use, and is not documented in detail here;
	 * consult the main Perforce fstat command documentation for detailed help. In particular,
	 * the various options are too complex to be described in a few sentences here, and
	 * the various option arguments reflect this complexity. Note that setting both
	 * sinceChangelist and affectedByChangelist to zero or a positive value will cause
	 * usage errors from the server (these are currently intended to be mutually-exclusive
	 * options).<p>
	 * 
	 * This method can be a real server and bandwidth resource hog, and should be used as
	 * sparingly as possible; alternatively, try to use it with as narrow a set of file
	 * specs as possible.
	 * 
	 * @param fileSpecs non-null list of Perforce file specification(s)
	 * @param maxFiles if positive, restrict the output to the first maxReturns files.
	 * 				Implementations are free to ignore this parameter if necessary
	 * 				(and return all qualifying results).
	 * @param sinceChangelist if larger than or equal to zero, display only files
	 *						affected since the given changelist number; zero is equivalent
	 *						to IChangelist.DEFAULT.
	 * @param affectedByChangelist if larger than or equal to zero, display only files
	 *						affected by the given changelist number; zero is equivalent
	 *						to IChangelist.DEFAULT.
	 * @param outputOptions if non-null, specifies the oputput options to be used
	 * @param ancilliaryOptions if non-null, specifies the ancilliary output options to be used
	 * @return a non-null (but possibly empty) list of qualifying files and associated stat info
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> fileSpecs, int maxFiles,
						int sinceChangelist, int affectedByChangelist,
						FileStatOutputOptions outputOptions, FileStatAncilliaryOptions ancilliaryOptions)
								throws ConnectionException, AccessException;
	
	/**
	 * Get a list of submitted integrations for the passed-in filespecs.
	 * 
	 * @param fileSpecs if null or ommitted, all qualifying depot files are used.
	 * @param branchSpec if non-null, only files integrated from the source
	 * 			to target files in the branch view are shown. Qualified files
	 * 			are displayed even if they were integrated without using the
	 * 			branch view itself.
	 * @param reverseMappings if true,reverses the mappings in the branch view, with
	 * 			the target files and source files exchanging place. This requires the
	 * 			branchSpec to be non-null.
	 * @return a non-null but possibly empty list of IFileSpec representing
	 * 			qualifying integrations.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> fileSpecs,
						String branchSpec, boolean reverseMappings)
								throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of changes and / or associated files not yet integrated (unsupported).
	 * Corresponds fairly closely to the p4 interchanges command for filespecs.<p>
	 * 
	 * Note that if showFiles is true, the returned files are attached to the associated
	 * changelist, and can be retrieved using the getFiles(false) method -- and note that
	 * if you call getFiles(true) you will get a refreshed list of <i>all</i> files associated with
	 * the changelist, which is probably different from the list associated with the
	 * integration.<p>
	 * 
	 * Note also that if there are no qualifying changes, this method will return an empty
	 * list rather than throw an exception; this behaviour is different to that seen with
	 * the p4 command line which will throw an exception.
	 * 
	 * @param fromFile non-null from-file specification.
	 * @param toFile non-null to-file specification.
	 * @param showFiles if true, show the individual files that would require integration.
	 * @param longDesc if true, return a long description in the changelist.
	 * @param maxChangelistId if greater than zero, only consider integration
	 * 				history from changelists at or below the given number
	 * @return non-null (but possibly empty) list of qualifying changelists. Note that
	 * 				the changelists returned here may not have all fields set (only
	 * 				description, ID, date, user, and client are known to be properly
	 * 				set by the server for this command).
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile, boolean showFiles,
										boolean longDesc, int maxChangelistId)
								throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of changes and / or associated files not yet integrated, based on
	 * branchspecs (unsupported). Corresponds fairly closely to the p4 interchanges
	 * command for branchspecs.<p>
	 * 
	 * Note that if showFiles is true, the returned files are attached to the associated
	 * changelist, and can be retrieved using the getFiles(false) method -- and note that
	 * if you call getFiles(true) you will get a refreshed list of <i>all</i> files associated with
	 * the changelist, which is probably different from the list associated with the
	 * integration.<p>
	 * 
	 * Note also that if there are no qualifying changes, this method will return an empty
	 * list rather than throw an exception; this behaviour is different to that seen with
	 * the p4 command line which will throw an exception.
	 * 
	 * @param branchSpecName non-null, non-empty branch spec name.
	 * @param fromFileList if non-null and not empty, and biDirectional is true,
	 * 				use this as the from file list.
	 * @param toFileList if non-null and not empty, use this as the to file list.
	 * @param showFiles if true, show the individual files that would require integration.
	 * @param longDesc if true, return a long description in the changelist.
	 * @param maxChangelistId if greater than zero, only consider integration
	 * @param reverseMapping if true, reverse the mappings in the branch view, with the
	 * 				target files and source files exchanging place.
	 * @param biDirectional
	 * @return non-null (but possibly empty) list of qualifying changelists. Note that
	 * 				the changelists returned here may not have all fields set (only
	 * 				description, ID, date, user, and client are known to be properly
	 * 				set by the server for this command).
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IChangelist> getInterchanges(String branchSpecName,
										List<IFileSpec> fromFileList, List<IFileSpec> toFileList,
										boolean showFiles, boolean longDesc, int maxChangelistId,
										boolean reverseMapping, boolean biDirectional)
								throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return a list of Perforce jobs. Note that (as discussed in the IJob comments)
	 * Perforce jobs can have a wide variety of fields, formats, semantics, etc., and
	 * this method can return a list that may have to be unpacked at the map level by
	 * the consumer to make any sense of it.<p>
	 * 
	 * Note that this method (unlike the main file list methods) throws an exception
	 * and stops at the first encountered error.
	 * 
	 * @param fileSpecs if given, return only jobspecs affecting the given file(s)
	 * @param maxJobs if positive, return only up to maxJobs results
	 * @param longDescriptions if true, return full descriptions, otherwise show
	 * 				only a subset (typically the first 128 characters, but
	 * 				this is not guaranteed).
	 * @param reverseOrder if true, reverse the normal sort order
	 * @param includeIntegrated if true, include any fixes made by changelists
	 * 				integrated into the specified files
	 * @param jobView if non-null, a string in format detailed by "p4 help jobview"
	 * 				used to restrict jobs to those satisfying the job view expression.
	 * @return a non-null (but possibly-empty) list of qualifying Perforce jobs
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IJob> getJobs(List<IFileSpec> fileSpecs, int maxJobs, boolean longDescriptions,
								boolean reverseOrder, boolean includeIntegrated,
								String jobView)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a specific job. Note that some implementations of the underlying
	 * server do not return null if you ask for a job that doesn't exist; you
	 * must do your own checking to see of what's returned represents a real
	 * job or not.
	 * 
	 * @param jobId non-null job Id.
	 * @return IJob for the named job; null if no such job.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	IJob getJob(String jobId)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Create a new Perforce job in the Perforce server corresponding to the passed-in Perforce
	 * job fields (which in turn should correspond to at least the mandatory fields defined
	 * in the reigning Perforce job spec).<p>
	 * 
	 * Perforce job semantics, field count and layout, etc., are to some extent free-form and
	 * specified for each server by the associated job spec (retrievable using the getJobSpec()
	 * method below), so map fields are passed to the Perforce server exactly as passed to the
	 * create method in the job's field map, so you need to know the field names and semantics
	 * given by the associated job spec. This includes setting the relevant job ID field to
	 * "new", but otherwise, no checking is done on fields in this method against the
	 * job spec (this may be added later).<p>
	 * 
	 * @param fieldMap non-null field map defining the new job in the Perforce server.
	 * @return returns an IJob representing the newly-created job, if successful.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	IJob createJob(Map<String, Object> fieldMap)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update a Perforce job on the Perforce server. Note that <i>only</i> the
	 * associated raw fields map is used for field values; the main description
	 * and ID fields are actually ignored.<p>
	 * 
	 * The returned string will contain whatever the Perforce server returned
	 * in response to this command; in general, if the update fails, an exception
	 * will be thrown, meaning that the returned string represents success only.
	 * There are two success states -- either the job was saved or it didn't need
	 * saving (it was the same after updating). Consumers should parse this
	 * accordingly.
	 * 
	 * @param job non-null candidate for updating.
	 * @return possibly-null status message as returned from the server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String updateJob(IJob job)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Delete a job from the Perforce server. Note that this method does not change
	 * the status of the associated job locally, just on the Perforce server.
	 * 
	 * @param jobId ID of the job to be deleted.
	 * @return possibly-null status message as returned from the server; this may include
	 * 				form trigger output pre-pended and / or appended to the "normal" message.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	String deleteJob(String jobId)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return the Perforce jobspec associated with this Perforce server.<p>
	 * 
	 * @return possibly-null IJobSpec representing the unserlying Perforc server's
	 * 				jobspec.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	IJobSpec getJobSpec()
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return a list of all Perforce jobs with fix records associated with them,
     * along with the changelist number of the fix. Detailed semantics for this
     * method are given in the main Perforce documentation for the p4 command "fixes".<p>
     * 
     * Note that this method (unlike the main file list methods) throws an exception
	 * and stops at the first encountered error.
	 * 
     * 
	 * @param fileSpecs if given, restrict output to fixes associated with these files
	 * @param changeListId if positive, only fixes from the numbered changelist are listed.
	 * @param jobId if non-null, only fixes for the named job are listed
	 * @param includeIntegrations if true, include any fixes made by changelists integrated
	 * 				into the specified files
	 * @param maxFixes if positive, restrict the list to the first maxFixes fixes
	 * @return non-null but possibly empty list of qualifying IFix fixes.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFix> getFixList(List<IFileSpec> fileSpecs, int changeListId, String jobId,
							boolean includeIntegrations, int maxFixes)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Mark each named job as being fixed by the changelist number given with changeListId.
	 * Full details of the use of this method and the associated parameters will not be 
	 * given here; consult the main Perforce documentation for the details.
	 * 
	 * @param changeListId changelist number
	 * @param status if non-null, use this as the new status rather than "closed"
	 * @param delete if true, delete the specified fixes
	 * @return list of affected fixes
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IFix> fixJobs(List<String> jobIdList, int changeListId, String status, boolean delete)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Return a list of Perforce server processes active on the Perforce server. Will throw
	 * a request exception if monitors are not enabled on the target server.
	 * 
	 * @return non-null but possibly-empty list of IServerProcess objects
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	List<IServerProcess> getServerProcesses()
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Run diff on the Perforce server of two files in the depot.<p>
	 * 
	 * This method corresponds closely to the standard diff2 command, and that
	 * command's documentation should be consulted for the overall and detailed
	 * semantics. In particular, the various potentially-valid combinations of
	 * branch spec and file specs can be complicated and won't be repeated here.<p>
	 * 
	 * As with other streams-based IServer methods, callers should ensure that
	 * the stream returned here is always explicitly closed after use; if not
	 * closed, the stream's associated temporary files managed by P4Java
	 * (if they exist) may not be properly deleted.
	 * 
	 * @param file1 optional first file IFileSpec 
	 * @param file2 optional second file IFileSpec
	 * @param branchSpecName optional branch spec name
	 * @param quiet if true, suppresses the display of the header lines of files whose
	 * 				content and types are identical and suppresses the actual diff
	 * 				for all files.
	 * @param includeNonTextDiffs if true, forces 'p4 diff2' to diff even files with
	 * 				non-text (binary) types
	 * @param gnuDiffs see "-u" option in the main diff2 documentation.
	 * @return non-null but possibly empty InputStream of diffs and headers
	 * 				as returned from the server.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	InputStream getServerFileDiffs(IFileSpec file1, IFileSpec file2, String branchSpecName,
							DiffType diffType, boolean quiet, boolean includeNonTextDiffs, boolean gnuDiffs)
					throws ConnectionException, RequestException, AccessException;
	
	
	/**
	 * Run diff on the Perforce server of two files in the depot.<p>
	 * 
	 * This method corresponds closely to the standard diff2 command, and that
	 * command's documentation should be consulted for the overall and detailed
	 * semantics. In particular, the various potentially-valid combinations of
	 * branch spec and file specs can be complicated and won't be repeated here.<p>
	 * 
	 * @param file1 optional first file IFileSpec 
	 * @param file2 optional second file IFileSpec
	 * @param branchSpecName optional branch spec name
	 * @param quiet if true, suppresses the display of the header lines of files whose
	 * 				content and types are identical and suppresses the actual diff
	 * 				for all files.
	 * @param includeNonTextDiffs if true, forces 'p4 diff2' to diff even files with
	 * 				non-text (binary) types
	 * @param gnuDiffs see "-u" option in the main diff2 documentation.
	 * @return non-null but possibly empty array of file diffs
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	List<IFileDiff> getFileDiffs(IFileSpec file1, IFileSpec file2, String branchSpecName,
			DiffType diffType, boolean quiet, boolean includeNonTextDiffs, boolean gnuDiffs)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a map.<p>
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
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	Map<String, Object>[] execMapCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a map.<p>
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
	 * @since 2011.1
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
	
	Map<String, Object>[] execInputStringMapCmd(String cmdName, String[] cmdArgs, String inString)
					throws P4JavaException;
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and return the results as a map without
	 * invoking any command callbacks.<p>
	 * 
	 * Basically equivalent to execMapCmd with temporary disabling of any ICommandCallback
	 * calls and / or listeners; this turns out to be useful for various reasons we won't go
	 * into here...<p>
	 *
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
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	Map<String, Object>[] execQuietMapCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
					throws ConnectionException, RequestException, AccessException;
	
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
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @return an InputStream on the command output. This will never be null, but it may be empty.
	 * 				You <i>must</i> properly close this stream after use or temporary files may
	 * 				be left lying around the VM's java.io.tmpdir area.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	InputStream execStreamCmd(String cmdName, String[] cmdArgs)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Issue an arbitrary P4Java command to the Perforce server and get the results as a stream
	 * without invoking any command callbacks.<p>
	 * 
	 * Basically equivalent to execStreamCmd with temporary disabling of any ICommandCallback
	 * calls and / or listeners; this turns out to be useful for various reasons we won't go
	 * into here...<p>
	 * 
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @return an InputStream on the command output. This will never be null, but it may be empty.
	 * 				You <i>must</i> properly close this stream after use or temporary files may
	 * 				be left lying around the VM's java.io.tmpdir area.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	InputStream execQuietStreamCmd(String cmdName, String[] cmdArgs)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Issue a streaming map command to the Perforce server, using an optional
	 * map for any input expected by the server (such as label or job specs,
	 * etc.).<p>
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
	 * @since 2011.1
	 * 
	 * @param cmdName the command to be issued; must be non-null, and correspond to a Perforce
	 * 				command recognized by P4Java and defined in CmdSpec.
	 * @param cmdArgs the array of command arguments (options and file arguments, etc.) to be
	 * 				sent to the Perforce server. These must be in the form used by the corresponding
	 * 				p4 command line interpreter. Ignored if null.
	 * @param inMap an optional map to be sent to the server as standard input, using the
	 * 				Python map format (-G) form. You must remember to issue the relevant
	 * 				command-specific option to enable this if needed.
	 * @param callback a non-null IStreamingCallback to be used to process the incoming
	 * 				results.
	 * @param key an opaque integer key that is passed to the IStreamingCallback callback
	 * 				methods to identify the action as being associated with this specific
	 * 				call.
	 * @throws P4JavaException if an error occurs processing this method and its parameters.
	 */
	
	void execStreamingMapCommand(String cmdName, String[] cmdArgs, Map<String, Object> inMap,
											IStreamingCallback callback, int key) throws P4JavaException;
	
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
	 * @since 2011.1
	 * 
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
	 * 
	 * @deprecated As of release 2013.1, replaced by {@link com.perforce.p4java.server.IOptionsServer#execInputStringStreamingMapCmd(java.lang.String, java.lang.String[], java.lang.String, com.perforce.p4java.server.callback.IStreamingCallback, int)}
 	 */
	@Deprecated
	void execInputStringStreamingMapComd(String cmdName, String[] cmdArgs, String inString,
											IStreamingCallback callback, int key) throws P4JavaException;

	/**
	 * Get the value of a named Perforce counter from the Perforce server. Note that this
	 * method will return a zero string (i.e. "0") if the named counter doesn't exist (rather
	 * than throw an exception); use getCounters to see if a counter actually exists before
	 * you use it.<p>
	 * 
	 * Note that despite their name, counters can be any value, not just a number; hence
	 * the string return value here.
	 * 
	 * @param counterName non-null counter name.
	 * @return non-null (but possibly empty or useless) counter value associated
	 * 				with counterName.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	String getCounter(String counterName) 
					throws ConnectionException, RequestException, AccessException;
	
	void setCounter(String counterName, String value, boolean perforceCounter)
					throws ConnectionException, RequestException, AccessException;
	
	void deleteCounter(String counterName, boolean perforceCounter)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a map of the Perforce server's counters. Counter usage is not explained here -- see the
	 * main Perforce documentation -- but in general they're sometimes useful to get an indirect
	 * idea of server capabilities and state.
	 * 
	 * @return a non-null (but possibly empty) map of counters. key and value semantics and format
	 * 				are not specified here.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	Map<String, String> getCounters()
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the database schema associated with this server (admin / superuser command).<p>
	 * 
	 * See the main p4 admin command documentation for full semantics and usage details.
	 * 
	 * @param tableSpecs if null, return all known schema; otherwise, restrict the returned
	 * 				list to the named tables and table versions.
	 * @return a non-null but possibly empty list of IDbSchema for the passed-in
	 * 				schema identifiers.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<IDbSchema> getDbSchema(List<String> tableSpecs)
					throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get a list of exported journal or checkpoint records (admin / superuser command).<p>
	 * 
	 * See the main p4 admin command documentation for full semantics and usage details.
	 * 
	 * @param useJournal if true, export journal records; otherwise, export checkpoint records.
	 * @param maxRecs if larger than zero, return only the first maxRec lines
	 * @param sourceNum checkpoint or journal number.
	 * @param offset offset with in checkpoint or journal.
	 * @param format if true, formats record output appropriately for the type of data.
	 * @param journalPrefix if not null, specify a journal name prefix.
	 * @param filter if not null, pass the specified filter to the exporter.
	 * @return non-null but possibly empty list of maps representing exported
	 * 				journal or checkpoint records.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	List<Map<String, Object>> getExportRecords(boolean useJournal, long maxRecs, int sourceNum, long offset,
									boolean format, String journalPrefix, String filter)
					throws ConnectionException, RequestException, AccessException;
}
