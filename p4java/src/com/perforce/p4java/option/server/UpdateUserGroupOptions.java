/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Combined Options class for user group create / update / delete methods.
 * Note that not all methods honor all options -- see the individual Javadoc
 * for explanations.
 */
public class UpdateUserGroupOptions extends Options {
	
	/**
	 * Options: -a, -A
	 */
	public static final String OPTIONS_SPECS = "b:a b:A";
	
	/**
	 * If true, allow a user without 'super'
	 * access to modify the group only if that user is an
	 * 'owner' of that group. Corresponds to -a flag.
	 */
	protected boolean updateIfOwner = false;

	/**
	 * If true, enables a user with 'admin' access to add a
	 * new group. Existing groups may not be modified when
	 * this flag is used. Corresponds to -A flag.
	 */
	protected boolean addIfAdmin = false;

	/**
	 * Default constructor.
	 */
	public UpdateUserGroupOptions() {
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
	public UpdateUserGroupOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public UpdateUserGroupOptions(boolean updateIfOwner) {
		super();
		this.updateIfOwner = updateIfOwner;
	}

	/**
	 * Explicit-value constructor.
	 */
	public UpdateUserGroupOptions(boolean updateIfOwner, boolean addIfAdmin) {
		super();
		this.updateIfOwner = updateIfOwner;
		this.addIfAdmin = addIfAdmin;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.isUpdateIfOwner(),
											this.isAddIfAdmin());
		return this.optionList;
	}

	public boolean isUpdateIfOwner() {
		return updateIfOwner;
	}

	public UpdateUserGroupOptions setUpdateIfOwner(boolean updateIfOwner) {
		this.updateIfOwner = updateIfOwner;
		return this;
	}

	public boolean isAddIfAdmin() {
		return addIfAdmin;
	}

	public UpdateUserGroupOptions setAddIfAdmin(boolean addIfAdmin) {
		this.addIfAdmin = addIfAdmin;
		return this;
	}
}
