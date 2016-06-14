/**
 * 
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * IClient reconcileFiles method Options definitions.
 * 
 * @see com.perforce.p4java.client.IClient#addFiles(java.util.List, com.perforce.p4java.option.client.ReconcileFilesOptions) 
 */
public class ReconcileFilesOptions extends Options {
	
	/**
	 * Options: -n, -c[changelist], -e, -a, -f, -I, -d, -l, -m, -w
	 */
	public static final String OPTIONS_SPECS = "b:n i:c:gtz b:e b:a b:f b:I b:d b:l b:m b:w";
	
	/**
	 * If true, don't actually do the add, just return the files that
	 * would have been opened for addition.
	 * Corresponds to the '-n' flag.
	 */
	protected boolean noUpdate = false;
	
	/**
	 * If positive, the opened files are put into the pending
	 * changelist identified by changelistId (this changelist must have been
	 * previously created for this to succeed). If zero or negative, the
	 * file is opened in the 'default' (unnumbered) changelist.
	 * Corresponds to the '-c changelist#' flag.
	 */
	protected int changelistId = 0;
	
	/**
	 * If true, allows the user to reconcile files that have been modified
	 * outside of Perforce. The reconcile command will open these files for edit.
	 * Corresponds to the '-e' flag.
	 */
	protected boolean outsideEdit = false;
	
	/**
	 * If true, allows the user to reconcile files that are in the user's
	 * directory that are not under Perforce source control. These files are
	 * opened for add.
	 * Corresponds to the '-a' flag.
	 */
	protected boolean outsideAdd = false;

	/**
	 * If true, filenames that contain wildcards are permitted.
	 * See the main Perforce documentation for file adding for details.
	 * Corresponds to the '-f' flag.
	 */
	protected boolean useWildcards = false;

	/**
	 * If true, informs the client that it should not perform any ignore checking.
	 * Corresponds to the '-I' flag
	 */
	protected boolean noIgnoreChecking = false;
	
	/**
	 * If true, allows the user to reconcile files that have been removed from
	 * the user's directory but are still in the depot. These files will be
	 * opened for delete only if they are still on the user's have list.
	 * Corresponds to the '-d' flag.
	 */
	protected boolean removed = false;

	/**
	 * If true, requests output in local file syntax using relative paths,
	 * similar to the workspace-centric view provided by 'status'.
	 * Corresponds to the '-l' flag.
	 */
	protected boolean localSyntax = false;

	/**
	 * If true, used in conjunction with '-e' can be used to minimize costly
	 * digest computation on the client by checking file modification times
	 * before checking digests to determine if files have been modified outside
	 * of Perforce. Corresponds to the '-m' flag.
	 */
	protected boolean checkModTime = false;

	/**
	 * If true, forces the workspace files to be updated to match the depot
	 * rather than opening them so that the depot can be updated to match the
	 * workspace. Files that are not under source control will be deleted, and
	 * modified or deleted files will be refreshed. Note that this operation
	 * will result in the loss of any changes made to unopened files. This
	 * option requires read permission. Corresponds to the '-w' flag.
	 */
	protected boolean updateWorkspace = false;

	/**
	 * Default constructor.
	 */
	public ReconcileFilesOptions() {
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
	public ReconcileFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public ReconcileFilesOptions(boolean noUpdate, int changelistId, boolean outsideEdit,
			boolean outsideAdd, boolean useWildcards, boolean noIgnoreChecking, boolean removed,
			boolean localSyntax) {
		super();
		this.noUpdate = noUpdate;
		this.changelistId = changelistId;
		this.outsideEdit = outsideEdit;
		this.outsideAdd = outsideAdd;
		this.useWildcards = useWildcards;
		this.noIgnoreChecking = noIgnoreChecking;
		this.removed = removed;
		this.localSyntax = localSyntax;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.noUpdate,
								this.changelistId,
								this.outsideEdit,
								this.outsideAdd,
								this.useWildcards,
								this.noIgnoreChecking,
								this.removed,
								this.localSyntax,
								this.checkModTime,
								this.updateWorkspace);
		return this.optionList;
	}

	public boolean isNoUpdate() {
		return noUpdate;
	}

	public ReconcileFilesOptions setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
		return this;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public ReconcileFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public boolean getOutsideEdit() {
		return outsideEdit;
	}

	public ReconcileFilesOptions setOutsideEdit(boolean outsideEdit) {
		this.outsideEdit = outsideEdit;
		return this;
	}

	public boolean getOutsideAdd() {
		return outsideAdd;
	}

	public ReconcileFilesOptions setOutsideAdd(boolean outsideAdd) {
		this.outsideAdd = outsideAdd;
		return this;
	}

	public boolean isUseWildcards() {
		return useWildcards;
	}

	public ReconcileFilesOptions setUseWildcards(boolean useWildcards) {
		this.useWildcards = useWildcards;
		return this;
	}

	public boolean isNoIgnoreChecking() {
		return noIgnoreChecking;
	}
	
	public ReconcileFilesOptions setNoIgnoreChecking(boolean noIgnoreChecking) {
		this.noIgnoreChecking = noIgnoreChecking;
		return this;
	}

	public boolean isRemoved() {
		return removed;
	}
	
	public ReconcileFilesOptions setRemoved(boolean removed) {
		this.removed = removed;
		return this;
	}

	public boolean isLocalSyntax() {
		return localSyntax;
	}
	
	public ReconcileFilesOptions setLocalSyntax(boolean localSyntax) {
		this.localSyntax = localSyntax;
		return this;
	}

	public boolean isCheckModTime() {
		return checkModTime;
	}
	
	public ReconcileFilesOptions setCheckModTime(boolean checkModTime) {
		this.checkModTime = checkModTime;
		return this;
	}

	public boolean isUpdateWorkspace() {
		return updateWorkspace;
	}
	
	public ReconcileFilesOptions setUpdateWorkspace(boolean updateWorkspace) {
		this.updateWorkspace = updateWorkspace;
		return this;
	}
}
