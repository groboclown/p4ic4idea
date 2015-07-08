/**
 * 
 */
package com.perforce.p4java.impl.generic.core.file;

import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IResolveRecord;

/**
 * Default implementation class for the IResolveRecord interface.
 */
public class ResolveRecord implements IResolveRecord {

	private FileAction resolveAction = null;
	private String resolveBaseFile = null;
	private int resolveBaseRevision = 0;
	private int resolveEndFromRevision = 0;
	private String resolveFromFile = null;
	private int resolveStartFromRevision = 0;
	private String resolveType = null;
	
	/**
	 * Default constructor; sets all fields to zero or null.
	 */
	public ResolveRecord() {
	}
	
	/**
	 * Explicit-value constructor.
	 */
	public ResolveRecord(FileAction resolveAction, String resolveBaseFile,
			int resolveBaseRevision, int resolveEndFromRevision,
			String resolveFromFile, int resolveStartFromRevision) {
		this.resolveAction = resolveAction;
		this.resolveBaseFile = resolveBaseFile;
		this.resolveBaseRevision = resolveBaseRevision;
		this.resolveEndFromRevision = resolveEndFromRevision;
		this.resolveFromFile = resolveFromFile;
		this.resolveStartFromRevision = resolveStartFromRevision;
	}
	
	/**
	 * Construct a new ResolveRecord from the passed-in map. This map is assumed
	 * to have been returned from the getExtendedFiles method; other maps will
	 * probably not work. The recNum parameter is used to tell the constructor
	 * which resolve rec to extract. If map is null and / or recNum is negative,
	 * this is equivalent to calling the default constructor.
	 */
	public ResolveRecord(Map<String, Object> map, int recNum) {
		if ((map != null) && (recNum >= 0)) {
			try {
				this.resolveBaseFile = (String) map.get("resolveBaseFile" + recNum);
				this.resolveFromFile = (String) map.get("resolveFromFile" + recNum);
				if (map.containsKey("resolveAction" + recNum)) {
					this.resolveAction = FileAction.fromString((String) map.get("resolveAction" + recNum));
				}
				if (map.containsKey("resolveBaseRev" + recNum)) {
					this.resolveBaseRevision = new Integer((String) map.get("resolveBaseRev" + recNum));
				}
				if (map.containsKey("resolveStartFromRev" + recNum)) {
					this.resolveStartFromRevision = new Integer((String) map.get("resolveStartFromRev" + recNum));
				}
				if (map.containsKey("resolveEndFromRev" + recNum)) {
					this.resolveEndFromRevision = new Integer((String) map.get("resolveEndFromRev" + recNum));
				}
			} catch (Throwable thr) {
				Log.error(
						"Unexpected exception in ResolveRecord constructor: " + thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveAction()
	 */
	public FileAction getResolveAction() {
		return resolveAction;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveAction(com.perforce.p4java.core.file.FileAction)
	 */
	public void setResolveAction(FileAction resolveAction) {
		this.resolveAction = resolveAction;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveBaseFile()
	 */
	public String getResolveBaseFile() {
		return resolveBaseFile;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveBaseFile(String)
	 */
	public void setResolveBaseFile(String resolveBaseFile) {
		this.resolveBaseFile = resolveBaseFile;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveBaseRevision()
	 */
	public int getResolveBaseRevision() {
		return resolveBaseRevision;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveBaseRevision(int)
	 */
	public void setResolveBaseRevision(int resolveBaseRevision) {
		this.resolveBaseRevision = resolveBaseRevision;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveEndFromRevision()
	 */
	public int getResolveEndFromRevision() {
		return resolveEndFromRevision;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveEndFromRevision(int)
	 */
	public void setResolveEndFromRevision(int resolveEndFromRevision) {
		this.resolveEndFromRevision = resolveEndFromRevision;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveFromFile()
	 */
	public String getResolveFromFile() {
		return resolveFromFile;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveFromFile(String)
	 */
	public void setResolveFromFile(String resolveFromFile) {
		this.resolveFromFile = resolveFromFile;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveStartFromRevision()
	 */
	public int getResolveStartFromRevision() {
		return resolveStartFromRevision;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveStartFromRevision(int)
	 */
	public void setResolveStartFromRevision(int resolveStartFromRevision) {
		this.resolveStartFromRevision = resolveStartFromRevision;
	}

	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#getResolveType()
	 */
	public String getResolveType() {
		return resolveType;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IResolveRecord#setResolveType(String)
	 */
	public void setResolveType(String resolveType) {
		this.resolveType = resolveType;
	}
}
