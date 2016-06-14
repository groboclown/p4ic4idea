/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer setFileAttributes method.
 * 
 * @since 2011.1
 */
public class SetFileAttributesOptions extends Options {
	
	/**
	 * Options: -e, -f, -p
	 */
	public static final String OPTIONS_SPECS = "b:e b:f b:p";
	
	/**
	 * If true, indicates values are in hex format.
	 * Corresponds to p4's -e flag.
	 */
	protected boolean hexValue = false;
	
	/**
	 * If true, attributes are set on submitted files.
	 * Corresponds to -f.
	 */
	protected boolean setOnSubmittedFiles = false;
	
	/**
	 * If true, creates attributes whose value will be propagated
	 * when the files are opened with 'p4 add', 'p4 edit', or 'p4 delete'.
	 * Corresponds to -p.
	 */
	protected boolean propagateAttributes = false;

	/**
	 * Default constructor.
	 */
	public SetFileAttributesOptions() {
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
	public SetFileAttributesOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit-value constructor.
	 */
	public SetFileAttributesOptions(boolean hexValue,
			boolean setOnSubmittedFiles, boolean propagateAttributes) {
		super();
		this.hexValue = hexValue;
		this.setOnSubmittedFiles = setOnSubmittedFiles;
		this.propagateAttributes = propagateAttributes;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isHexValue(),
								this.isSetOnSubmittedFiles(),
								this.isPropagateAttributes());

		return this.optionList;
	}

	public boolean isHexValue() {
		return hexValue;
	}

	public SetFileAttributesOptions setHexValue(boolean hexValue) {
		this.hexValue = hexValue;
		return this;
	}

	public boolean isSetOnSubmittedFiles() {
		return setOnSubmittedFiles;
	}

	public SetFileAttributesOptions setSetOnSubmittedFiles(boolean setOnSubmittedFiles) {
		this.setOnSubmittedFiles = setOnSubmittedFiles;
		return this;
	}

	public boolean isPropagateAttributes() {
		return propagateAttributes;
	}

	public SetFileAttributesOptions setPropagateAttributes(boolean propagateAttributes) {
		this.propagateAttributes = propagateAttributes;
		return this;
	}
}
