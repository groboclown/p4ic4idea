/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public interface IFileDiff {

	/**
	 * Status of diff
	 */
	enum Status {

		LEFT_ONLY,

		RIGHT_ONLY,

		CONTENT,
		
		TYPES,

		IDENTICAL;

		public static Status fromString(String serverValue) {
			Status status = null;
			if (serverValue != null) {
				if ("content".equals(serverValue)) {
					status = CONTENT;
				} else if ("left only".equals(serverValue)) {
					status = LEFT_ONLY;
				} else if ("right only".equals(serverValue)) {
					status = RIGHT_ONLY;
				} else if ("types".equals(serverValue)) {
					status = TYPES;
				} else if ("identical".equals(serverValue)) {
					status = IDENTICAL;
				}
			}
			return status;
		}

	}

	/**
	 * Get depot path of first file in diff
	 * 
	 * @return depot file path
	 */
	String getDepotFile1();

	/**
	 * Get revision of first file in diff
	 * 
	 * @return revision number
	 */
	int getRevision1();

	/**
	 * Get depot path of second file in diff
	 * 
	 * @return depot file path
	 */
	String getDepotFile2();

	/**
	 * Get revision of second file in diff
	 * 
	 * @return revision number
	 */
	int getRevision2();

	/**
	 * Get file type of first file in diff
	 * 
	 * @return file type
	 */
	String getFileType1();

	/**
	 * Get file type of second file in diff
	 * 
	 * @return file type
	 */
	String getFileType2();

	/**
	 * Get status of file diff
	 * 
	 * @return status
	 */
	Status getStatus();

}
