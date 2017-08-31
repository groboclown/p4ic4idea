/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core;

/**
 * Interface for file line matches resulting from grep commands
 */
public interface IFileLineMatch {

	/**
	 * Match type
	 */
	enum MatchType {

		/**
		 * Matched line
		 */
		MATCH,

		/**
		 * Before matched line
		 */
		BEFORE,

		/**
		 * After matched line
		 */
		AFTER;

		/**
		 * Get a match type enumeration for the specified server value.
		 * 
		 * The returned value will be {@link MatchType#MATCH} when the
		 * serverValue specified is null.
		 * 
		 * @param serverValue
		 * @return type
		 */
		public static MatchType fromServerString(String serverValue) {
			MatchType type = MATCH;
			if (serverValue != null) {
				type = valueOf(serverValue.toUpperCase());
			}
			return type;
		}

	}

	/**
	 * Get depot file path of match
	 * 
	 * @return depot file path
	 */
	String getDepotFile();

	/**
	 * Get revision number of match
	 * 
	 * @return revision number
	 */
	int getRevision();

	/**
	 * Get line text of match
	 * 
	 * @return line text
	 */
	String getLine();

	/**
	 * Get line number of match
	 * 
	 * @return line number
	 */
	int getLineNumber();

	/**
	 * Get match type
	 * 
	 * @return - type
	 */
	MatchType getType();

}
