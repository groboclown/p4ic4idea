/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

import java.util.List;

/**
 * Defines the stream's cached integration status with respect to its parent. If
 * the cache is stale, either because newer changes have been submitted or the
 * stream's branch view has changed, 'istat' checks for pending integrations and
 * updates the cache before showing status.
 */
public interface IStreamIntegrationStatus {

	/**
	 * Defines the cached state of the stream's integration status without
	 * refreshing stale data.
	 */
	public interface ICachedState {

		/**
		 * Get the changelist.
		 */
		int getChange();

		/**
		 * Get the parent changelist.
		 */
		int getParentChange();

		/**
		 * Get the copy parent changelist.
		 */
		int getCopyParent();

		/**
		 * Get the merge parent changelist.
		 */
		int getMergeParent();

		/**
		 * Get the merge high value changelist.
		 */
		int getMergeHighVal();

		/**
		 * Get the branch hash.
		 */
		int getBranchHash();

		/**
		 * Get the status
		 */
		int getStatus();
	};

	/**
	 * Get the stream's path in a stream depot.
	 */
	String getStream();

	/**
	 * Get the stream's parent.
	 */
	String getParent();

	/**
	 * Get the stream's type.
	 */
	IStreamSummary.Type getType();

	/**
	 * Is firmer than parent.
	 */
	boolean isFirmerThanParent();

	/**
	 * Is change flows to parent.
	 */
	boolean isChangeFlowsToParent();

	/**
	 * Is change flows from parent.
	 */
	boolean isChangeFlowsFromParent();

	/**
	 * Is integration to parent.
	 */
	boolean isIntegToParent();

	/**
	 * Get how the integration to parent was performed.
	 */
	String getIntegToParentHow();

	/**
	 * Get the to result.
	 */
	String getToResult();

	/**
	 * Is integration from parent.
	 */
	boolean isIntegFromParent();

	/**
	 * Get how the integration from parent was performed.
	 */
	String getIntegFromParentHow();

	/**
	 * Get the from result.
	 */
	String getFromResult();
	
	/**
	 * Get the cached states
	 */
	List<ICachedState> getCachedStates();
}
