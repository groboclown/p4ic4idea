/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.verifyFiles method.<p>
 * 
 * @see com.perforce.p4java.server.IOptionsServer#verifyFiles(java.util.List, com.perforce.p4java.option.server.VerifyFilesOptions)
 */
public class VerifyFilesOptions extends Options {
	
	/**
	 * <pre>
	 * Options:
	 * 		[-t | -u | -v | -z] [-m max -q -s -X -b N] file[revRange] ...
     * 		-U unloadfiles...
	 * </pre>
	 */
	public static final String OPTIONS_SPECS = "b:t b:u b:v b:z i:m:gtz b:q b:s b:X l:b:gez b:U";
	
	/**
	 * If true, causes 'p4 verify' to schedule transfer of the content of any
	 * damaged revision. This option is for use only with a replica server and
	 * cannot be used with the '-v' or '-u' options. Corresponds to the -t flag.
	 */
	protected boolean transferContent = false;
	
	/**
	 * If true, computes and saves the digest only for revisions that have no
	 * saved digest. Corresponds to the -u flag.
	 */
	protected boolean computeMissingDigest = false;
	
	/**
	 * If true, computes and saves the digest for each revision, regardless of
	 * whether the revision already has a saved digest. This option can be used
	 * to update the saved digest if the archive was deliberately changed. The
	 * '-u' and '-v' options are mutually exclusive. Corresponds to the -v flag.
	 */
	protected boolean computeDigest = false;

	/**
	 * If true, optimizes digest computations by skipping revisions that have
	 * already been computed in the current pass. This option is useful when the
	 * specified files contain lazy copies. The resulting output might report a
	 * lazy copy revision if it is the first revision in the sort order to
	 * access a common archive file. This option cannot be used with the '-v' or
	 * '-u' options. Corresponds to the -z flag.
	 */
	protected boolean skipComputedDigest = false;

	/**
	 * If positive, specifies the maximum number of revisions to process. This
	 * option can be used with the -u flag to compute and save digests for a
	 * limited number of revisions in each 'p4 verify' invocation. Corresponds
	 * to the -z flag.
	 */
	protected int maxRevisions = 0;

	/**
	 * If true, minimizes command output, displaying only errors from mismatched
	 * digests or unreproducible revisions. Corresponds to the -q flag.
	 */
	protected boolean quiet = false;

	/**
	 * If true, specifies that the file size should also be verified. The -v
	 * flag implies the -s flag. Corresponds to the -s flag.
	 */
	protected boolean verifySize = false;

	/**
	 * If true, specifies that files with the +X filetype modifier should be
	 * skipped. Corresponds to the -s flag.
	 */
	protected boolean skipPlusXModifier = false;
	
	/**
	 * If greater than zero, specifies the batch size. By default, 'p4 verify'
	 * processes files in batches of 10000 files at a time. Specify -b 0 to
	 * disable batching and process all files in a single batch. If the -z flag
	 * is specified, the -b flag is ignored and all files are processed in a
	 * single batch.. Corresponds to the -b N flag.
	 */
	protected long batchSize = 0;
	
	/**
	 * If true, verifies files in the unload depot (see 'p4 help unload').
	 * Corresponds to the -U flag.
	 */
	protected boolean verifyUnload = false;

	/**
	 * Default constructor.
	 */
	public VerifyFilesOptions() {
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
	public VerifyFilesOptions(String... options) {
		super(options);
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isTransferContent(),
								this.isComputeMissingDigest(),
								this.isComputeDigest(),
								this.isSkipComputedDigest(),
								this.getMaxRevisions(),
								this.isQuiet(),
								this.isVerifySize(),
								this.isSkipPlusXModifier(),
								this.getBatchSize(),
								this.isVerifyUnload());
		return this.optionList;
	}

	public boolean isTransferContent() {
		return transferContent;
	}

	public VerifyFilesOptions setTransferContent(boolean transferContent) {
		this.transferContent = transferContent;
		return this;
	}

	public boolean isComputeMissingDigest() {
		return computeMissingDigest;
	}

	public VerifyFilesOptions setComputeMissingDigest(boolean computeMissingDigest) {
		this.computeMissingDigest = computeMissingDigest;
		return this;
	}

	public boolean isComputeDigest() {
		return computeDigest;
	}

	public VerifyFilesOptions setComputeDigest(boolean computeDigest) {
		this.computeDigest = computeDigest;
		return this;
	}

	public boolean isSkipComputedDigest() {
		return skipComputedDigest;
	}

	public VerifyFilesOptions setSkipComputedDigest(boolean skipComputedDigest) {
		this.skipComputedDigest = skipComputedDigest;
		return this;
	}

	public int getMaxRevisions() {
		return maxRevisions;
	}

	public VerifyFilesOptions setMaxRevisions(int maxRevisions) {
		this.maxRevisions = maxRevisions;
		return this;
	}

	public boolean isQuiet() {
		return quiet;
	}

	public VerifyFilesOptions setQuiet(boolean quiet) {
		this.quiet = quiet;
		return this;
	}

	public boolean isVerifySize() {
		return verifySize;
	}

	public VerifyFilesOptions setVerifySize(boolean verifySize) {
		this.verifySize = verifySize;
		return this;
	}

	public boolean isSkipPlusXModifier() {
		return skipPlusXModifier;
	}

	public VerifyFilesOptions setSkipPlusXModifier(boolean skipPlusXModifier) {
		this.skipPlusXModifier = skipPlusXModifier;
		return this;
	}

	public long getBatchSize() {
		return batchSize;
	}

	public VerifyFilesOptions setBatchSize(long batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	public boolean isVerifyUnload() {
		return verifyUnload;
	}

	public VerifyFilesOptions setVerifyUnload(boolean verifyUnload) {
		this.verifyUnload = verifyUnload;
		return this;
	}
}
