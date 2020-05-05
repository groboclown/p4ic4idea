/**
 *
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options for the IClient.mergeFiles method.<p>
 *
 * Note also that the current implementation makes no attempt
 * to validate the sanity or otherwise of the various options and
 * their combination.
 *
 * @see com.perforce.p4java.client.IClient#mergeFiles(IFileSpec, List, MergeFilesOptions)
 */
public class MergeFilesOptions extends Options {

        /**
         * Options: -c[changelist], -n, -q, -m[max], -b[branch], -S[stream], -P[parentStream], -F, -r, -s
         */
        public static final String OPTIONS_SPECS = "i:c:clz b:n b:q i:m:gtz s:b s:S s:P b:F b:r b:s";

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
        protected boolean bidirectionalMerge = false;

        /**
         * Display what integrations would be necessary but don't
         * actually do them. Corresponds to the -n flag.
         */
        protected boolean showActionsOnly = false;

        /**
         * If true, suppresses normal output messages. Messages regarding
	     * errors or exceptional conditions are not suppressed.
	     * Corresponds to -q flag.
	     */
        protected boolean quiet = false;
        
        /**
         * Reverse the mappings in the branch view, with the
         * target files and source files exchanging place.
         * Corresponds to the -r flag.
         */
        protected boolean reverseMapping = false;

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
         * If true, force merging even though the stream does not expect a merge
         * to occur in the direction indicated. Normally 'p4 merge' enforces the
         * expected flow of change dictated by the stream's spec. The 'p4 istat'
         * command summarizes a stream's expected flow of change.
         */
        protected boolean forceStreamMerge = false;

        /**
         * Default constructor.
         */
        public MergeFilesOptions() {
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
        public MergeFilesOptions(String... options) {
                super(options);
        }

        /**
        * Explicit-value constructor for use with a branch.
         */
        public MergeFilesOptions(int changelistId, boolean showActionsOnly,
        						int maxFiles, String branch,
        						boolean reverseMapping, boolean bidirectionalInteg) {
                super();
                this.changelistId = changelistId;
                this.showActionsOnly = showActionsOnly;
                this.maxFiles = maxFiles;
                this.branch = branch;
                this.reverseMapping = reverseMapping;
                this.bidirectionalMerge = bidirectionalInteg;
        }

        /**
         * Explicit-value constructor for use with a stream.
         */
        public MergeFilesOptions(int changelistId, boolean showActionsOnly,
        						int maxFiles, String stream, String parentStream,
        						boolean forceStreamMerge, boolean reverseMapping) {
                super();
                this.changelistId = changelistId;
                this.showActionsOnly = showActionsOnly;
                this.maxFiles = maxFiles;
                this.stream = stream;
                this.parentStream = parentStream;
                this.forceStreamMerge = forceStreamMerge;
                this.reverseMapping = reverseMapping;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
	                                                    this.getChangelistId(),
	                                                    this.isShowActionsOnly(),
	                                                    this.isQuiet(),
	                                                    this.getMaxFiles(),
	                                                    this.getBranch(),
	                                                    this.getStream(),
	                                                    this.getParentStream(),
	                                                    this.isForceStreamMerge(),
	                                                    this.isReverseMapping(),
	                                                    this.isBidirectionalInteg());
                return this.optionList;
        }

        public int getChangelistId() {
                return changelistId;
        }

        public MergeFilesOptions setChangelistId(int changelistId) {
                this.changelistId = changelistId;
                return this;
        }

        public boolean isShowActionsOnly() {
                return showActionsOnly;
        }

        public MergeFilesOptions setShowActionsOnly(boolean showActionsOnly) {
                this.showActionsOnly = showActionsOnly;
                return this;
        }

        public boolean isQuiet() {
    			return quiet;
        }

        public MergeFilesOptions setQuiet(boolean quiet) {
    			this.quiet = quiet;
    			return this;
        }
        
        public boolean isReverseMapping() {
                return reverseMapping;
        }

        public MergeFilesOptions setReverseMapping(boolean reverseMapping) {
                this.reverseMapping = reverseMapping;
                return this;
        }

        public boolean isBidirectionalInteg() {
                return bidirectionalMerge;
        }

        public MergeFilesOptions setBidirectionalInteg(boolean bidirectionalInteg) {
                this.bidirectionalMerge = bidirectionalInteg;
                return this;
        }

        public int getMaxFiles() {
                return maxFiles;
        }

        public MergeFilesOptions setMaxFiles(int maxFiles) {
                this.maxFiles = maxFiles;
                return this;
        }

        public String getBranch() {
                return branch;
        }

        public MergeFilesOptions setBranch(String branch) {
                this.branch = branch;
                return this;
        }

        public String getStream() {
                return stream;
        }

        public MergeFilesOptions setStream(String stream) {
                this.stream = stream;
                return this;
        }

        public String getParentStream() {
    	        return parentStream;
        }

        public MergeFilesOptions setParentStream(String parentStream) {
    	        this.parentStream = parentStream;
    	        return this;
        }

        public boolean isForceStreamMerge() {
	            return forceStreamMerge;
        }

        public MergeFilesOptions setForceStreamMerge(boolean forceStreamMerge) {
	           this.forceStreamMerge = forceStreamMerge;
	           return this;
        }
}
