/**
 * 
 */
package com.perforce.p4java.core;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;

/**
 * Defines a full Perforce user object. See the main
 * Perforce documentation for full usage and semantics.<p>
 * 
 * IUser objects are complete and updateable only if they come from the
 * IServer.getUser() method (or are hand-crafted); user objects from other sources
 * are not complete or completable, and nor are they refreshable. Setter methods
 * defined below or on the superclass affect only local values unless a suitable
 * update is done.<p>
 */

public interface IUser extends IUserSummary {
	String getPassword();
	void setPassword(String password);
	
	String getJobView();
	void setJobView(String jobView);
	
	ViewMap<IReviewSubscription> getReviewSubscriptions();
	void setReviewSubscriptions(ViewMap<IReviewSubscription> subs);
	
	/**
	 * Updates this user on the Perforce server; if force is
	 * true, force the change (requires super user / admin
	 * privileges to work properly). This method is supplied
	 * in addition to the normal IServerRespurce.update() method
	 * since that method is always called with force set to false.
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller 
	 */
	void update(boolean force)
				throws ConnectionException, RequestException, AccessException;
}
