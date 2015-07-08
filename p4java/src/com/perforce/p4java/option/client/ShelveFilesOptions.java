/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IClient.shelveFiles method and associated
 * convenience methods.
 * 
 * @see com.perforce.p4java.client.IClient#shelveFiles(List, int, com.perforce.p4java.option.client.ShelveFilesOptions)
 */
public class ShelveFilesOptions extends Options {
	
	/**
	 * Options: -f, -r, -d
	 */
	public static final String OPTIONS_SPECS = "b:f b:r b:d";

	/** If true, force the shelve operation; corresponds to the -f flag */
	protected boolean forceShelve = false;
	
	/** If true, allow the incoming files to replace the shelved files;
	 * corresponds to the -r flag.
	 */
	protected boolean replaceFiles = false;
	
	/** If true, delete incoming files from the shelf; corresponds to the -d flag. */
	protected boolean deleteFiles = false;

	/**
	 * Default constructor..
	 */
	public ShelveFilesOptions() {
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
	 * @see com.perforce.p4java.option.Options#Options(String...)
	 */
	public ShelveFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public ShelveFilesOptions(boolean forceShelve, boolean replaceFiles,
			boolean deleteFiles) {
		super();
		this.forceShelve = forceShelve;
		this.replaceFiles = replaceFiles;
		this.deleteFiles = deleteFiles;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isForceShelve(),
								this.isReplaceFiles(),
								this.isDeleteFiles());
		return this.optionList;
	}

	public boolean isForceShelve() {
		return forceShelve;
	}

	public ShelveFilesOptions setForceShelve(boolean forceShelve) {
		this.forceShelve = forceShelve;
		return this;
	}

	public boolean isReplaceFiles() {
		return replaceFiles;
	}

	public ShelveFilesOptions setReplaceFiles(boolean replaceFiles) {
		this.replaceFiles = replaceFiles;
		return this;
	}

	public boolean isDeleteFiles() {
		return deleteFiles;
	}

	public ShelveFilesOptions setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
		return this;
	}
}
