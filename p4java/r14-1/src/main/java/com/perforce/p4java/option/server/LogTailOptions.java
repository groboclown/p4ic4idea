/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for Perforce IOptionsServer.getLogtail methods.
 */
public class LogTailOptions extends Options {
	
	/**
	 * Options: -b[blockSize], -s[startingOffset], -m[maxBlocks]
	 */
	public static final String OPTIONS_SPECS = "l:b:gtz l:s:gez i:m:gtz";
	
	/**
	 * If greater than zero, specifies the block size in bytes (default 8192).
	 * Corresponds to the -b flag.
	 */
	protected long blockSize = 0;
	
	/**
	 * If positive, enables you to specify the offset from the beginning of the
	 * file (in bytes). Corresponds to the -s flag.
	 */
	protected long startingOffset = -1;
	
	/**
	 * If greater than zero, specifies a maximum number of blocks to output.
	 * This flag is ignored unless the -s flag is specified.
	 * Corresponds to the -m flag.
	 */
	protected int maxBlocks = 0;

	/**
	 * Default constructor.
	 */
	public LogTailOptions() {
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
	public LogTailOptions(String... options) {
		super(options);
	}
	
	/**
	 * Explicit value constructor.
	 */
	public LogTailOptions(long blockSize, long startingOffset, int maxBlocks) {
		super();
		this.blockSize = blockSize;
		this.startingOffset = startingOffset;
		this.maxBlocks = maxBlocks;
	}
	
	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.getBlockSize(),
								this.getStartingOffset(),
								this.getMaxBlocks());

		return this.optionList;
	}

	/**
	 * Gets the block size (in bytes).
	 * 
	 * @return the block size (in bytes)
	 */
	public long getBlockSize() {
		return blockSize;
	}

	/**
	 * Sets the block size (in bytes).
	 * 
	 * @param blockSize
	 *            the block size (in bytes)
	 * @return the log tail options
	 */
	public LogTailOptions setBlockSize(long blockSize) {
		this.blockSize = blockSize;
		return this;
	}

	/**
	 * Gets the offset from the	beginning of the file (in bytes).
	 * 
	 * @return the starting offset (in bytes)
	 */
	public long getStartingOffset() {
		return startingOffset;
	}

	/**
	 * Sets the offset from the	beginning of the file (in bytes).
	 * 
	 * @param startingOffset
	 *            the starting offset (in bytes)
	 * @return the log tail options
	 */
	public LogTailOptions setStartingOffset(long startingOffset) {
		this.startingOffset = startingOffset;
		return this;
	}

	/**
	 * Gets the maximum number of blocks to output.
	 * 
	 * @return the maximum blocks
	 */
	public int getMaxBlocks() {
		return maxBlocks;
	}

	/**
	 * Sets the maximum number of blocks to output.
	 * 
	 * @param maxBlocks
	 *            the maximum blocks
	 * @return the log tail options
	 */
	public LogTailOptions setMaxBlocks(int maxBlocks) {
		this.maxBlocks = maxBlocks;
		return this;
	}
}