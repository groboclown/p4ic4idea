package com.perforce.p4java.impl.mapbased.client;

import com.perforce.p4java.Log;

/**
 * Enum representing the ViewDepotType which is part of the client spec.
 * LOCAL    - represents the standard classic perforce depot type
 * GRAPH    - represents the git repo based depot type
 * HYBRID   - represents the both LOCAL and GRAPH types
 */
public enum ViewDepotType {

	LOCAL, GRAPH, HYBRID;

	/**
	 * Returns the Enum constant that matches the string argument
	 * @param str The value to matched up to an existing constant
	 * @return An instance of ViewDepotType that matches the argument.
	 */
	public static ViewDepotType fromString(String str) {
		if (str == null) {
			return null;
		}

		try {
			return ViewDepotType.valueOf(str.toUpperCase());
		} catch (IllegalArgumentException iae) {
			Log.error("Bad conversion attempt in ViewDepotType.fromString; string: "
					+ str + "; message: " + iae.getMessage());
			Log.exception(iae);
			return LOCAL;
		}
	}
}
