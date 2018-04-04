/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

/**
 * Defines disk space information on the server.
 */
public interface IDiskSpace {

	/**
	 * Gets the location (Perforce filesystem: P4ROOT, P4JOURNAL, P4LOG, TEMP,
	 * or <depot name>)
	 * 
	 * @return the location
	 */
	String getLocation();

	/**
	 * Gets the used bytes.
	 * 
	 * @return the used bytes
	 */
	long getUsedBytes();

	/**
	 * Gets the free bytes.
	 * 
	 * @return the free bytes
	 */
	long getFreeBytes();

	/**
	 * Gets the total bytes.
	 * 
	 * @return the total bytes
	 */
	long getTotalBytes();

	/**
	 * Gets the percent used.
	 * 
	 * @return the percent used
	 */
	int getPercentUsed();

	/**
	 * Gets the file system type (nfs, ext2, xfs, tmpfs, ramfs, ufs, reiserfs,
	 * or <other>).
	 * 
	 * @return the file system type
	 */
	String getFileSystemType();
}
