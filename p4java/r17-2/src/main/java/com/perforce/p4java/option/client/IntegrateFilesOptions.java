/**
 *
 */
package com.perforce.p4java.option.client;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.Iterator;
import java.util.List;

/**
 * Options for the IClient.integrateFiles method.<p>
 * <p>
 * Note that this implementation extends the normal default
 * options processing with its own method(s) due to the complexity
 * of the various -D and -R flag options; note also the somewhat odd
 * relationship with the previous IntegrationOptions class.<p>
 * <p>
 * Note also that the current implementation makes no attempt
 * to validate the sanity or otherwise of the various options and
 * their combination.
 *
 * @see com.perforce.p4java.client.IClient#integrateFiles(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.core.file.IFileSpec, java.lang.String, com.perforce.p4java.option.client.IntegrateFilesOptions)
 */
public class IntegrateFilesOptions extends Options {

	/**
	 * Options: -c[changelist], -d, -D[flag], -R[flag], -f, -h, -i, -o, -n, -t, -q, -v, -m[max], -b[branch], -S[stream], -P[parentStream], -r, -s
	 */
	public static final String OPTIONS_SPECS = "i:c:clz b:d b:Di b:Ds b:Dt b:Rb b:Rd b:Rs b:f b:h b:i b:o b:n b:t b:q b:v i:m:gtz s:b s:S s:P b:r b:s b:Ob b:Or b:2";

	/**
	 * If positive, the integrated files are opened in the numbered
	 * pending changelist instead of the default changelist.
	 */
	protected int changelistId = IChangelist.UNKNOWN;

	/**
	 * Cause the branch view to work bidirectionally, where the scope of
	 * the command is limited to integrations whose 'from' files match
	 * fromFile[revRange]. Corresponds to the -s flag, with the fromFile
	 * arg being specified in the main method fromFile parameter.
	 */
	protected boolean bidirectionalInteg = false;

	/**
	 * If true, enable integrations around deleted revisions; equivalent
	 * to -d (i.e. -Ds + -Di + -Dt)
	 */
	protected boolean integrateAroundDeletedRevs = false;

	/**
	 * If the target file has been deleted and the source
	 * file has changed, will re-branch the source file
	 * on top of the target file. A.k.a "-Dt".
	 */
	protected boolean rebranchSourceAfterDelete = false;

	/**
	 * If the source file has been deleted and the target
	 * file has changed, will delete the target file.
	 * A.k.a "-Ds".
	 */
	protected boolean deleteTargetAfterDelete = false;

	/**
	 * If the source file has been deleted and re-added,
	 * will attempt to integrate all outstanding revisions
	 * of the file, including those revisions prior to the
	 * delete.  Normally 'p4 integrate' only considers
	 * revisions since the last add. A.k.a. "-Di".
	 */
	protected boolean integrateAllAfterReAdd = false;

	/**
	 * Schedules 'branch resolves' instead of branching new target files
	 * automatically. A.k.a "-Rb".
	 */
	protected boolean branchResolves = false;

	/**
	 * Schedules 'delete resolves' instead of deleting target files
	 * automatically. A.k.a "-Rd".
	 */
	protected boolean deleteResolves = false;

	/**
	 * Skips cherry-picked revisions already integrated. This can improve
	 * merge results, but can also cause multiple resolves per file to be
	 * scheduled. A.k.a "-Rs".
	 */
	protected boolean skipIntegratedRevs = false;

	/**
	 * Forces integrate to act without regard for previous
	 * integration history. Corresponds to the -f flag.
	 */
	protected boolean forceIntegration = false;

	/**
	 * Causes the target files to be left at the revision
	 * currently on the client (the '#have' revision).
	 * Corresponds to the -h flag.
	 */
	protected boolean useHaveRev = false;

	/**
	 * Enables integration between files that have no
	 * integration history. Corresponds to the -i flag.
	 */
	protected boolean doBaselessMerge = false;

	/**
	 * Display the base file name and revision which will
	 * be used in subsequent resolves if a resolve is needed.
	 * Corresponds to the -o flag.
	 */
	protected boolean displayBaseDetails = false;

	/**
	 * Display what integrations would be necessary but don't
	 * actually do them. Corresponds to the -n flag.
	 */
	protected boolean showActionsOnly = false;

	/**
	 * Reverse the mappings in the branch view, with the
	 * target files and source files exchanging place.
	 * Corresponds to the -r flag.
	 */
	protected boolean reverseMapping = false;

	/**
	 * Propagate the source file's filetype to the target file.
	 * Corresponds to the -t flag.
	 */
	protected boolean propagateType = false;

	/**
	 * If true, suppresses normal output messages. Messages regarding
	 * errors or exceptional conditions are not suppressed.
	 * Corresponds to -q flag.
	 */
	protected boolean quiet = false;

	/**
	 * Don't copy newly branched files to the client.
	 * Corresponds to the -v flag.
	 */
	protected boolean dontCopyToClient = false;

	/**
	 * If positive, integrate only the first maxFiles files.
	 * Corresponds to -m flag.
	 */
	protected int maxFiles = 0;

	/**
	 * If non-null, use a user-defined branch view. The source is the left
	 * side of the branch view and the target is the right side. With -r,
	 * the direction is reversed. Corresponds to -b flag.
	 */
	protected String branch = null;

	/**
	 * If not null, makes 'p4 integrate' use a stream's branch view. The
	 * source is the stream itself, and the target is the stream's parent.
	 * With -r, the direction is reversed.  -P can be used to specify a
	 * parent stream other than the stream's actual parent. Note that to
	 * submit integrated stream files, the current client must be dedicated
	 * to the target stream. Corresponds to -S flag.
	 */
	protected String stream = null;

	/**
	 * If non-null, specify a parent stream other than the stream's actual
	 * parent. Corresponds to -P flag.
	 */
	protected String parentStream = null;

	/**
	 * -Ob	Show the base revision for the merge (if any).
	 */
	protected boolean showBaseRevision = false;

	/**
	 * -Or	Show the resolve(s) that are being scheduled.
	 */
	protected boolean showScheduledResolve = false;

	/**
	 * -2 enable old version 2 integration engine
	 */
	protected boolean integ2 = false;

	/**
	 * Default constructor.
	 */
	public IntegrateFilesOptions() {
		super();
	}

	/**
	 * Strings-based constructor; see 'p4 help [command]' for possible options.
	 * <p>
	 * <p>
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 * <p>
	 * <b>NOTE: setting options this way always bypasses the internal options
	 * values, and getter methods against the individual values corresponding to
	 * the strings passed in to this constructor will not normally reflect the
	 * string's setting. Do not use this constructor unless you know what you're
	 * doing and / or you do not also use the field getters and setters.</b>
	 *
	 * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
	 */
	public IntegrateFilesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public IntegrateFilesOptions(int changelistId,
	                             boolean bidirectionalInteg,
	                             boolean integrateAroundDeletedRevs,
	                             boolean rebranchSourceAfterDelete, boolean deleteTargetAfterDelete,
	                             boolean integrateAllAfterReAdd, boolean forceIntegration,
	                             boolean useHaveRev, boolean doBaselessMerge,
	                             boolean displayBaseDetails, boolean showActionsOnly,
	                             boolean reverseMapping, boolean propagateType,
	                             boolean dontCopyToClient, int maxFiles) {
		super();
		this.changelistId = changelistId;
		this.bidirectionalInteg = bidirectionalInteg;
		this.integrateAroundDeletedRevs = integrateAroundDeletedRevs;
		this.rebranchSourceAfterDelete = rebranchSourceAfterDelete;
		this.deleteTargetAfterDelete = deleteTargetAfterDelete;
		this.integrateAllAfterReAdd = integrateAllAfterReAdd;
		this.forceIntegration = forceIntegration;
		this.useHaveRev = useHaveRev;
		this.doBaselessMerge = doBaselessMerge;
		this.displayBaseDetails = displayBaseDetails;
		this.showActionsOnly = showActionsOnly;
		this.reverseMapping = reverseMapping;
		this.propagateType = propagateType;
		this.dontCopyToClient = dontCopyToClient;
		this.maxFiles = maxFiles;
	}

	/**
	 * Explicit-value constructor.
	 */
	public IntegrateFilesOptions(int changelistId,
	                             boolean bidirectionalInteg,
	                             boolean integrateAroundDeletedRevs,
	                             boolean rebranchSourceAfterDelete, boolean deleteTargetAfterDelete,
	                             boolean integrateAllAfterReAdd,
	                             boolean branchResolves, boolean deleteResolves,
	                             boolean skipIntegratedRevs, boolean forceIntegration,
	                             boolean useHaveRev, boolean doBaselessMerge,
	                             boolean displayBaseDetails, boolean showActionsOnly,
	                             boolean reverseMapping, boolean propagateType,
	                             boolean dontCopyToClient, int maxFiles) {
		super();
		this.changelistId = changelistId;
		this.bidirectionalInteg = bidirectionalInteg;
		this.integrateAroundDeletedRevs = integrateAroundDeletedRevs;
		this.rebranchSourceAfterDelete = rebranchSourceAfterDelete;
		this.deleteTargetAfterDelete = deleteTargetAfterDelete;
		this.integrateAllAfterReAdd = integrateAllAfterReAdd;
		this.branchResolves = branchResolves;
		this.deleteResolves = deleteResolves;
		this.skipIntegratedRevs = skipIntegratedRevs;
		this.forceIntegration = forceIntegration;
		this.useHaveRev = useHaveRev;
		this.doBaselessMerge = doBaselessMerge;
		this.displayBaseDetails = displayBaseDetails;
		this.showActionsOnly = showActionsOnly;
		this.reverseMapping = reverseMapping;
		this.propagateType = propagateType;
		this.dontCopyToClient = dontCopyToClient;
		this.maxFiles = maxFiles;
	}

	/**
	 * Explicit-value constructor for use with a branch.
	 */
	public IntegrateFilesOptions(int changelistId,
	                             boolean integrateAroundDeletedRevs,
	                             boolean rebranchSourceAfterDelete, boolean deleteTargetAfterDelete,
	                             boolean integrateAllAfterReAdd,
	                             boolean branchResolves, boolean deleteResolves,
	                             boolean skipIntegratedRevs, boolean forceIntegration,
	                             boolean useHaveRev, boolean doBaselessMerge,
	                             boolean displayBaseDetails, boolean showActionsOnly,
	                             boolean propagateType, boolean dontCopyToClient,
	                             int maxFiles, String branch,
	                             boolean reverseMapping, boolean bidirectionalInteg) {
		super();
		this.changelistId = changelistId;
		this.integrateAroundDeletedRevs = integrateAroundDeletedRevs;
		this.rebranchSourceAfterDelete = rebranchSourceAfterDelete;
		this.deleteTargetAfterDelete = deleteTargetAfterDelete;
		this.integrateAllAfterReAdd = integrateAllAfterReAdd;
		this.branchResolves = branchResolves;
		this.deleteResolves = deleteResolves;
		this.skipIntegratedRevs = skipIntegratedRevs;
		this.forceIntegration = forceIntegration;
		this.useHaveRev = useHaveRev;
		this.doBaselessMerge = doBaselessMerge;
		this.displayBaseDetails = displayBaseDetails;
		this.showActionsOnly = showActionsOnly;
		this.propagateType = propagateType;
		this.dontCopyToClient = dontCopyToClient;
		this.maxFiles = maxFiles;
		this.branch = branch;
		this.reverseMapping = reverseMapping;
		this.bidirectionalInteg = bidirectionalInteg;
	}

	/**
	 * Explicit-value constructor for use with a stream.
	 */
	public IntegrateFilesOptions(int changelistId,
	                             boolean integrateAroundDeletedRevs,
	                             boolean rebranchSourceAfterDelete, boolean deleteTargetAfterDelete,
	                             boolean integrateAllAfterReAdd,
	                             boolean branchResolves, boolean deleteResolves,
	                             boolean skipIntegratedRevs, boolean forceIntegration,
	                             boolean useHaveRev, boolean doBaselessMerge,
	                             boolean displayBaseDetails, boolean showActionsOnly,
	                             boolean propagateType, boolean dontCopyToClient,
	                             int maxFiles, String stream,
	                             String parentStream, boolean reverseMapping) {
		super();
		this.changelistId = changelistId;
		this.integrateAroundDeletedRevs = integrateAroundDeletedRevs;
		this.rebranchSourceAfterDelete = rebranchSourceAfterDelete;
		this.deleteTargetAfterDelete = deleteTargetAfterDelete;
		this.integrateAllAfterReAdd = integrateAllAfterReAdd;
		this.branchResolves = branchResolves;
		this.deleteResolves = deleteResolves;
		this.skipIntegratedRevs = skipIntegratedRevs;
		this.forceIntegration = forceIntegration;
		this.useHaveRev = useHaveRev;
		this.doBaselessMerge = doBaselessMerge;
		this.displayBaseDetails = displayBaseDetails;
		this.showActionsOnly = showActionsOnly;
		this.propagateType = propagateType;
		this.dontCopyToClient = dontCopyToClient;
		this.maxFiles = maxFiles;
		this.stream = stream;
		this.parentStream = parentStream;
		this.reverseMapping = reverseMapping;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
				this.getChangelistId(),
				this.isIntegrateAroundDeletedRevs(),
				this.isIntegrateAllAfterReAdd(),
				this.isDeleteTargetAfterDelete(),
				this.isRebranchSourceAfterDelete(),
				this.isBranchResolves(),
				this.isDeleteResolves(),
				this.isSkipIntegratedRevs(),
				this.isForceIntegration(),
				this.isUseHaveRev(),
				this.isDoBaselessMerge(),
				this.isDisplayBaseDetails(),
				this.isShowActionsOnly(),
				this.isPropagateType(),
				this.isQuiet(),
				this.isDontCopyToClient(),
				this.getMaxFiles(),
				this.getBranch(),
				this.getStream(),
				this.getParentStream(),
				this.isReverseMapping(),
				this.isBidirectionalInteg(),
				this.isShowBaseRevision(),
				this.isShowScheduledResolve(),
				this.isInteg2());

		combineGroupOptions("-R");
		combineGroupOptions("-O");

		return this.optionList;
	}

	private void combineGroupOptions(String groupFlag) {
		// Combine the group flags (e.g. -R), if more than one is set
		StringBuilder sb = new StringBuilder();
		if (this.optionList != null) {
			// Use an iterator for safe removal of elements in a collection
			for (Iterator<String> it = this.optionList.iterator(); it.hasNext(); ) {
				String s = it.next();
				if (s.startsWith(groupFlag)) {
					if (s.length() == 3) {
						char c = s.charAt(2);
						sb.append(c);
					}
					it.remove();
				}
			}
		}
		if (sb != null && sb.length() > 0) {
			optionList.add(groupFlag + sb.toString());
		}
	}

	public int getChangelistId() {
		return changelistId;
	}

	public IntegrateFilesOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}

	public boolean isIntegrateAroundDeletedRevs() {
		return integrateAroundDeletedRevs;
	}

	public IntegrateFilesOptions setIntegrateAroundDeletedRevs(boolean integrateAroundDeletedRevs) {
		this.integrateAroundDeletedRevs = integrateAroundDeletedRevs;
		return this;
	}

	public boolean isRebranchSourceAfterDelete() {
		return rebranchSourceAfterDelete;
	}

	public IntegrateFilesOptions setRebranchSourceAfterDelete(boolean rebranchSourceAfterDelete) {
		this.rebranchSourceAfterDelete = rebranchSourceAfterDelete;
		return this;
	}

	public boolean isDeleteTargetAfterDelete() {
		return deleteTargetAfterDelete;
	}

	public IntegrateFilesOptions setDeleteTargetAfterDelete(boolean deleteTargetAfterDelete) {
		this.deleteTargetAfterDelete = deleteTargetAfterDelete;
		return this;
	}

	public boolean isIntegrateAllAfterReAdd() {
		return integrateAllAfterReAdd;
	}

	public IntegrateFilesOptions setIntegrateAllAfterReAdd(boolean integrateAllAfterReAdd) {
		this.integrateAllAfterReAdd = integrateAllAfterReAdd;
		return this;
	}

	public boolean isBranchResolves() {
		return branchResolves;
	}

	public IntegrateFilesOptions setBranchResolves(boolean branchResolves) {
		this.branchResolves = branchResolves;
		return this;
	}

	public boolean isDeleteResolves() {
		return deleteResolves;
	}

	public IntegrateFilesOptions setDeleteResolves(boolean deleteResolves) {
		this.deleteResolves = deleteResolves;
		return this;
	}

	public boolean isSkipIntegratedRevs() {
		return skipIntegratedRevs;
	}

	public IntegrateFilesOptions setSkipIntegratedRevs(boolean skipIntegratedRevs) {
		this.skipIntegratedRevs = skipIntegratedRevs;
		return this;
	}

	public boolean isForceIntegration() {
		return forceIntegration;
	}

	public IntegrateFilesOptions setForceIntegration(boolean forceIntegration) {
		this.forceIntegration = forceIntegration;
		return this;
	}

	public boolean isUseHaveRev() {
		return useHaveRev;
	}

	public IntegrateFilesOptions setUseHaveRev(boolean useHaveRev) {
		this.useHaveRev = useHaveRev;
		return this;
	}

	public boolean isDoBaselessMerge() {
		return doBaselessMerge;
	}

	public IntegrateFilesOptions setDoBaselessMerge(boolean doBaselessMerge) {
		this.doBaselessMerge = doBaselessMerge;
		return this;
	}

	public boolean isDisplayBaseDetails() {
		return displayBaseDetails;
	}

	public IntegrateFilesOptions setDisplayBaseDetails(boolean displayBaseDetails) {
		this.displayBaseDetails = displayBaseDetails;
		return this;
	}

	public boolean isShowActionsOnly() {
		return showActionsOnly;
	}

	public IntegrateFilesOptions setShowActionsOnly(boolean showActionsOnly) {
		this.showActionsOnly = showActionsOnly;
		return this;
	}

	public boolean isReverseMapping() {
		return reverseMapping;
	}

	public IntegrateFilesOptions setReverseMapping(boolean reverseMapping) {
		this.reverseMapping = reverseMapping;
		return this;
	}

	public boolean isPropagateType() {
		return propagateType;
	}

	public IntegrateFilesOptions setPropagateType(boolean propagateType) {
		this.propagateType = propagateType;
		return this;
	}

	public boolean isDontCopyToClient() {
		return dontCopyToClient;
	}

	public boolean isQuiet() {
		return quiet;
	}

	public IntegrateFilesOptions setQuiet(boolean quiet) {
		this.quiet = quiet;
		return this;
	}

	public IntegrateFilesOptions setDontCopyToClient(boolean dontCopyToClient) {
		this.dontCopyToClient = dontCopyToClient;
		return this;
	}

	public boolean isBidirectionalInteg() {
		return bidirectionalInteg;
	}

	public IntegrateFilesOptions setBidirectionalInteg(boolean bidirectionalInteg) {
		this.bidirectionalInteg = bidirectionalInteg;
		return this;
	}

	public int getMaxFiles() {
		return maxFiles;
	}

	public IntegrateFilesOptions setMaxFiles(int maxFiles) {
		this.maxFiles = maxFiles;
		return this;
	}

	public String getBranch() {
		return branch;
	}

	public IntegrateFilesOptions setBranch(String branch) {
		this.branch = branch;
		return this;
	}

	public String getStream() {
		return stream;
	}

	public IntegrateFilesOptions setStream(String stream) {
		this.stream = stream;
		return this;
	}

	public String getParentStream() {
		return parentStream;
	}

	public IntegrateFilesOptions setParentStream(String parentStream) {
		this.parentStream = parentStream;
		return this;
	}

	public boolean isShowBaseRevision() {
		return showBaseRevision;
	}

	public IntegrateFilesOptions setShowBaseRevision(boolean showBaseRevision) {
		this.showBaseRevision = showBaseRevision;
		return this;
	}

	public boolean isShowScheduledResolve() {
		return showScheduledResolve;
	}

	public IntegrateFilesOptions setShowScheduledResolve(boolean showScheduledResolve) {
		this.showScheduledResolve = showScheduledResolve;
		return this;
	}

	public boolean isInteg2() {
		return integ2;
	}

	public IntegrateFilesOptions setInteg2(boolean enable) {
		this.integ2 = enable;
		return this;
	}
}
