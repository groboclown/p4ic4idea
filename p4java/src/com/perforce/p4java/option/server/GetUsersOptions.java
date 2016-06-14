/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for server getUsers method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getUsers(java.util.List, com.perforce.p4java.option.server.GetUsersOptions)
 */
public class GetUsersOptions extends Options {
	
	/**
	 * Options: -m[max], -l, -a
	 */
	public static final String OPTIONS_SPECS = "i:m:gtz b:a b:l";
	
	/**
	 * If positive, return only the first maxUsers users.
	 * Corresponds to the -m flag.
	 */
	protected int maxUsers = 0;
	
	/**
	 * If true, include service users in the returned list;
	 * corresponds to the -a flag.
	 * 
	 * @since 2011.1
	 */
	protected boolean includeServiceUsers = false;
	
	/**
	 * If true, include additional information in the output;
	 * corresponds to the -l flag.
	 * 
	 * @since 2011.1
	 */
	protected boolean extendedOutput = false;

	/**
	 * Default constructor.
	 */
	public GetUsersOptions() {
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
	public GetUsersOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetUsersOptions(int maxUsers) {
		super();
		this.maxUsers = maxUsers;
	}
	
	/**
	 * Explicit-value constructor.
	 * 
	 * @since 2011.1
	 */
	public GetUsersOptions(int maxUsers, boolean includeServiceUsers, boolean extendedOutput) {
		super();
		this.maxUsers = maxUsers;
		this.includeServiceUsers = includeServiceUsers;
		this.extendedOutput = extendedOutput;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.getMaxUsers(),
											this.isIncludeServiceUsers(),
											this.isExtendedOutput());
		return this.optionList;
	}

	public int getMaxUsers() {
		return maxUsers;
	}

	public GetUsersOptions setMaxUsers(int maxUsers) {
		this.maxUsers = maxUsers;
		return this;
	}

	/**
	 * @since 2011.1
	 */
	public boolean isIncludeServiceUsers() {
		return includeServiceUsers;
	}

	/**
	 * @since 2011.1
	 */
	public GetUsersOptions setIncludeServiceUsers(boolean includeServiceUsers) {
		this.includeServiceUsers = includeServiceUsers;
		return this;
	}

	/**
	 * @since 2011.1
	 */
	public boolean isExtendedOutput() {
		return extendedOutput;
	}

	/**
	 * @since 2011.1
	 */
	public GetUsersOptions setExtendedOutput(boolean extendedOutput) {
		this.extendedOutput = extendedOutput;
		return this;
	}
}
