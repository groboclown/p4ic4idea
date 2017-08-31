/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServer;

/**
 * Defines and describes a Perforce label. See the main Perforce
 * documentation for label usage and semantics.<p>
 * 
 * ILabel objects are complete and updateable only if they come from the
 * IServer.getLabel() method (or are hand-crafted); label objects from other sources
 * are not complete or completable, and nor are they refreshable. Setter methods
 * defined below affect only local values unless a suitable update is done.<p>
 */

public interface ILabel extends ILabelSummary {
	
	/**
	 * Get the view mapping for this label. Note that only the
	 * left hand side (the depot path) of a mapping is used for labels
	 * and will be valid here.<p>
	 * 
	 * Note also that this method will only return the actual view
	 * mapping a label if the label object was returned from the IServer's
	 * getLabel() method (this is due to limitations in the underlying
	 * implementation).
	 * 
	 * @return non-null but possibly empty list of IClientViewMapping
	 * 			mappings for this label.
	 */
	ViewMap<ILabelMapping> getViewMapping();
	
	/**
	 * Set the view mapping for this label. Note that only the
	 * left hand side (the depot path) of a mapping is used for labels
	 * and will be valid here.
	 * 
	 * @param viewMapping list of IClientViewMapping mappings for this label.
	 */
	void setViewMapping(ViewMap<ILabelMapping> viewMapping);
	
	/**
	 * Update (or even create) this label on the associated Perforce server,
	 * if that server has been set for this label. Will throw a suitable
	 * RequestException if the label is not associated with a server,
	 * either as the result of being returned from a server earlier, or
	 * as the result of an explicit call on the underlying implementation
	 * object.<p>
	 * 
	 * Note that you should only call this method on "full" labels, i.e.
	 * those returned from an explicit single call to IServer.getLabel()
	 * or those created by hand; calling this on a label returned from the
	 * getLabelList() method may cause the associated in-server label to lose
	 * its view mapping.
	 * 
	 * @deprecated use update() instead.
	 * 
	 * @return the string message resulting from the update
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	String updateOnServer()
				throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Update (or even create) this label on the associated Perforce server,
	 * if that server has been set for this label. Will throw a suitable
	 * RequestException if the label is not associated with a server,
	 * either as the result of being returned from a server earlier, or
	 * as the result of an explicit call on the underlying implementation
	 * object.<p>
	 * 
	 * Note that you should only call this method on complete labels, i.e.
	 * those returned from an explicit single call to IServer.getLabel()
	 * or those created by hand; calling this on a label returned from the
	 * getLabelList() method will result in a UnimplementedError being
	 * thrown.
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 * 
	 */
	
	void update() throws ConnectionException, RequestException, AccessException;
	
	/**
	 * Get the Perforce server object associated with this label.
	 * 
	 * @return possibly-null IServer object associated with this label.
	 */
	IServer getServer();
	
	/**
	 * Set the Perforce server object associated with this label.
	 * 
	 * @param server possibly-null IServer object to be associated with this label.
	 */
	void setServer(IServer server);
}
