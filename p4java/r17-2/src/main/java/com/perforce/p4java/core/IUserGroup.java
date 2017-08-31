/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

import java.util.List;

/**
 * Defines Perforce user group attributes and methods. See the main Perforce
 * documentation for a full detailed description of Perforce user groups and
 * the associated methods defined below.<p>
 * 
 * The IUserGroup interface and implementation objects are somewhat unusual
 * in the P4Java object menagerie in that they do not have corresponding summary
 * objects due to the way the Perforce server handles and communicates
 * Perforce user groups. This may change in the future, but in general,
 * with caveats noted below and elsewhere, IUserGroup objects are complete,
 * updateable, and refreshable, and (as explained) have no summary versions.<p>
 * 
 * Note that the getSubgroups() method is not guaranteed to return correct
 * values for IUserGroup objects returned from the getUserGroupList; you
 * normally have to get the group definition from getUserGroup to see the
 * list of that group's sub groups (this is a Perforce server limitation).
 * Conversely, the isSubGroup() method only works for a user group object
 * returned by the list version of the get group(s) commands.<p>
 * 
 * Setter methods defined below only have local effect unless an object
 * update is performed.
 */

public interface IUserGroup extends IServerResource {
	/**
	 * Used to signal that a specific user group max value (eg. MaxScanRows
	 * or password timeout) is unset.
	 */
	int UNSET = 0;
	
	/**
	 * Used to signal that a specific user group max value (eg. MaxScanRows
	 * or password timeout) is unlimited.
	 */
	int UNLIMITED = -1;
	
	/**
	 * Get the group's name.
	 */
	String getName();
	
	/**
	 * Set the group's name.
	 */
	void setName(String name);
	
	/**
	 * Get the maximum number of results returned for queries by members
	 * of this group.
	 */
	int getMaxResults();
	
	/**
	 * Set the maximum number of results returned for queries by members
	 * of this group.
	 */
	void setMaxResults(int maxResults);
	
	/**
	 * Get the maximum number of scan rows returned for queries by members
	 * of this group.
	 */
	int getMaxScanRows();
	
	/**
	 * Set the maximum number of scan rows returned for queries by members
	 * of this group.
	 */
	void setMaxScanRows(int maxScanRows);
	
	/**
	 * Get the maximum lock time for queries by members of this group.
     * @return max lock time - milliseconds
	 */
	int getMaxLockTime();
	
	/**
	 * Set the maximum lock time for queries by members of this group.
     * @param maxLockTimeOfMilliSeconds - milliseconds
	 */
	void setMaxLockTime(int maxLockTimeOfMilliSeconds);
	
	/**
	 * Get the timeout value for commands by members of this group.
     * @return seconds
	 */
	int getTimeout();
	
	/**
	 * Set the timeout value for commands by members of this group.
     * @param timeoutOfSeconds - seconds
	 */
	void setTimeout(int timeoutOfSeconds);
	
	/**
	 * Get the password timeout value associated with this user group.
     *
     * @return max password time out - seconds
	 * 
	 * @since 2011.1
	 */
	int getPasswordTimeout();
	
	/**
	 * Set the password timeout value associated with this user group.
     *
     * @param passwordTimeoutOfSeconds - seconds
	 * 
	 * @since 2011.1
	 */
	void setPasswordTimeout(int passwordTimeoutOfSeconds);
	
	/**
	 * Return true if this group is a sub group of another group on this server.<p>
	 * 
	 * Note that this method will always return false on an IUserGroup object
	 * retrieved from the IServer.getUserGroup() method (this is a restriction
	 * imposed by the Perforce server).
	 */
	boolean isSubGroup();
	
	/**
	 * Set whether this group is a sub group of another group on this server.<p>
	 */
	void setSubGroup(boolean subGroup);
	
	/**
	 * Get the list of known subgroups of this groups. Will be null or empty if
	 * no subgroups exist or if this user group object was returned from the IServer
	 * getUserGroupList method.
	 */
	List<String> getSubgroups();
	
	/**
	 * Set the list of known subgroups of this groups.
	 */
	void setSubgroups(List<String> subgroups);
	
	/**
	 * Get a list of owner names for this group. Will be null or empty if this group has
	 * no owners.
	 */
	List<String> getOwners();
	
	/**
	 * Set the list of owner names for this group.
	 */
	void setOwners(List<String> owners);
	
	/**
	 * Get a list of user names for this group. Will be null or empty if this group has
	 * no users.
	 */
	List<String> getUsers();
	
	/**
	 * Set the list of user names for this group.
	 */
	void setUsers(List<String> users);
}
