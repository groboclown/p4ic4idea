/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

import java.util.List;

/**
 * Defines the last block(s) of the errorLog.
 */
public interface ILogTail {

	/**
	 * Gets the log file path. <p>
	 * 
	 * By default, it's set to null, which is not valid.
	 * 
	 * @return the log file path
	 */
	String getLogFilePath();

	/**
	 * Gets the offset required to get the next block when it becomes available. <p>
	 * 
	 * By default, it is set to -1, which is not valid (same as not setting the offset).
	 * 
	 * @return the offset bytes
	 */
	long getOffset();

	/**
	 * Gets the last block(s) of the errorLog. <p>
	 * 
	 * By default, it is set to null, which is not valid.
	 * 
	 * @return the log data
	 */
	List<String> getData();
}
