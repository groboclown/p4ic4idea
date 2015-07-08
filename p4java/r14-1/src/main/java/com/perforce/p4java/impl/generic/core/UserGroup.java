/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.server.IServer;

/**
 * Simple generic implementation class for the IUserGroup interface.
 */

public class UserGroup extends ServerResource implements IUserGroup {
	
	/**
	 * How the Perforce server represents an unset group value as a string.
	 */
	public static final String UNSET_STR = "unset";
	
	/**
	 * How the Perforce server represents an unlimited group value as a string.
	 */
	public static final String UNLIMITED_STR = "unlimited";
	
	private String name = null;
	private int maxResults = UNSET;
	private int maxScanRows = UNSET;
	private int maxLockTime = UNSET;
	private int timeout = UNSET;
	private int passwordTimeout = UNSET;
	private boolean subGroup = false;
	private List<String> subgroups = null;
	private List<String> owners = null;
	private List<String> users = null;
	
	/**
	 * Simple convenience factory method to return a new local UserGroup object.<p>
	 * 
	 * All fields not passed as parameters here default to the defaults applied by
	 * the associated default UserGroup constructor.
	 * 
	 * @param name non-null name for the UserGroup.
	 * @param users possibly-null list of users to be associated with the group.
	 * @return new user group.
	 */
	public static UserGroup newUserGroup(String name, List<String> users) {
		if (name == null) {
			throw new NullPointerError("null user group name in UserGroup.newUserGroup()");
		}
		
		UserGroup group = new UserGroup();
		group.setName(name);
		group.setUsers(users);
		
		return group;
	}
	
	/**
	 * Default constructor. Sets all fields to null, UNSET, or false.
	 * Sets superclass IServerResource fields complete, completeable,
	 * refereable and updateable to true.
	 */
	public UserGroup() {
		super(true, true);
	}
	
	/**
	 * Construct a new user group impl from the passed-in map. Note that this
	 * map must come from the Perforce "group" command or exact equivalent; using
	 * a map passed back by (e.g.) the Perforce "groups" (note the plural) command
	 * will fail due to the way the Perforce server returns group lists rather
	 * than individual groups. Calling this with a null map argument is
	 * equivalent to calling the default constructor.<p>
	 * 
	 * Sets superclass IServerResource fields complete, completeable,
	 * refereable and updateable to true.
	 */
	public UserGroup(Map<String, Object> map) {
		super(true, true);
		if (map != null) {
			try {
				this.name = (String) map.get(MapKeys.GROUP_KEY);
				this.maxLockTime = parseGroupIntValue((String) map.get(MapKeys.MAXLOCKTIME_KEY));
				this.maxResults = parseGroupIntValue((String) map.get(MapKeys.MAXRESULTS_KEY));
				this.maxScanRows = parseGroupIntValue((String) map.get(MapKeys.MAXSCANROWS_KEY));
				this.timeout = parseGroupIntValue((String) map.get(MapKeys.TIMEOUT_KEY));
				this.passwordTimeout = parseGroupIntValue((String) map.get(MapKeys.PASSWORD_TIMEOUT_KEY));
				
				String key = MapKeys.USERS_KEY;
				for (int i = 0; ; i++) {
					if (!map.containsKey(key + i)) {
						break;
					} else {
						if (this.users == null) {
							this.users = new ArrayList<String>();
						}
						this.users.add((String) map.get(key + i));
					}
				}
				
				key = MapKeys.OWNERS_KEY;
				for (int i = 0; ; i++) {
					if (!map.containsKey(key + i)) {
						break;
					} else {
						if (this.owners == null) {
							this.owners = new ArrayList<String>();
						}
						this.owners.add((String) map.get(key + i));
					}
				}
				
				key = MapKeys.SUBGROUPS_KEY;
				for (int i = 0; ; i++) {
					if (!map.containsKey(key + i)) {
						break;
					} else {
						if (this.subgroups == null) {
							this.subgroups = new ArrayList<String>();
						}
						this.subgroups.add((String) map.get(key + i));
					}
				}
			} catch (Throwable thr) {
				Log.warn("Unexpected exception in UserGroup constructor: "
											+ thr.getMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getMaxLockTime()
	 */
	public int getMaxLockTime() {
		return this.maxLockTime;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getMaxResults()
	 */
	public int getMaxResults() {
		return this.maxResults;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getMaxScanRows()
	 */
	public int getMaxScanRows() {
		return this.maxScanRows;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getName()
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getOwners()
	 */
	public List<String> getOwners() {
		return this.owners;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getSubgroups()
	 */
	public List<String> getSubgroups() {
		return this.subgroups;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getTimeout()
	 */
	public int getTimeout() {
		return this.timeout;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getUsers()
	 */
	public List<String> getUsers() {
		return this.users;
	}
	
	/**
	 * @see com.perforce.p4java.core.IUserGroup#isSubGroup()
	 */
	public boolean isSubGroup() {
		return this.subGroup;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public void setMaxScanRows(int maxScanRows) {
		this.maxScanRows = maxScanRows;
	}

	public void setMaxLockTime(int maxLockTime) {
		this.maxLockTime = maxLockTime;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setSubgroups(List<String> subgroups) {
		this.subgroups = subgroups;
	}

	public void setOwners(List<String> owners) {
		this.owners = owners;
	}

	public void setUsers(List<String> users) {
		this.users = users;
	}

	public void setSubGroup(boolean subGroup) {
		this.subGroup = subGroup;
	}
	
	/**
	 * Parse a Perforce server-side string representing a user group
	 * integer value (such as timeout). Copes with "unset" and
	 * "unlimited" properly.
	 */
	public int parseGroupIntValue(String str) {
		if (str != null) {
			if (str.equalsIgnoreCase(UNSET_STR)) {
				return UNSET;
			} else if (str.equalsIgnoreCase(UNLIMITED_STR)) {
				return UNLIMITED;
			} else {
				return new Integer(str);
			}
		}
		return UNSET;
	}
	
	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	@Override
	public void refresh() throws ConnectionException, RequestException,
										AccessException {
		IServer refreshServer = this.server;
		String refreshName = this.getName();
		if (refreshServer != null && refreshName != null) {
			IUserGroup refreshedUserGroup = refreshServer.getUserGroup(refreshName);
			if (refreshedUserGroup != null) {
				this.maxLockTime = refreshedUserGroup.getMaxLockTime();
				this.name = refreshedUserGroup.getName();
				this.maxResults = refreshedUserGroup.getMaxResults();
				this.maxScanRows = refreshedUserGroup.getMaxScanRows();
				this.owners = refreshedUserGroup.getOwners();
				this.subGroup = refreshedUserGroup.isSubGroup();
				this.timeout = refreshedUserGroup.getTimeout();
				this.subgroups = refreshedUserGroup.getSubgroups();
				this.users = refreshedUserGroup.getUsers();
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	@Override
	public void update()
		throws ConnectionException, RequestException, AccessException {
			this.server.updateUserGroup(this, false);
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#getPasswordTimeout()
	 */
	public int getPasswordTimeout() {
		return passwordTimeout;
	}

	/**
	 * @see com.perforce.p4java.core.IUserGroup#setPasswordTimeout(int)
	 */
	public void setPasswordTimeout(int passwordTimeout) {
		this.passwordTimeout = passwordTimeout;
	}
}
