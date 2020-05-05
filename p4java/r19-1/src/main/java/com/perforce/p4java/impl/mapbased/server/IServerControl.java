/**
 * 
 */
package com.perforce.p4java.impl.mapbased.server;

import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.server.ServerStatus;

import java.util.Properties;

/**
 * The minimal control interface all IServer impl classes must implement.
 */

public interface IServerControl {
	
	/**
	 * Initialize the server. Called immediately after the server class is
	 * instantiated. The semantics of this method are fairly broad: the result
	 * of calling this method is that a future call to "normal" server calls must
	 * not fail because of any setting-up problems.
	 * 
	 * @param host the Perforce server hostname or IP address as passed in to the factory method
	 * @param port the Perforce server port number as passed in to the factory method
	 * @param props the properties passed in to the factory method
	 * @param opts the UsageOptions object to be associated with the server object; if null,
	 * 			the server should construct a new default UsageOptions object.
	 * @param secure indicates whether the server is secure (SSL) or not.
	 * @param rsh the 'rsh' mode server launch command.
	 * @return the resulting status; should be ACTIVE unless otherwise specified
	 * @throws ConfigException if there's something wrong with the
	 * 			specified configuration or associated config files, etc.
	 * @throws ConnectionException if the server is unreachable on initialization, and that
	 * 			unreachability is serious and unrecoverable (there are implementations that don't
	 * 			really do connections per se, so they may not consider this an error or even try
	 * 			connecting during initialisation).
	 */
	
	ServerStatus init(String host, int port, Properties props, UsageOptions opts, boolean secure,
							String rsh) throws ConfigException, ConnectionException;

	/**
	 * Convenience method for init(host, port, props, opts, secure, null). See init's main Javadoc
	 * for full documentation.
	 * 
	 * @param host the Perforce server hostname or IP address as passed in to the factory method
	 * @param port the Perforce server port number as passed in to the factory method
	 * @param props the properties passed in to the factory method
	 * @param opts the UsageOptions object to be associated with the server object; if null,
	 * 			the server should construct a new default UsageOptions object.
	 * @return the resulting status; should be ACTIVE unless otherwise specified
	 * @throws ConfigException if there's something wrong with the
	 * 			specified configuration or associated config files, etc.
	 * @throws ConnectionException if the server is unreachable on initialization, and that
	 * 			unreachability is serious and unrecoverable (there are implementations that don't
	 * 			really do connections per se, so they may not consider this an error or even try
	 * 			connecting during initialisation).
	 */
	
	ServerStatus init(String host, int port, Properties props, UsageOptions opts, boolean secure)
							throws ConfigException, ConnectionException;
	
	/**
	 * Convenience method for init(host, port, props, opts, secure). See init's main Javadoc
	 * for full documentation.
	 * 
	 * @param host the Perforce server hostname or IP address as passed in to the factory method
	 * @param port the Perforce server port number as passed in to the factory method
	 * @param props the properties passed in to the factory method
	 * @param opts the UsageOptions object to be associated with the server object; if null,
	 * 			the server should construct a new default UsageOptions object.
	 * @return the resulting status; should be ACTIVE unless otherwise specified
	 * @throws ConfigException if there's something wrong with the
	 * 			specified configuration or associated config files, etc.
	 * @throws ConnectionException if the server is unreachable on initialization, and that
	 * 			unreachability is serious and unrecoverable (there are implementations that don't
	 * 			really do connections per se, so they may not consider this an error or even try
	 * 			connecting during initialisation).
	 */

	ServerStatus init(String host, int port, Properties props, UsageOptions opts)
	throws ConfigException, ConnectionException;

	/**
	 * Convenience method for init(host, port, props, null). See init's main Javadoc
	 * for full documentation.
	 *
	 * @param host the Perforce server hostname or IP address as passed in to the factory method
	 * @param port the Perforce server port number as passed in to the factory method
	 * @param props the UsageOptions object to be associated with the server object; if null,
	 * 			the server should construct a new default UsageOptions object.
	 * @return the resulting status; should be ACTIVE unless otherwise specified
	 * @throws ConfigException if there's something wrong with the
	 * 			specified configuration or associated config files, etc.
	 * @throws ConnectionException if the server is unreachable on initialization, and that
	 * 			unreachability is serious and unrecoverable (there are implementations that don't
	 * 			really do connections per se, so they may not consider this an error or even try
	 * 			connecting during initialisation).
	 */
	
	ServerStatus init(String host, int port, Properties props)
							throws ConfigException, ConnectionException;
}
