/**
 * 
 */
package com.perforce.p4java.core.file;

import java.util.Date;
import java.util.List;

/**
 * Describes a Perforce file revision in detail, including the changelist number and
 * associated description, action, user, etc. data. Full field semantics and usage
 * are given in the main Perforce documentation.
 */

public interface IFileRevisionData {
	
	/**
	 * Get the revision ID associated with this revision.
	 */
	int getRevision();
	
	/**
	 * Get the changelist ID associated with this revision.
	 */
	int getChangelistId();
	
	/**
	 * Get the file action associated with this revision.
	 */
	FileAction getAction();
	
	/**
	 * Get the date associated with this revision.
	 */
	Date getDate();
	
	/**
	 * Get the Perforce user name associated with this revision.
	 */
	String getUserName();
	
	/**
	 * Get the Perforce file type string associated with this revision.
	 */
	String getFileType();
	
	/**
	 * Get the description string associated with this revision.
	 */
	String getDescription();
	
	/**
	 * Get the depot file name associated with this revision.
	 */
	String getDepotFileName();
	
	/**
	 * Get the client file name associated with this revision.
	 */
	String getClientName();
	
	/**
	 * This method can be used to retrieve a (possibly-empty or even
	 * null) list of contributory integration data for revisions that
	 * have resulted from (or caused) a merge or branch. There's generally
	 * no easy way to tell whether there's anything to be retrieved here,
	 * so you may have to always call it and if it's null or empty, just
	 * ignore it...
	 * 
	 * @return potentially null or empty list of revision integration data
	 * 			for this specific revision.
	 */
	List<IRevisionIntegrationData> getRevisionIntegrationDataList();

	@Deprecated
	List<IRevisionIntegrationData> getRevisionIntegrationData();
}
