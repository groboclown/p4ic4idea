/**
 * 
 */
package com.perforce.p4java.core.file;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaError;

/**
 * Defines the possible Perforce actions that can be associated with a Perforce file,
 * including synchronization actions. Also used to indicate open status for files on
 * pending changelists, resolve status, etc.<p>
 * 
 * Basically self-explanatory if you have much experience with Perforce, but if not,
 * the main Perforce documentation goes into great detail about each of these actions.
 */

public enum FileAction {
	ADD,
	BRANCH,
	EDIT,
	INTEGRATE,
	DELETE,

	// sync and misc. actions and pseudo-actions:
	
	SYNC,
	UPDATED,
	ADDED,
	REFRESHED,
	REPLACED,
	DELETED,
	IGNORED,
	ABANDONED,
	EDIT_IGNORED,
	MOVE,
	MOVE_ADD,
	MOVE_DELETE,
	RESOLVED,
	UNRESOLVED,
	COPY_FROM,
	MERGE_FROM,
	EDIT_FROM,
	PURGE,
	IMPORT,
	
	UNKNOWN;
	
	// NOTE: the following array MUST be in 1:1 correspondence
	// with the ordering and values used above or bizarre results
	// will occurr elsewhere...
	
	private static final String[] names = {
		"add",
		"branch",
		"edit",
		"integrate",
		"delete",
		"sync",
		"updated",
		"added",
		"refreshed",
		"replaced",
		"deleted",
		"ignored",
		"abandoned",
		"edit/ignored",
		"move",
		"move/add",
		"move/delete",
		"resolved",
		"unresolved",
		"copy from",
		"merge from",
		"edit from",
		"purge",
		"import",
		
		"unknown"
	};
	
	/**
	 * A user-friendly fail-safe way to convert from strings to file actions
	 * without generating exceptions, etc.
	 * 
	 * @param str upper, lower, or mixed-case candidate string, or null
	 * @return null if no match or if str was null, otherwise returns the
	 * 			closest matching file action
	 */
	
	public static FileAction fromString(String str) {
		if (str == null) {
			return null;
		}

		for (String name : names) {
			if ((name != null) && name.equalsIgnoreCase(str)) {
				try {
					if (str.contains(" ")) {
						return FileAction.valueOf(name.toUpperCase().replace(' ', '_'));
					} else if (str.contains("/")) {
						return FileAction.valueOf(name.toUpperCase().replace('/', '_'));
					} else {
						return FileAction.valueOf(name.toUpperCase());
					}
				} catch (IllegalArgumentException iae) {
					Log.error("Bad conversion attempt in FileAction.fromString; string: "
							+ str + "; message: " + iae.getMessage());
					Log.exception(iae);
					return UNKNOWN;
				}
			}
		}

		return UNKNOWN;
	}
	
	/**
	 * Provide a string representation that looks like the same actions
	 * seen through the p4 command interpreter rather than the raw enum.
	 * 
	 * @see java.lang.Enum#toString()
	 */
	public String toString() {
		int ord = this.ordinal();
		if (ord >= names.length) {
			throw new P4JavaError("name / ordinal mismatch in FileAction.toString; "
					+ "ord: " + ord + "; names.length: " + names.length);
		}
		
		return names[ord];
	}
}
