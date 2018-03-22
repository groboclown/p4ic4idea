/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IClient.unshelveFiles method and associated
 * convenience methods.
 * 
 * @see com.perforce.p4java.client.IClient#unshelveFiles(java.util.List, int, int, com.perforce.p4java.option.client.UnshelveFilesOptions)
 */
public class UnshelveFilesOptions extends Options {

	/**
	 * Options: -f, -n
	 */
	public static final String OPTIONS_SPECS = "b:f b:n";
	
	/** If true, force the unshelve operation; corresponds to the -f flag */
	protected boolean forceUnshelve = false;
	
	/** If true, preview what would be unshelved without actually changing
	 * any files or metadata; corresponds to the -n flag.
	 * */
	protected boolean preview = false;
	
	/**
	 * Default constructor.
	 */
	public UnshelveFilesOptions() {
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
	public UnshelveFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public UnshelveFilesOptions(boolean forceUnshelve, boolean preview) {
		super();
		this.forceUnshelve = forceUnshelve;
		this.preview = preview;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isForceUnshelve(),
								this.isPreview());
		return this.optionList;
	}

	public boolean isForceUnshelve() {
		return forceUnshelve;
	}

	public UnshelveFilesOptions setForceUnshelve(boolean forceUnshelve) {
		this.forceUnshelve = forceUnshelve;
		return this;
	}

	public boolean isPreview() {
		return preview;
	}

	public UnshelveFilesOptions setPreview(boolean preview) {
		this.preview = preview;
		return this;
	}
}
