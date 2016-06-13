/**
 * 
 */
package com.perforce.p4java.impl.generic.core.file;

import java.util.Map;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSize;

/**
 * Default implementation for the IFileSize interface.
 * 
 * @see com.perforce.p4java.core.file.IFileSize
 */

public class FileSize implements IFileSize {
	
	private String depotFile = null;
	private int revisionId = 0;
	private int fileSize = 0;
	private String path = null;
	private int fileCount = 0;
	private int changeListId = IChangelist.UNKNOWN;
	
	public FileSize(String depotFile, int revisionId, int fileSize, String path,
			int fileCount, int changeListId) {
		this.depotFile = depotFile;
		this.revisionId = revisionId;
		this.fileSize = fileSize;
		this.path = path;
		this.fileCount = fileCount;
		this.changeListId = changeListId;
	}
	
	public FileSize(Map<String, Object> map) {
		if (map != null) {
			if (map.get("depotFile") != null) {
				this.setDepotFile((String) map.get("depotFile"));
			}
			if (map.get("rev") != null) {
				this.setRevisionId(new Integer((String) map.get("rev")));
			}
			if (map.get("fileSize") != null) {
				this.setFileSize(new Integer((String) map.get("fileSize")));
			}
			if (map.get("path") != null) {
				this.setPath((String) map.get("path"));
			}
			if (map.get("fileCount") != null) {
				this.setFileCount(new Integer((String) map.get("fileCount")));
			}
			if (map.get("change") != null) {
				if (((String) map.get("change")).equalsIgnoreCase("default")) {
					this.setChangelistId(IChangelist.DEFAULT);
				} else {
					this.setChangelistId(new Integer((String) map.get("change")));
				}
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#getDepotFile()
	 */
	public String getDepotFile() {
		return this.depotFile;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#setDepotFile(java.lang.String)
	 */
	public void setDepotFile(String depotFile) {
		this.depotFile = depotFile;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#getRevisionId()
	 */
	public int getRevisionId() {
		return this.revisionId;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSize#setRevisionId(int)
	 */
	public void setRevisionId(int revisionId) {
		this.revisionId = revisionId;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#getFileSize()
	 */
	public int getFileSize() {
		return this.fileSize;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#setFileSize(int)
	 */
	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#getPath()
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#setPath(java.lang.String)
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#getFileCount()
	 */
	public int getFileCount() {
		return this.fileCount;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#setFileCount(int)
	 */
	public void setFileCount(int fileCount) {
		this.fileCount = fileCount;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#getChangelistId()
	 */
	public int getChangelistId() {
		return this.changeListId;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSize#setChangelistId(int)
	 */
	public void setChangelistId(int changeListId) {
		this.changeListId = changeListId;
	}
}
