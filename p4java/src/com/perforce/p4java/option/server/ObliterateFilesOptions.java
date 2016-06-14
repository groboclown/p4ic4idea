/**
 *
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options-based method options for IOptionsServer obliterateFiles method(s).
 * 
 * @see com.perforce.p4java.server.IOptionsServer#obliterateFiles(java.util.List, com.perforce.p4java.option.server.ObliterateFilesOptions)
 */
public class ObliterateFilesOptions extends Options {

	/**
	 * Options: -y, -a, -b, -h
	 */
	public static final String OPTIONS_SPECS = "b:y b:a b:b b:h";

	/**
	 * If true, the '-y' flag executes the obliterate operation. By default,
	 * obliterate displays a preview of the results.
	 */
	protected boolean executeObliterate = false;

	/**
	 * If true, the '-a' flag skips the archive search and removal phase. This
	 * phase of obliterate can take a very long time for sites with big archive
	 * maps (db.archmap). However, file content is not removed; if the file was
	 * a branch, then it's most likely that the archival search is not
	 * necessary. This option is safe to use with the '-b' option.
	 * <p>
	 * 
	 * Note: this is an 'undoc' flag; use 'p4 help undoc' for more details
	 */
	protected boolean skipArchiveSearchRemoval = false;

	/**
	 * If true, the '-b' flag restricts files in the argument range to those
	 * that are branched and are both the first revision and the head revision.
	 * This flag is useful for removing old branches while keeping files of
	 * interest (files that were modified).
	 * <p>
	 * 
	 * Note: this is an 'undoc' flag; use 'p4 help undoc' for more details
	 */
	protected boolean branchedFirstHeadRevOnly = false;

	/**
	 * If true, the '-h' flag instructs obliterate not to search db.have for all
	 * possible matching records to delete. Usually, db.have is one of the
	 * largest tables in a repository and consequently this search takes a long
	 * time. Do not use this flag when obliterating branches or namespaces for
	 * reuse, because the old content on any client will not match the
	 * newly-added repository files.
	 * <p>
	 * 
	 * Note: this is an 'undoc' flag; use 'p4 help undoc' for more details
	 */
	protected boolean skipHaveSearch = false;

	/**
	 * Default constructor.
	 */
	public ObliterateFilesOptions() {
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
	public ObliterateFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public ObliterateFilesOptions(boolean executeObliterate,
			boolean skipArchiveSearchRemoval, boolean branchedFirstHeadRevOnly,
			boolean skipHaveSearch) {
		super();
		this.executeObliterate = executeObliterate;
		this.skipArchiveSearchRemoval = skipArchiveSearchRemoval;
		this.branchedFirstHeadRevOnly = branchedFirstHeadRevOnly;
		this.skipHaveSearch = skipHaveSearch;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.executeObliterate, this.skipArchiveSearchRemoval,
				this.branchedFirstHeadRevOnly, this.skipHaveSearch);

		return this.optionList;
	}

	public boolean isExecuteObliterate() {
		return executeObliterate;
	}

	public ObliterateFilesOptions setExecuteObliterate(boolean executeObliterate) {
		this.executeObliterate = executeObliterate;
		return this;
	}

	public boolean isSkipArchiveSearchRemoval() {
		return skipArchiveSearchRemoval;
	}

	public ObliterateFilesOptions setSkipArchiveSearchRemoval(
			boolean skipArchiveSearchRemoval) {
		this.skipArchiveSearchRemoval = skipArchiveSearchRemoval;
		return this;
	}

	public boolean isBranchedFirstHeadRevOnly() {
		return branchedFirstHeadRevOnly;
	}

	public ObliterateFilesOptions setBranchedFirstHeadRevOnly(
			boolean branchedFirstHeadRevOnly) {
		this.branchedFirstHeadRevOnly = branchedFirstHeadRevOnly;
		return this;
	}

	public boolean isSkipHaveSearch() {
		return skipHaveSearch;
	}

	public ObliterateFilesOptions setSkipHaveSearch(boolean skipHaveSearch) {
		this.skipHaveSearch = skipHaveSearch;
		return this;
	}
}
