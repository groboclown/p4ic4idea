/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.admin.IDiskSpace;

/**
 * Default implementation of the IDiskSpace interface.
 */
public class DiskSpace implements IDiskSpace {

	/** The location. */
	private String location = null;

	/** The used bytes. */
	private long usedBytes = 0;

	/** The free bytes. */
	private long freeBytes = 0;

	/** The total bytes. */
	private long totalBytes = 0;

	/** The percentage used. */
	private int percentageUsed = 0;

	/** The file system type. */
	private String fileSystemType = null;

	/**
	 * Instantiates a new disk space.
	 * 
	 * @param location
	 *            the location
	 * @param usedBytes
	 *            the used bytes
	 * @param freeBytes
	 *            the free bytes
	 * @param totalBytes
	 *            the total bytes
	 * @param percentageUsed
	 *            the percentage used
	 * @param fileSystemType
	 *            the file system type
	 */
	public DiskSpace(String location, long usedBytes, long freeBytes,
			long totalBytes, int percentageUsed, String fileSystemType) {

		this.location = location;
		this.usedBytes = usedBytes;
		this.freeBytes = freeBytes;
		this.totalBytes = totalBytes;
		this.percentageUsed = percentageUsed;
		this.fileSystemType = fileSystemType;
	}

	/**
	 * Constructs a DiskSpace from the passed-in map; this map must have come
	 * from a Perforce IServer method call or it may fail. If map is null,
	 * equivalent to calling the default constructor.
	 * 
	 * @param map
	 *            the map
	 */
	public DiskSpace(Map<String, Object> map) {
		if (map != null) {
			try {
				if (map.containsKey("location")) {
					this.location = (String) map.get("location");
				}
				if (map.containsKey("usedBytes")) {
					this.usedBytes = new Long((String) map.get("usedBytes"));
				}
				if (map.containsKey("freeBytes")) {
					this.freeBytes = new Long((String) map.get("freeBytes"));
				}
				if (map.containsKey("totalBytes")) {
					this.totalBytes = new Long((String) map.get("totalBytes"));
				}
				if (map.containsKey("pctUsed")) {
					this.percentageUsed = new Integer((String) map.get("pctUsed"));
				}
				if (map.containsKey("fsType")) {
					this.fileSystemType = (String) map.get("fsType");
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}
		}
	}

	/**
	 * Gets the location.
	 * 
	 * @return the location
	 * @see com.perforce.p4java.admin.IDiskSpace#getLocation()
	 */
	public String getLocation() {
		return this.location;
	}

	/**
	 * Gets the used bytes.
	 * 
	 * @return the used bytes
	 * @see com.perforce.p4java.admin.IDiskSpace#getUsedBytes()
	 */
	public long getUsedBytes() {
		return this.usedBytes;
	}

	/**
	 * Gets the free bytes.
	 * 
	 * @return the free bytes
	 * @see com.perforce.p4java.admin.IDiskSpace#getFreeBytes()
	 */
	public long getFreeBytes() {
		return this.freeBytes;
	}

	/**
	 * Gets the total bytes.
	 * 
	 * @return the total bytes
	 * @see com.perforce.p4java.admin.IDiskSpace#getTotalBytes()
	 */
	public long getTotalBytes() {
		return this.totalBytes;
	}

	/**
	 * Gets the percent used.
	 * 
	 * @return the percent used
	 * @see com.perforce.p4java.admin.IDiskSpace#getPercentUsed()
	 */
	public int getPercentUsed() {
		return this.percentageUsed;
	}

	/**
	 * Gets the file system type.
	 * 
	 * @return the file system type
	 * @see com.perforce.p4java.admin.IDiskSpace#getFileSystemType()
	 */
	public String getFileSystemType() {
		return this.fileSystemType;
	}
}
