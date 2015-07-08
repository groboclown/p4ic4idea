/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.core;

import java.util.Date;

import com.perforce.p4java.Log;

/**
 * Provides an interface onto, and a set of methods to access a specific
 * Perforce depot contained in a Perforce server. See the main Perforce documentation
 * and help system for a full discussion of depots and associated metadata and usage models.<p>
 * 
 * IDepot objects are not updateable or refreshable, and are currently "complete" in all
 * implementations. There are no setter methods here as depots are intentionally read-only
 * in P4Java.
 */

public interface IDepot extends IServerResource {
	
	public enum DepotType {
		LOCAL,
		REMOTE,
		SPEC,
		STREAM,
		ARCHIVE,
		UNLOAD,
		UNKNOWN;
		
		/**
		 * Return a suitable Depot type as inferred from the passed-in
		 * string, which is assumed to be the string form of a Depot type.
		 * Otherwise return the UNKNOWN type
		 */
		public static DepotType fromString(String str) {
			if (str == null) {
				return null;
			}

			try {
				return DepotType.valueOf(str.toUpperCase());
			} catch (IllegalArgumentException iae) {
				Log.error("Bad conversion attempt in DepotType.fromString; string: "
						+ str + "; message: " + iae.getMessage());
				Log.exception(iae);
				return UNKNOWN;
			}
		}
	};
		
	/**
	 * Get the name of the depot.
	 */
	String getName();
	
	/**
	 * Get the Perforce user name of the depot's owner.
	 */
	String getOwnerName();
	
	/**
	 * Get the date the depot was last modified.
	 */
	Date getModDate();
	
	/**
	 * Get the description associated with this depot.
	 */
	String getDescription();
	
	/**
	 * Get the type of this depot.
	 */
	DepotType getDepotType();
	
	/**
	 * For remote depots, return the (remote) address of the depot; for other
	 * types of depot, will return null.
	 */
	String getAddress();
	
	/**
	 * For spec depots, return the optional suffix (default '.p4s')
	 * for the generated paths associated with the depot; for other depot types,
	 * return null.
	 */
	String getSuffix();
	
	/**
	 * Get the depot's path translation information.
	 */
	String getMap();
}
