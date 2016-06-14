/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for server getUserGroups method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getUserGroups(java.lang.String, com.perforce.p4java.option.server.GetUserGroupsOptions)
 */
public class GetUserGroupsOptions extends Options {
	
	/**
	 * Options: -m[max], -i, -v, -g, -u, -o
	 */
	public static final String OPTIONS_SPECS = "i:m:gtz b:i b:v b:g b:u b:o";
	
	/**
	 * If positive, return only the first maxGroups groups.
	 * Corresponds to the -m flag.
	 */
	protected int maxGroups = 0;
	
	/**
	 * If true, also display groups that the specified user or group belongs
	 * to indirectly via subgroups. Corresponds to the -i flag.
	 */
	protected boolean indirect = false;
	
	/**
	 * If true, display the MaxResults, MaxScanRows, MaxLockTime, 
	 * and Timeout values for the named group. Corresponds to the
	 * -v flag.
	 */
	protected boolean displayValues = false;
	
	/**
	 * If true, indicates that the 'name' argument is a group. Corresponds to
	 * the -g flag.
	 */
	protected boolean groupName = false;

	/**
	 * If true, indicates that the 'name' argument is a user. Corresponds to
	 * the -u flag.
	 */
	protected boolean userName = false;

	/**
	 * If true, indicates that the 'name' argument is an owner. Corresponds to
	 * the -o flag.
	 */
	protected boolean ownerName = false;

	/**
	 * Default constructor.
	 */
	public GetUserGroupsOptions() {
		super();
	}

	/**
	 * Strings-based constructor; see 'p4 help [command]' for possible options.
	 * <p>
	 * 
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 * 
	 * <b>NOTE: setting options this way always bypasses the internal options
	 * values, and getter methods against the individual values corresponding to
	 * the strings passed in to this constructor will not normally reflect the
	 * string's setting. Do not use this constructor unless you know what you're
	 * doing and / or you do not also use the field getters and setters.</b>
	 * 
	 * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
	 */
	public GetUserGroupsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetUserGroupsOptions(int maxGroups, boolean indirect,
			boolean displayValues) {
		super();
		this.maxGroups = maxGroups;
		this.indirect = indirect;
		this.displayValues = displayValues;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.getMaxGroups(),
											this.isIndirect(),
											this.isDisplayValues(),
											this.isGroupName(),
											this.isUserName(),
											this.isOwnerName());
		return this.optionList;
	}

	public int getMaxGroups() {
		return maxGroups;
	}

	public GetUserGroupsOptions setMaxGroups(int maxGroups) {
		this.maxGroups = maxGroups;
		return this;
	}

	public boolean isIndirect() {
		return indirect;
	}

	public GetUserGroupsOptions setIndirect(boolean indirect) {
		this.indirect = indirect;
		return this;
	}

	public boolean isDisplayValues() {
		return displayValues;
	}

	public GetUserGroupsOptions setDisplayValues(boolean displayValues) {
		this.displayValues = displayValues;
		return this;
	}

	public boolean isGroupName() {
		return groupName;
	}

	public GetUserGroupsOptions setGroupName(boolean groupName) {
		this.groupName = groupName;
		return this;
	}

	public boolean isUserName() {
		return userName;
	}

	public GetUserGroupsOptions setUserName(boolean userName) {
		this.userName = userName;
		return this;
	}

	public boolean isOwnerName() {
		return ownerName;
	}

	public GetUserGroupsOptions setOwnerName(boolean ownerName) {
		this.ownerName = ownerName;
		return this;
	}
}
