/**
 * Copyright 2013 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer's getChangelist and deletePendingChangelist
 * methods, and Changelist's update method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getChangelist(int, com.perforce.p4java.option.server.ChangelistOptions)
 * @see com.perforce.p4java.server.IOptionsServer#deletePendingChangelist(int, com.perforce.p4java.option.server.ChangelistOptions)
 * @see com.perforce.p4java.impl.generic.core.Changelist#update(Options)
 */
public class ChangelistOptions extends Options {
	
	/**
	 * Options: -s, -f, -u, -O
	 */
	public static final String OPTIONS_SPECS = "b:s b:f b:u b:O";
	
	/**
	 * If true, extend the list of jobs to include the fix status for each job.
	 * On new changelists, the fix status begins as the special status 'ignore',
	 * which, if left unchanged simply excludes the job from those being fixed.
	 * Otherwise, the fix status, like that applied with 'p4 fix -s', becomes
	 * the job's status when the changelist is committed. Note that this option
	 * exists to support integration with external defect trackers.
	 */
	protected boolean includeFixStatus = false;

	/**
	 * If true, force the update or deletion of other users' pending
	 * changelists. The -f flag can also force the deletion of submitted
	 * changelists after they have been emptied of files using 'p4 obliterate'.
	 * By default, submitted changelists cannot be changed. The -f flag can also
	 * force display of the 'Description' field in a restricted changelist.
	 * Finally the -f flag can force changing the 'User' of an empty pending
	 * change via -U. The -f flag requires 'admin' access granted by 'p4
	 * protect'. The -f and -u flags are mutually exclusive.
	 */
	protected boolean force = false;
	
	/**
	 * If true, force the update of a submitted change by the owner	of the
	 * change. Only the Jobs, Type, and Description fields can be changed using
	 * the -u flag. The -f and -u flags cannot be used in the same change
	 * command.
	 */
	protected boolean forceUpdateByOwner = false;

	/**
	 * If true, specify that the changelist number is the original number of a
	 * changelist which was renamed on submit. Corresponds to -O flag.
	 */
	protected boolean originalChangelist = false;
	
	/**
	 * Default constructor.
	 */
	public ChangelistOptions() {
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
	public ChangelistOptions(String... options) {
		super(options);
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.isIncludeFixStatus(),
											this.isForce(),
											this.isForceUpdateByOwner(),
											this.isOriginalChangelist());
		return this.optionList;
	}

	public boolean isIncludeFixStatus() {
		return includeFixStatus;
	}

	public ChangelistOptions setIncludeFixStatus(boolean includeFixStatus) {
		this.includeFixStatus = includeFixStatus;
		return this;
	}

	public boolean isForce() {
		return force;
	}

	public ChangelistOptions setForce(boolean force) {
		this.force = force;
		return this;
	}

	public boolean isForceUpdateByOwner() {
		return forceUpdateByOwner;
	}

	public ChangelistOptions setForceUpdateByOwner(boolean forceUpdateByOwner) {
		this.forceUpdateByOwner = forceUpdateByOwner;
		return this;
	}

	public boolean isOriginalChangelist() {
		return originalChangelist;
	}

	public ChangelistOptions setOriginalChangelist(boolean originalChangelist) {
		this.originalChangelist = originalChangelist;
		return this;
	}
}
