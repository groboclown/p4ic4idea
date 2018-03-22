/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.deleteBranchSpec methods.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#deleteBranchSpec(java.lang.String, com.perforce.p4java.option.server.DeleteBranchSpecOptions)
 */
public class DeleteBranchSpecOptions extends Options {
	
	/**
	 * Options: -f
	 */
	public static final String OPTIONS_SPECS = "b:f";
	
	/**
	 * If true, forces the deletion of any branch; normally branches
	 * can only be deleted by their owner. Corresponds to -f flag.
	 */
	protected boolean force = false;

	/**
	 * Default constructor.
	 */
	public DeleteBranchSpecOptions() {
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
	public DeleteBranchSpecOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public DeleteBranchSpecOptions(boolean force) {
		super();
		this.force = force;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.isForce());
		return this.optionList;
	}

	public boolean isForce() {
		return force;
	}

	public DeleteBranchSpecOptions setForce(boolean force) {
		this.force = force;
		return this;
	}
}
