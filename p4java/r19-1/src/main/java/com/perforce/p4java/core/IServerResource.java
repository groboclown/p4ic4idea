/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Defines methods available on all participating objects returned from
 * P4Java server and client methods that represent server-side objects
 * such as changelists, jobs, etc.<p>
 * 
 * The general intention here is to allow the underlying P4Java plumbing
 * to use lightweight metadata-only objects where possible (or full-strength
 * objects only partially-implemented or filled-out), and allow the consumer
 * to determine the status of the underlying object and act accordingly.<p>
 * 
 * This approach requires all participating P4Java interfaces to spell
 * out what both "updateable" and "refreshable" mean, and whether the 
 * associated methods are implemented or not.
 */

public interface IServerResource {
	
	/**
	 * Returns true if the underlying object is refreshable from the Perforce
	 * server.<p>
	 * 
	 * The details of what "refreshable" means in this context are always
	 * object-dependent, but typically mean that "live" data and metadata
	 * will be updated from the server. This is especially useful on objects
	 * like changelists, where the underlying server-side data may change
	 * often outside P4Java's control.
	 * 
	 * @return true if the underlying the object is refreshable.
	 */
	boolean canRefresh();
	
	/**
	 * Refresh the underlying object from the Perforce server.<p>
	 * 
	 * The details of what "refreshable" means in this context are always
	 * object-dependent, but typically mean that "live" data and metadata
	 * will be updated from the server.<p>
	 * 
	 * The results of calling this method on objects whose canRefresh
	 * method returns false are undefined (but will generally result in a
	 * UnimplementedError being thrown).
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	void refresh() throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Returns true if the underlying object can be updated back to (or on) the
	 * associated Perforce server. The semantics of server updates are generally
	 * object-specific.
	 * 
	 * @return true if the underlying object can be updated back to (or on) the
	 * 				associated Perforce server
	 */
	boolean canUpdate();

	/**
	 * Old method called used to call refresh when completing a spec.
	 *
	 * @throws ConnectionException
	 *             when there is an error talking to the Helix server
	 * @throws RequestException
	 *             when there is a problem with the data provided in the request
	 * @throws AccessException
	 *             when access to the branch command is not authorised
	 */
	void complete() throws ConnectionException, RequestException, AccessException;

	/**
	 * Update the Perforce server object associated with the underlying P4Java object,
	 * if possible.  The semantics of server updates are generally object-specific and
	 * will be spelled out for each participating object.<p>
	 * 
	 * The results of calling this method on objects whose canUpdate
	 * method returns false are undefined (but will generally result in a
	 * UnimplementedError being thrown).
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	void update() throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Force (if true) update the Perforce server object associated with the underlying
	 * P4Java object, if possible.  The semantics of server updates are generally
	 * object-specific and will be spelled out for each participating object.<p>
	 * 
	 * Note, in order to force the change it may require super user / admin privileges
	 * to work properly.
	 * 
	 * The results of calling this method on objects whose canUpdate
	 * method returns false are undefined (but will generally result in a
	 * UnimplementedError being thrown).
	 * 
	 * @param force if true, force the update of the object on the server.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	void update(boolean force) throws ConnectionException, RequestException, AccessException;

	/**
	 * Update the Perforce server object associated with the underlying P4Java object
	 * and its options, if possible.  The semantics of server updates are generally
	 * object-specific and will be spelled out for each participating object.<p>
	 * 
	 * The results of calling this method on objects whose canUpdate
	 * method returns false are undefined (but will generally result in a
	 * UnimplementedError being thrown).
	 * 
	 * @param opts Options object describing optional parameters; if null, no
	 * 				options are set.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	void update(Options opts) throws ConnectionException, RequestException, AccessException;

	/**
	 * Set the server associated with this resource. Setting this null can have
	 * bad effects down the line...
	 * 
	 * @param server IServer to be used for refresh, update, etc.
	 */
	void setServer(IServer server);
}
