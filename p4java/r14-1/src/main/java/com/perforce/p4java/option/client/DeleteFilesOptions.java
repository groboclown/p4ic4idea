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
 * Options-based method options for IClient deleteFiles method(s).
 *
 * @see com.perforce.p4java.client.IClient#deleteFiles(java.util.List, com.perforce.p4java.option.client.DeleteFilesOptions)
 */
public class DeleteFilesOptions extends Options {

        /**
         * Options: -c[changelist], -n, -v, -k
         */
        public static final String OPTIONS_SPECS = "i:c:clz b:n b:v b:k";

        /**
         * If positive, the deleted files are put into the pending
         * changelist identified by changelistId (this changelist must have been
         * previously created for this to succeed). If zero or negative, the
         * file is opened in the 'default' (unnumbered) changelist.
         * Corresponds to the -c flag.
         */
        protected int changelistId = IChangelist.DEFAULT;

        /**
         * If true, don't actually do the deletes, just return the files that
         * would have been opened for deletion. Corresponds to the -n flag.
         */
        protected boolean noUpdate = false;

        /**
         * If true, delete files that are not synced into the client workspace.
         * Corresponds to the -v flag.
         */
        protected boolean deleteNonSyncedFiles = false;

        /**
         * If true, bypass deleting files on the client workspace. The -k flag
         * performs the delete on the server without modifying client files. Use
         * with caution, as an incorrect delete can cause discrepancies between
         * the state of the client and the corresponding server metadata.
         * Corresponds to the -k flag.
         */
        protected boolean bypassClientDelete = false;

        /**
         * Default constructor.
         */
        public DeleteFilesOptions() {
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
        public DeleteFilesOptions(String... options) {
                super(options);
        }


        /**
         * Explicit-value constructor.
         */
        public DeleteFilesOptions(int changelistId, boolean noUpdate,
                        boolean deleteNonSyncedFiles) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.deleteNonSyncedFiles = deleteNonSyncedFiles;
        }

        /**
         * Explicit-value constructor.
         */
        public DeleteFilesOptions(int changelistId, boolean noUpdate,
                        boolean deleteNonSyncedFiles, boolean bypassClientDelete) {
                super();
                this.changelistId = changelistId;
                this.noUpdate = noUpdate;
                this.deleteNonSyncedFiles = deleteNonSyncedFiles;
                this.bypassClientDelete = bypassClientDelete;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                                                                this.changelistId,
                                                                this.noUpdate,
                                                                this.deleteNonSyncedFiles,
                                                                this.bypassClientDelete);

                return this.optionList;
        }

        public int getChangelistId() {
                return changelistId;
        }

        public DeleteFilesOptions setChangelistId(int changelistId) {
                this.changelistId = changelistId;
                return this;
        }

        public boolean isNoUpdate() {
                return noUpdate;
        }

        public DeleteFilesOptions setNoUpdate(boolean noUpdate) {
                this.noUpdate = noUpdate;
                return this;
        }

        public boolean isDeleteNonSyncedFiles() {
                return deleteNonSyncedFiles;
        }

        public DeleteFilesOptions setDeleteNonSyncedFiles(boolean deleteNonSyncedFiles) {
                this.deleteNonSyncedFiles = deleteNonSyncedFiles;
                return this;
        }

        public boolean isBypassClientDelete() {
                return bypassClientDelete;
        }

        public DeleteFilesOptions setBypassClientDelete(boolean bypassClientDelete) {
                this.bypassClientDelete = bypassClientDelete;
                return this;
        }
}
