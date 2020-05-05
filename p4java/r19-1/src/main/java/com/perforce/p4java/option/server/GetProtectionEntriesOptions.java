/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options subclass for the IOptionsServer.getProtectionEntries method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getProtectionEntries(List, GetProtectionEntriesOptions)
 */
public class GetProtectionEntriesOptions extends Options {
	
	/**
	 * Options: -a, -g[group], -u[user], -h[host]
	 */
	public static final String OPTIONS_SPECS = "b:a s:g s:u s:h";
	
	/** If true,protection lines for all users are displayed; corresponds to the -a flag. */
	protected boolean allUsers = false;
	
	/** If not null, only those protection lines that apply to the given host (IP address)
	 * are displayed; corresponds to the -h flag. */
	protected String hostName = null;
	
	/** If not null, protection lines for the named user are displayed; -u flag */
	protected String userName = null;
	
	/** If not null, protection lines for the named group are displayed; -g flag */
	protected String groupName = null;

	/**
	 * Default constructor.
	 */
	public GetProtectionEntriesOptions() {
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
	public GetProtectionEntriesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetProtectionEntriesOptions(boolean allUsers, String hostName,
			String userName, String groupName) {
		super();
		this.allUsers = allUsers;
		this.hostName = hostName;
		this.userName = userName;
		this.groupName = groupName;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
												this.isAllUsers(),
												this.getGroupName(),
												this.getUserName(),
												this.getHostName());
		return this.optionList;
	}

	public boolean isAllUsers() {
		return allUsers;
	}

	public GetProtectionEntriesOptions setAllUsers(boolean allUsers) {
		this.allUsers = allUsers;
		return this;
	}

	public String getHostName() {
		return hostName;
	}

	public GetProtectionEntriesOptions setHostName(String hostName) {
		this.hostName = hostName;
		return this;
	}

	public String getUserName() {
		return userName;
	}

	public GetProtectionEntriesOptions setUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public String getGroupName() {
		return groupName;
	}

	public GetProtectionEntriesOptions setGroupName(String groupName) {
		this.groupName = groupName;
		return this;
	}
}
