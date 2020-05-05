/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

import com.perforce.p4java.core.IMapEntry;

/**
 * Describes a protection entry (line) in a Perforce protection
 * table. These are described in more detail in the various
 * main Perforce admin documentation pages.<p>
 * 
 * Note that the pathExcluded methods return whether a path is
 * excluded or unmapped from the user's permissions; this
 * corresponds to the "-" (minus sign) prepended to the path
 * on the p4 command version of p4 protects, and <i>must</i>
 * be used to determine whether the path is accessible or not.
 */

public interface IProtectionEntry extends IMapEntry {
	
	/**
	 * Gets the protection mode for this entry.
	 * 
	 * @return the protection mode
	 */
	String getMode();
	
	/**
	 * Sets the protection mode for this entry.
	 * 
	 * @param mode
	 *            the protection mode
	 */
	void setMode(String mode);
	
	/**
	 * Checks if the grantee is a group.
	 * 
	 * @return true, if the grantee is a group.
	 */
	boolean isGroup();
	
	/**
	 * Sets the group indicator (true/false).
	 * 
	 * @param group
	 *            the group indicator (true/false).
	 */
	void setGroup(boolean group);
	
	/**
	 * Gets the name of the grantee (user or group).
	 * 
	 * @return the name of the grantee
	 */
	String getName();
	
	/**
	 * Sets the name of the grantee (user or group).
	 * 
	 * @param name
	 *            the name of the grantee
	 */
	void setName(String name);
	
	/**
	 * Gets the client host.
	 * 
	 * @return the client host
	 */
	String getHost();
	
	/**
	 * Sets the client host.
	 * 
	 * @param host
	 *            the client host
	 */
	void setHost(String host);
	
	/**
	 * Gets the depot path.
	 * 
	 * @return the depot path
	 */
	String getPath();
	
	/**
	 * Sets the depot path.
	 * 
	 * @param path
	 *            the depot path
	 */
	void setPath(String path);
	
	/**
	 * Checks if the path is excluded.
	 * 
	 * @return true, if the path is excluded
	 */
	boolean isPathExcluded();
	
	/**
	 * Sets the path excluded indicator (true/false).
	 * 
	 * @param pathExcluded
	 *            the path excluded indicator (true/false)
	 */
	void setPathExcluded(boolean pathExcluded);
}
