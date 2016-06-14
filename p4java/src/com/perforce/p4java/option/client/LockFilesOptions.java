/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options subclass for IClient.lockFiles.
 * 
 * @see com.perforce.p4java.client.IClient#lockFiles(java.util.List, com.perforce.p4java.option.client.LockFilesOptions)
 */
public class LockFilesOptions extends Options {
	
	/**
	 * Options: -c[changelist]
	 */
	public static final String OPTIONS_SPECS = "i:c:cl";
	
	/**
	 * If positive, use the changelistId given instead of
	 * the default changelist. Corresponds to the -c option.
	 */
	protected int changelistId = IChangelist.UNKNOWN;

	/**
	 * Default constructor.
	 */
	public LockFilesOptions() {
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
	public LockFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public LockFilesOptions(int changelistId) {
		super();
		this.changelistId = changelistId;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
										this.getChangelistId()
								);
		return this.optionList;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public LockFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}
}
