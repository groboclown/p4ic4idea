/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core.file;

import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.impl.generic.core.file.FileSpec;

import java.util.Map;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public class FileDiff implements IFileDiff {

	private Status status = null;
	private String file1 = null;
	private String file2 = null;
	private int revision1 = -1;
	private int revision2 = -1;
	private String type1 = null;
	private String type2 = null;

	/**
	 * Create a new file diff with the values from the specified map
	 * 
	 * @param map
	 */
	public FileDiff(Map<String, Object> map) {
		String depotFile1 = (String) map.get("depotFile");
		if (depotFile1 != null) {
			this.file1 = depotFile1;
			this.revision1 = FileSpec.getRevFromString((String) map.get("rev"));
			this.type1 = (String) map.get("type");
		}
		String depotFile2 = (String) map.get("depotFile2");
		if (depotFile2 != null) {
			this.file2 = depotFile2;
			this.revision2 = FileSpec
					.getRevFromString((String) map.get("rev2"));
			this.type2 = (String) map.get("type2");
		}
		this.status = Status.fromString((String) map.get("status"));
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getStatus()
	 */
	public Status getStatus() {
		return this.status;
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getDepotFile1()
	 */
	public String getDepotFile1() {
		return this.file1;
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getDepotFile2()
	 */
	public String getDepotFile2() {
		return this.file2;
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getRevision1()
	 */
	public int getRevision1() {
		return this.revision1;
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getRevision2()
	 */
	public int getRevision2() {
		return this.revision2;
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getFileType1()
	 */
	public String getFileType1() {
		return this.type1;
	}

	/**
	 * @see com.perforce.p4java.core.IFileDiff#getFileType2()
	 */
	public String getFileType2() {
		return this.type2;
	}

}
