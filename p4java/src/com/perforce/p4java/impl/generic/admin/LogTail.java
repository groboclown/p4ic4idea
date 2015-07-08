/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import java.util.List;

import com.perforce.p4java.admin.ILogTail;

/**
 * Default implementation of the ILogTail interface.
 */
public class LogTail implements ILogTail {

	/** The log file path. */
	private String logFilePath = null;

	/** The offset in bytes. */
	private long offset = -1;

	/** The log file data. */
	private List<String> data = null;

	public LogTail(String logFilePath, long offset, List<String> data) {
		if (logFilePath == null) {
			throw new IllegalArgumentException("Null logFilePath passed to the LogTail constructor.");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("Negative offset passed to the LogTail constructor.");
		}
		if (data == null) {
			throw new IllegalArgumentException("Null data passed to the LogTail constructor.");
		}
		if (data.size() == 0) {
			throw new IllegalArgumentException("No data passed to the LogTail constructor.");
		}

		this.logFilePath = logFilePath;
		this.offset = offset;
		this.data = data;
	}

	/**
	 * @see com.perforce.p4java.admin.ILogTail#getLogFilePath()
	 */
	public String getLogFilePath() {
		return this.logFilePath;
	}

	/**
	 * @see com.perforce.p4java.admin.ILogTail#getOffset()
	 */
	public long getOffset() {
		return this.offset;
	}

	/**
	 * @see com.perforce.p4java.admin.ILogTail#getData()
	 */
	public List<String> getData() {
		return this.data;
	}
}
