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
 * Options-based method options for IClient revertFiles method(s).
 *
 * @see com.perforce.p4java.client.IClient#revertFiles(java.util.List, com.perforce.p4java.option.client.RevertFilesOptions)
 */

public class RevertFilesOptions extends Options {

	/**
	 * Options: -n, -cN, -a, -k, -w
	 */
	public static final String OPTIONS_SPECS = "b:n i:c:cl b:a b:k b:w";

	/**
	 * If true, don't actually do the revert, just return the files that
	 * would have been opened for reversion. Corresponds to -n.
	 */
	protected boolean noUpdate = false;

	/**
	 * If non-negative, limits reversion to files opened under the given,
	 * pending changelist. Corresponds to -c.
	 */
	protected int changelistId = IChangelist.UNKNOWN;

	/**
	 * If true, revert only files which are opened for edit or integrate and
	 * are unchanged or missing. Corresponds to -a.
	 */
	protected boolean revertOnlyUnchanged = false;

	/**
	 * If true bypass the client file refresh. Corresponds to -k.
	 */
	protected boolean noClientRefresh = false;

	/**
	 * If true causes files that are open for add to be deleted from the
	 * workspace when they are reverted. Corresponds to -w.
	 */
	protected boolean wipeAddFiles = false;

	/**
	 * Default constructor.
	 */
	public RevertFilesOptions() {
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
	public RevertFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public RevertFilesOptions(boolean noUpdate, int changeListId,
			boolean revertOnlyUnchanged, boolean noClientRefresh) {
		super();
		this.noUpdate = noUpdate;
		this.changelistId = changeListId;
		this.revertOnlyUnchanged = revertOnlyUnchanged;
		this.noClientRefresh = noClientRefresh;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.noUpdate,
								this.changelistId,
								this.revertOnlyUnchanged,
								this.noClientRefresh,
								this.wipeAddFiles);
		return this.optionList;
	}

	public boolean isNoUpdate() {
		return noUpdate;
	}

	public RevertFilesOptions setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
		return this;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public RevertFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public boolean isRevertOnlyUnchanged() {
		return revertOnlyUnchanged;
	}

	public RevertFilesOptions setRevertOnlyUnchanged(boolean revertOnlyUnchanged) {
		this.revertOnlyUnchanged = revertOnlyUnchanged;
		return this;
	}

	public boolean isNoClientRefresh() {
		return noClientRefresh;
	}

	public RevertFilesOptions setNoClientRefresh(boolean noClientRefresh) {
		this.noClientRefresh = noClientRefresh;
		return this;
	}

	public boolean isWipeAddFiles() {
		return wipeAddFiles;
	}

	public RevertFilesOptions setWipeAddFiles(boolean wipeAddFiles) {
		this.wipeAddFiles = wipeAddFiles;
		return this;
	}
}
