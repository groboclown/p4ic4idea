/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Label sync options subclass for use with IClient.labelSync.
 * 
 * @see com.perforce.p4java.client.IClient#labelSync(java.util.List, java.lang.String, com.perforce.p4java.option.client.LabelSyncOptions)
 */
public class LabelSyncOptions extends Options {
	
	/**
	 * Options: -n, -a, -d
	 */
	public static final String OPTIONS_SPECS = "b:n b:a b:d";
	
	/** If true, don't actually do the update (c.f. the p4 -n flag) */
	protected boolean noUpdate = false;
	
	/** If true, add the files in fileSpecs to the label
	 * (c.f. the p4 -a flag) */
	protected boolean addFiles = false;
	
	/** If true, delete the files in fileSpecs from the label
	 * (c.f. the p4 -d flag) */
	protected boolean deleteFiles = false;

	/**
	 * Default constructor.
	 */
	public LabelSyncOptions() {
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
	public LabelSyncOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit-value constructor.
	 */
	public LabelSyncOptions(boolean noUpdate, boolean addFiles, boolean deleteFiles) {
		super();
		this.noUpdate = noUpdate;
		this.addFiles = addFiles;
		this.deleteFiles = deleteFiles;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.noUpdate,
								this.addFiles,
								this.deleteFiles);
		return this.optionList;
	}

	public boolean isNoUpdate() {
		return noUpdate;
	}

	public LabelSyncOptions setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
		return this;
	}

	public boolean isAddFiles() {
		return addFiles;
	}

	public LabelSyncOptions setAddFiles(boolean addFiles) {
		this.addFiles = addFiles;
		return this;
	}

	public boolean isDeleteFiles() {
		return deleteFiles;
	}

	public LabelSyncOptions setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
		return this;
	}
}
