/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;


/**
 * Defines a full Perforce branch specification for use in Perforce integrate (merge)
 * commands.<p>
 * 
 * Full branch specs in the current implementation are always complete. 
 */

public interface IBranchSpec extends IBranchSpecSummary {
	
	/**
	 * Return the view map associated with this branch spec.<p>
	 */
	ViewMap<IBranchMapping> getBranchView();
	
	/**
	 * Set the the view map associated with this branch spec. This will not change
	 * the associated branch spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param branchView new view mappings for the branch.
	 */
	void setBranchView(ViewMap<IBranchMapping> branchView);
}
