/**
 * 
 */
package com.perforce.p4java.admin;

import java.util.List;
import java.util.Map;

/**
 * Defines Perforce DB schema associated with a Perforce server (admin / superuser feature).<p>
 * 
 * Usage of this feature is intentionally not documented here in any detail; see
 * "p4 help dbschema" for more useful details.
 */

public interface IDbSchema {
	
	/**
	 * What getVersion returns if there was no version number returned
	 * from the server.
	 */
	final int NOVERSION = -1;
	
	/**
	 * Get the name of the table.
	 */
	
	String getName();
	
	/**
	 * Get the table schema version.
	 */
	int getVersion();
	
	/**
	 * Get a list of column metadata maps. Note that while this list should
	 * never be null, individual map values within it may be null. Field (map
	 * key) names and values are not explained here.
	 */
	List<Map<String, String>> getColumnMetadata();
	
	/**
	 * Set the name of the table.
	 */
	void setName(String name);
	
	/**
	 * Get the table schema version.
	 */
	void setVersion(int version);
	
	/**
	 * Set the colum metadata map.
	 */
	void setColumnMetadata(List<Map<String, String>> columnMetadata);
}
