/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IClient 'populateFiles' method.
 * 
 * @since 2012.3
 */
public class PopulateFilesOptions extends Options {

	/**
	 * Options: -d[description], -f, -m[max] -n, -o, -b[branch], -S[stream],
	 * -P[parentStream], -r, -s.
	 */
	public static final String OPTIONS_SPECS = "s:d b:f i:m:gtz b:n b:o s:b s:S s:P b:r b:s";

	/**
	 * If non-null, use this as the description for the submitted changelist.
	 * <p>
	 * Corresponds to -d flag.
	 */
	public String description = null;

	/**
	 * If positive, copy only the first maxFiles files.
	 * <p>
	 * 
	 * Corresponds to -m flag.
	 */
	protected int maxFiles = 0;

	/**
	 * If true, forces deleted files to be branched into the target. By default,
	 * deleted files are treated as nonexistent and simply skipped.
	 * <p>
	 * 
	 * Corresponds to -f flag.
	 */
	protected boolean forceBranchDeletedFiles = false;

	/**
	 * If true, don't actually do the populate.
	 * <p>
	 * 
	 * Corresponds to -n flag.
	 */
	protected boolean noUpdate = false;

	/**
	 * If true, return a list of files created by the populate command.
	 * <p>
	 * 
	 * Corresponds to -o flag.
	 */
	protected boolean showPopulatedFiles = false;

	/**
	 * If true, this is a 'bidirectional' populate. The -s flag can be used with
	 * -b to cause fromFile to be treated as the source, and both sides of the
	 * user-defined branch view to be treated as the target, per the branch view
	 * mapping. Optional toFile arguments may be given to further restrict the
	 * scope of the target file set. -r is ignored when -s is used.
	 * <p>
	 * 
	 * Corresponds to -s flag.
	 */
	protected boolean bidirectional = false;

	/**
	 * Reverse the mappings in the branch view, with the target files and source
	 * files exchanging place.
	 * <p>
	 * 
	 * Corresponds to the -r flag.
	 */
	protected boolean reverseMapping = false;

	/**
	 * If non-null, use a user-defined branch view. The source is the left side
	 * of the branch view and the target is the right side. With -r, the
	 * direction is reversed.
	 * <p>
	 * 
	 * Corresponds to -b flag.
	 */
	protected String branch = null;

	/**
	 * If non-null, use this stream's branch view. The source is the stream
	 * itself, and the target is the stream's parent. With -r, the direction is
	 * reversed. -P can be used to specify a parent stream other than the
	 * stream's actual parent. Note that to submit copied stream files, the
	 * current client must be dedicated to the target stream.
	 * <p>
	 * 
	 * Corresponds to -S flag.
	 */
	protected String stream = null;

	/**
	 * If non-null, specify a parent stream other than the stream's actual
	 * parent.
	 * <p>
	 * 
	 * Corresponds to -P flag.
	 */
	protected String parentStream = null;

	/**
	 * Default constructor.
	 */
	public PopulateFilesOptions() {
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
	 * @param options
	 *            the options
	 * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
	 */
	public PopulateFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Instantiates a new populate files options.
	 * 
	 * @param description
	 *            the description
	 * @param forceBranchDeletedFiles
	 *            force branch of deleted files
	 * @param maxFiles
	 *            max files
	 * @param noUpdate
	 *            no update
	 * @param showPopulatedFiles
	 *            show populated files
	 */
	public PopulateFilesOptions(String description,
			boolean forceBranchDeletedFiles, int maxFiles, boolean noUpdate,
			boolean showPopulatedFiles) {
		super();
		this.description = description;
		this.forceBranchDeletedFiles = forceBranchDeletedFiles;
		this.maxFiles = maxFiles;
		this.noUpdate = noUpdate;
		this.showPopulatedFiles = showPopulatedFiles;
	}

	/**
	 * Instantiates a new populate files options.
	 * 
	 * @param description
	 *            the description
	 * @param forceBranchDeletedFiles
	 *            force branch of deleted files
	 * @param maxFiles
	 *            max files
	 * @param noUpdate
	 *            no update
	 * @param showPopulatedFiles
	 *            show populated files
	 * @param branch
	 *            the branch
	 * @param reverseMapping
	 *            reverse mapping
	 * @param bidirectional
	 *            bidirectional
	 */
	public PopulateFilesOptions(String description,
			boolean forceBranchDeletedFiles, int maxFiles, boolean noUpdate,
			boolean showPopulatedFiles, String branch, boolean reverseMapping,
			boolean bidirectional) {
		super();
		this.description = description;
		this.forceBranchDeletedFiles = forceBranchDeletedFiles;
		this.maxFiles = maxFiles;
		this.noUpdate = noUpdate;
		this.showPopulatedFiles = showPopulatedFiles;
		this.branch = branch;
		this.reverseMapping = reverseMapping;
		this.bidirectional = bidirectional;
	}

	/**
	 * Instantiates a new populate files options.
	 * 
	 * @param description
	 *            the description
	 * @param forceBranchDeletedFiles
	 *            force branch of deleted files
	 * @param maxFiles
	 *            max files
	 * @param noUpdate
	 *            no update
	 * @param showPopulatedFiles
	 *            show populated files
	 * @param stream
	 *            the stream
	 * @param parentStream
	 *            the parent stream
	 * @param reverseMapping
	 *            reverse mapping
	 */
	public PopulateFilesOptions(String description,
			boolean forceBranchDeletedFiles, int maxFiles, boolean noUpdate,
			boolean showPopulatedFiles, String stream, String parentStream,
			boolean reverseMapping) {
		super();
		this.description = description;
		this.forceBranchDeletedFiles = forceBranchDeletedFiles;
		this.maxFiles = maxFiles;
		this.noUpdate = noUpdate;
		this.showPopulatedFiles = showPopulatedFiles;
		this.stream = stream;
		this.parentStream = parentStream;
		this.reverseMapping = reverseMapping;
	}

	/**
	 * Process options.
	 * 
	 * @param server
	 *            the server
	 * @return the list
	 * @throws OptionsException
	 *             the options exception
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.getDescription(), this.isForceBranchDeletedFiles(),
				this.getMaxFiles(), this.isNoUpdate(),
				this.isShowPopulatedFiles(), this.getBranch(),
				this.getStream(), this.getParentStream(),
				this.isReverseMapping(), this.isBidirectional());
		return this.optionList;
	}

	/**
	 * Gets the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 * 
	 * @param description
	 *            the description
	 * @return the populate files options
	 */
	public PopulateFilesOptions setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Gets the max files.
	 * 
	 * @return the max files
	 */
	public int getMaxFiles() {
		return maxFiles;
	}

	/**
	 * Sets the max files.
	 * 
	 * @param maxFiles
	 *            the max files
	 * @return the populate files options
	 */
	public PopulateFilesOptions setMaxFiles(int maxFiles) {
		this.maxFiles = maxFiles;
		return this;
	}

	/**
	 * Checks if is force branch deleted files.
	 * 
	 * @return true, if is force branch deleted files
	 */
	public boolean isForceBranchDeletedFiles() {
		return forceBranchDeletedFiles;
	}

	/**
	 * Sets the force branch deleted files.
	 * 
	 * @param forceBranchDeletedFiles
	 *            the force branch deleted files
	 * @return the populate files options
	 */
	public PopulateFilesOptions setForceBranchDeletedFiles(
			boolean forceBranchDeletedFiles) {
		this.forceBranchDeletedFiles = forceBranchDeletedFiles;
		return this;
	}

	/**
	 * Checks if is show populated files.
	 * 
	 * @return true, if is show populated files
	 */
	public boolean isShowPopulatedFiles() {
		return showPopulatedFiles;
	}

	/**
	 * Sets the show populated files.
	 * 
	 * @param showPopulatedFiles
	 *            the show populated files
	 * @return the populate files options
	 */
	public PopulateFilesOptions setShowPopulatedFiles(boolean showPopulatedFiles) {
		this.showPopulatedFiles = showPopulatedFiles;
		return this;
	}

	/**
	 * Checks if is no update.
	 * 
	 * @return true, if is no update
	 */
	public boolean isNoUpdate() {
		return noUpdate;
	}

	/**
	 * Sets the no update.
	 * 
	 * @param noUpdate
	 *            the no update
	 * @return the populate files options
	 */
	public PopulateFilesOptions setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
		return this;
	}

	/**
	 * Checks if is bidirectional.
	 * 
	 * @return true, if is bidirectional
	 */
	public boolean isBidirectional() {
		return bidirectional;
	}

	/**
	 * Sets the bidirectional.
	 * 
	 * @param bidirectional
	 *            the bidirectional
	 * @return the populate files options
	 */
	public PopulateFilesOptions setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
		return this;
	}

	/**
	 * Checks if is reverse mapping.
	 * 
	 * @return true, if is reverse mapping
	 */
	public boolean isReverseMapping() {
		return reverseMapping;
	}

	/**
	 * Sets the reverse mapping.
	 * 
	 * @param reverseMapping
	 *            the reverse mapping
	 * @return the populate files options
	 */
	public PopulateFilesOptions setReverseMapping(boolean reverseMapping) {
		this.reverseMapping = reverseMapping;
		return this;
	}

	/**
	 * Gets the branch.
	 * 
	 * @return the branch
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * Sets the branch.
	 * 
	 * @param branch
	 *            the branch
	 * @return the populate files options
	 */
	public PopulateFilesOptions setBranch(String branch) {
		this.branch = branch;
		return this;
	}

	/**
	 * Gets the stream.
	 * 
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * Sets the stream.
	 * 
	 * @param stream
	 *            the stream
	 * @return the populate files options
	 */
	public PopulateFilesOptions setStream(String stream) {
		this.stream = stream;
		return this;
	}

	/**
	 * Gets the parent stream.
	 * 
	 * @return the parent stream
	 */
	public String getParentStream() {
		return parentStream;
	}

	/**
	 * Sets the parent stream.
	 * 
	 * @param parentStream
	 *            the parent stream
	 * @return the populate files options
	 */
	public PopulateFilesOptions setParentStream(String parentStream) {
		this.parentStream = parentStream;
		return this;
	}
}
