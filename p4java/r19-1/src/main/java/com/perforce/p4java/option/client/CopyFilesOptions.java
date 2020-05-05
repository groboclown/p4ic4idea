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
 * Options class for the IClient copyFiles method.
 *
 * @since 2011.1
 */
public class CopyFilesOptions extends Options {

        /**
         * Options: -c[changelist], -f, -n, -q, -v, -b[branch], -S[stream], -P[parentStream], -F, -r, -s
         */
        public static final String OPTIONS_SPECS = "i:c:cl b:f b:n b:q b:v i:m:gtz s:b s:S s:P b:F b:r b:s";

        /**
         * If positive, use the changelistId given instead of
         * the default changelist. Corresponds to the -c option.
         */
        protected int changelistId = IChangelist.UNKNOWN;

        /**
         * If true, force the creation of extra revisions in order to
         * explicitly record that files have been copied.
         * Corresponds to -f flag.
         */
        protected boolean force = false;

        /** If true, don't actually do the copy. Corresponds to -n flag. */
        protected boolean noUpdate = false;

        /**
         * If true, suppresses normal output messages. Messages regarding
	     * errors or exceptional conditions are not suppressed.
	     * Corresponds to -q flag.
	     */
        protected boolean quiet = false;
        
        /**
         * If true, don't do syncing or modifying of client files.
         * Corresponds to -v flag.
         */
        protected boolean noClientSyncOrMod = false;

        /**
         * If true, this is a 'bidirectional' copy. Corresponds
         * to -s flag.
         */
        protected boolean bidirectional = false;

        /**
         * Reverse the mappings in the branch view, with the
         * target files and source files exchanging place.
         * Corresponds to the -r flag.
         */
        protected boolean reverseMapping = false;

        /**
         * If positive, copy only the first maxFiles files.
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
         * If non-null, use this stream's branch view. The source is the stream
         * itself, and the target is the stream's parent. With -r, the direction
         * is reversed. -P can be used to specify a parent stream other than the
         * stream's actual parent. Note that to submit copied stream files, the
         * current client must be dedicated to the target stream.
         * Corresponds to -S flag.
         */
        protected String stream = null;
        
        /**
         * If non-null, specify a parent stream other than the stream's actual
         * parent. Corresponds to -P flag.
         */
        protected String parentStream = null;
        
        /**
         * If true, used with -S to force copying even though the stream does
         * not expect a copy to occur in the direction indicated. Normally
         * 'p4 copy' enforces the expected flow of change dictated by the
         * stream's spec. The 'p4 istat' command summarizes a stream's expected
         * flow of change. Corresponds to -F flag.
         */
        protected boolean forceStreamCopy = false;
        
        /**
         * Default constructor.
         */
        public CopyFilesOptions() {
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
        public CopyFilesOptions(String... options) {
                super(options);
        }

        /**
         * Explicit-value constructor.
         */
        public CopyFilesOptions(int changelistId, boolean noUpdate,
                        boolean noClientSyncOrMod) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.noClientSyncOrMod = noClientSyncOrMod;
        }

        /**
         * Explicit-value constructor.
         */
        public CopyFilesOptions(int changelistId, boolean noUpdate,
                        boolean noClientSyncOrMod, boolean bidirectional) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.noClientSyncOrMod = noClientSyncOrMod;
                this.bidirectional = bidirectional;
        }

        /**
         * Explicit-value constructor.
         */
        public CopyFilesOptions(int changelistId, boolean noUpdate,
                        boolean noClientSyncOrMod, boolean bidirectional, boolean reverseMapping) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.noClientSyncOrMod = noClientSyncOrMod;
                this.bidirectional = bidirectional;
                this.reverseMapping = reverseMapping;
        }

        /**
         * Explicit-value constructor.
         */
        public CopyFilesOptions(int changelistId, boolean noUpdate,
                        boolean noClientSyncOrMod, boolean bidirectional, boolean reverseMapping,
                        int  maxFiles) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.noClientSyncOrMod = noClientSyncOrMod;
                this.bidirectional = bidirectional;
                this.reverseMapping = reverseMapping;
                this.maxFiles = maxFiles;
        }

        /**
         * Explicit-value constructor for use with a branch.
         */
        public CopyFilesOptions(int changelistId, boolean noUpdate,
                        boolean noClientSyncOrMod, int  maxFiles, String branch,
                        boolean reverseMapping, boolean bidirectional) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.noClientSyncOrMod = noClientSyncOrMod;
                this.maxFiles = maxFiles;
                this.branch = branch;
                this.reverseMapping = reverseMapping;
                this.bidirectional = bidirectional;
        }

        /**
         * Explicit-value constructor for use with a stream.
         */
        public CopyFilesOptions(int changelistId, boolean noUpdate,
                        boolean noClientSyncOrMod, int  maxFiles,
                        String stream, String parentStream,
                        boolean forceStreamCopy, boolean reverseMapping) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.noClientSyncOrMod = noClientSyncOrMod;
                this.maxFiles = maxFiles;
                this.stream = stream;
                this.parentStream = parentStream;
                this.forceStreamCopy = forceStreamCopy;
                this.reverseMapping = reverseMapping;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                                this.getChangelistId(),
                                this.isForce(),
                                this.isNoUpdate(),
                                this.isQuiet(),
                                this.isNoClientSyncOrMod(),
                                this.getMaxFiles(),
                                this.getBranch(),
                                this.getStream(),
                                this.getParentStream(),
                                this.isForceStreamCopy(),
                                this.isReverseMapping(),
                                this.isBidirectional());
                return this.optionList;
        }

        public int getChangelistId() {
                return changelistId;
        }

        public CopyFilesOptions setChangelistId(int changelistId) {
                this.changelistId = changelistId;
                return this;
        }

        public boolean isForce() {
            return force;
        }

        public CopyFilesOptions setForce(boolean force) {
            this.force = force;
            return this;
        }
        public boolean isNoUpdate() {
                return noUpdate;
        }

        public CopyFilesOptions setNoUpdate(boolean noUpdate) {
                this.noUpdate = noUpdate;
                return this;
        }

        public boolean isQuiet() {
        		return quiet;
        }

        public CopyFilesOptions setQuiet(boolean quiet) {
        		this.quiet = quiet;
        		return this;
    	}
        
        public boolean isNoClientSyncOrMod() {
                return noClientSyncOrMod;
        }

        public CopyFilesOptions setNoClientSyncOrMod(boolean noClientSyncOrMod) {
                this.noClientSyncOrMod = noClientSyncOrMod;
                return this;
        }

        public boolean isBidirectional() {
                return bidirectional;
        }

        public CopyFilesOptions setBidirectional(boolean bidirectional) {
                this.bidirectional = bidirectional;
                return this;
        }

        public boolean isReverseMapping() {
                return reverseMapping;
        }

        public CopyFilesOptions setReverseMapping(boolean reverseMapping) {
                this.reverseMapping = reverseMapping;
                return this;
        }

        public int getMaxFiles() {
                return maxFiles;
        }

        public CopyFilesOptions setMaxFiles(int maxFiles) {
                this.maxFiles = maxFiles;
                return this;
        }

        public String getBranch() {
                return branch;
        }

        public CopyFilesOptions setBranch(String branch) {
                this.branch = branch;
                return this;
        }

        public String getStream() {
                return stream;
        }

        public CopyFilesOptions setStream(String stream) {
                this.stream = stream;
                return this;
        }

        public String getParentStream() {
        	    return parentStream;
        }

        public CopyFilesOptions setParentStream(String parentStream) {
        	    this.parentStream = parentStream;
        	    return this;
        }

        public boolean isForceStreamCopy() {
    	        return forceStreamCopy;
        }

        public CopyFilesOptions setForceStreamCopy(boolean forceStreamCopy) {
    	        this.forceStreamCopy = forceStreamCopy;
    	        return this;
        }
}
