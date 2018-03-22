/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getFileSizes method.<p>
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getFileSizes(java.util.List, com.perforce.p4java.option.server.GetFileSizesOptions)
 */
public class GetFileSizesOptions extends Options {
	
	/**
	 * <pre>
	 * Options:
	 * 		[-a -S] [-s | -z] [-b blocksize] [-m max]
	 * 		-A [-s] [-b blocksize] [-m max]
	 * 		-U
	 * </pre>
	 */
	public static final String OPTIONS_SPECS = "b:a b:S b:A b:U b:s b:z l:b:gtz i:m:gtz";
	
	/**
	 * If true, lists all revisions within the specific range, rather than just
	 * the highest revision in the range. Corresponds to the -a flag.
	 */
	protected boolean allRevisions = false;
	
	/**
	 * If true, list size information for shelved files only. With this option,
	 * revision specifications are not permitted. Corresponds to the -S flag.
	 */
	protected boolean shelvedFiles = false;
	
	/**
	 * If true, list files in archive depots (see 'p4 help archive').
	 * Corresponds to the -A flag.
	 */
	protected boolean archivedFiles = false;

	/**
	 * If true, list size information for unload files in the unload depot (see
	 * 'p4 help unload'). Corresponds to the -U flag.
	 */
	protected boolean unloadedFiles = false;

	/**
	 * If true, calculates the sum of the file sizes for the specified files.
	 * Corresponds to the -s flag.
	 */
	protected boolean sumFileSizes = false;

	/**
	 * If true, omits lazy copies when calculating the file sizes for the
	 * specified files. Corresponds to the -z flag.
	 */
	protected boolean omitLazyCopies = false;

	/**
	 * If greater than zero, specifies the block size in bytes. When this option
	 * is specified, each accumulated filesize is rounded up to the nearest
	 * blocksize. Corresponds to the -b flag.
	 */
	protected long blockSize = 0;
	
	/**
	 * If positive, limits sizes to the first 'max' number of files. Corresponds
	 * to the -m flag.
	 */
	protected int maxFiles = 0;

	/**
	 * Default constructor.
	 */
	public GetFileSizesOptions() {
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
	public GetFileSizesOptions(String... options) {
		super(options);
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isAllRevisions(),
								this.isShelvedFiles(),
								this.isArchivedFiles(),
								this.isUnloadedFiles(),
								this.isSumFileSizes(),
								this.isOmitLazyCopies(),
								this.getBlockSize(),
								this.getMaxFiles());
		return this.optionList;
	}

	public boolean isAllRevisions() {
		return this.allRevisions;
	}

	public GetFileSizesOptions setAllRevisions(boolean allRevisions) {
		this.allRevisions = allRevisions;
		return this;
	}

	public boolean isShelvedFiles() {
		return this.shelvedFiles;
	}

	public GetFileSizesOptions setShelvedFiles(boolean shelvedFiles) {
		this.shelvedFiles = shelvedFiles;
		return this;
	}

	public boolean isArchivedFiles() {
		return this.archivedFiles;
	}

	public GetFileSizesOptions setArchivedFiles(boolean archivedFiles) {
		this.archivedFiles = archivedFiles;
		return this;
	}

	public boolean isUnloadedFiles() {
		return this.unloadedFiles;
	}

	public GetFileSizesOptions setUnloadedFiles(boolean unloadedFiles) {
		this.unloadedFiles = unloadedFiles;
		return this;
	}

	public boolean isSumFileSizes() {
		return this.sumFileSizes;
	}

	public GetFileSizesOptions setSumFileSizes(boolean sumFileSizes) {
		this.sumFileSizes = sumFileSizes;
		return this;
	}

	public boolean isOmitLazyCopies() {
		return this.omitLazyCopies;
	}

	public GetFileSizesOptions setOmitLazyCopies(boolean omitLazyCopies) {
		this.omitLazyCopies = omitLazyCopies;
		return this;
	}

	public long getBlockSize() {
		return this.blockSize;
	}

	public GetFileSizesOptions setBlockSize(long blockSize) {
		this.blockSize = blockSize;
		return this;
	}

	public int getMaxFiles() {
		return this.maxFiles;
	}

	public GetFileSizesOptions setMaxFiles(int maxFiles) {
		this.maxFiles = maxFiles;
		return this;
	}
}
