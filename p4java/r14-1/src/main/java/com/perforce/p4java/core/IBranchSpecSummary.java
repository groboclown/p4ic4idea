/**
 * 
 */
package com.perforce.p4java.core;

import java.util.Date;

/**
 * Defines the summary Perforce branch metadata typically returned by
 * the  getBranchSummaryList() method, corresponding to "p4 branches"
 * and similar.<p>
 * 
 * In general, branch summary information excludes the branch view, and
 * no server-side operations can be performed against them; for full branch
 * functionality you should use the full IBranchSpec interface.<p>
 * 
 * Branch summaries are complete and not refreshable or updateable.
 */

public interface IBranchSpecSummary extends IServerResource {
	
	/**
	 * Get the name of this branch.
	 */
	String getName();
	
	/**
	 * Get the name of the user who created this branch.
	 */
	String getOwnerName();
	
	/**
	 * Get the date specification was last modified.
	 */
	Date getUpdated();
	
	/**
	 * Get the date of the last 'integrate' using this branch.
	 */
	Date getAccessed();
	
	/**
	 * Get the branch's description (if any).
	 */
	String getDescription();
	
	/**
	 * Return true if the branch spec is locked.
	 */
	boolean isLocked();
	
	/**
	 * Set the name of this branch. This will not change the name of
	 * the associated branch spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param name new branch spec name
	 */
	void setName(String name);
	
	/**
	 * Set the owner's name for this branch. This will not change
	 * the associated branch spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param ownerName new owner's name
	 */
	void setOwnerName(String ownerName);
	
	/**
	 * Set the last-updated date. This generally has no effect on the
	 * associated Perforce server version of this spec.
	 * 
	 * @param updated new updated date.
	 */
	void setUpdated(Date updated);
	
	/**
	 * Set the last-accessed date. This generally has no effect on the
	 * associated Perforce server version of this spec.
	 * 
	 * @param accessed new accessed date.
	 */
	void setAccessed(Date accessed);
	
	/**
	 * Set the branch spec description. This will not change
	 * the associated branch spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param description new description string.
	 */
	void setDescription(String description);
	
	/**
	 * Set whether the branch spec is locked or not. This will not change
	 * the associated branch spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param locked boolean lock status
	 */
	void setLocked(boolean locked);
}
