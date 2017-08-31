package com.perforce.p4java.core;

/**
 * Describes possible changelist status values.
 */

public enum ChangelistStatus {
	NEW,
	PENDING,
	SUBMITTED;
	
	/**
	 * A user-friendly fail-safe way to convert from strings to file actions
	 * without generating exceptions, etc.
	 * 
	 * @param str upper, lower, or mixed-case candidate string, or null
	 * @return null if no match or if str was null, otherwise returns the
	 * 			closest matching file action
	 */
	
	public static ChangelistStatus fromString(String str) {
		if (str != null) {
			for (ChangelistStatus status : ChangelistStatus.values()) {
				if (status.toString().equalsIgnoreCase(str)) {
					return status;
				}
			}
		}
		return null;
	}
}