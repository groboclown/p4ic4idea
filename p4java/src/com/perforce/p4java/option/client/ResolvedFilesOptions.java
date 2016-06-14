/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IClient.resolvedFiles method.
 * 
 * @see com.perforce.p4java.client.IClient#resolvedFiles(java.util.List, com.perforce.p4java.option.client.ResolvedFilesOptions)
 */
public class ResolvedFilesOptions extends Options {

	/**
	 * Options: -o
	 */
	public static final String OPTIONS_SPECS = "b:o";
	
	/**
	 * If true, report the revision used as the base during the resolve.
	 * Corresponds to the -o flag.
	 */
	protected boolean showBaseRevision = false;
	
	/**
	 * Default constructor.
	 */
	public ResolvedFilesOptions() {
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
	public ResolvedFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public ResolvedFilesOptions(boolean showBaseRevision) {
		super();
		this.showBaseRevision = showBaseRevision;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
										this.isShowBaseRevision()
								);
		return this.optionList;
	}

	public boolean isShowBaseRevision() {
		return showBaseRevision;
	}

	public ResolvedFilesOptions setShowBaseRevision(boolean showBaseRevision) {
		this.showBaseRevision = showBaseRevision;
		return this;
	}
}
