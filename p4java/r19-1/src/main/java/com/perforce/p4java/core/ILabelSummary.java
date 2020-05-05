/**
 * 
 */
package com.perforce.p4java.core;

import java.util.Date;

/**
 * Defines summary information for Perforce labels. Label summary
 * objects contain the summary field information returned by (e.g.)
 * the "p4 labels" command (which does not include the label view),
 * and do not support server-side operations against them. For the
 * full Perforce label object functionality, see ILabel, which
 * extends this interface.<p>
 * 
 * ILabelSummary objects are complete and neither refreshable nor updateable.<p>
 * 
 * See the main Perforce documentation for label usage and semantics.<p>
 */

public interface ILabelSummary extends IServerResource {
	
	/**
	 * Get the label's name (the label's label, so to speak).
	 * 
	 * @return possibly-null label name (should only be null if the underlying label
	 * 			object has just been created on the client and has no server-side
	 * 			counterpart yet).
	 */
	String getName();
	
	/**
	 * Set the label's name.
	 * 
	 * @param name the label's (new) name.
	 */
	void setName(String name);
	
	/**
	 * Get the name of the owner of this label.
	 * 
	 * @return possibly-null owner name.
	 */
	String getOwnerName();
	
	/**
	 * Set the name of the owner of this label.
	 * 
	 * @param ownerName the new owner name.
	 */
	void setOwnerName(String ownerName);
	
	/**
	 * Get the date and time the label specification was last updated.
	 * 
	 * @return possibly-null Date
	 */
	Date getLastUpdate();
	
	/**
	 * Set the date and time the label specification was last updated.
	 * 
	 * @param lastUpdate Date last updated.
	 */
	void setLastUpdate(Date lastUpdate);
	
	/**
	 * Get the date and time of the last 'labelsync' or use of '@label'
	 * on this label.
	 * 
	 * @return possibly-null Date
	 */
	Date getLastAccess();
	
	/**
	 * Set the date and time of the last 'labelsync' or use of '@label'
	 * on this label.
	 * 
	 * @param lastAccess new last access date.
	 */
	void setLastAccess(Date lastAccess);
	
	/**
	 * Get the description associated with this label.
	 * 
	 * @return possibly-null description string.
	 */
	String getDescription();
	
	/**
	 * Set the description associated with this label.
	 * 
	 * @param description new label description string.
	 */
	void setDescription(String description);
	
	/**
	 * Return the "locked/unlocked" status for this label.
	 * 
	 * @return true iff the label is locked else it is unlocked.
	 */
	boolean isLocked();
	
	/**
	 * Set the "locked/unlocked" status for this label.
	 * 
	 * @param locked true iff the label is locked else it is unlocked.
	 */
	void setLocked(boolean locked);
	
	/**
	 * Return the "autoreload/noautoreload" status for this label.
	 * 
	 * @return true iff the label is "autoreload" else it is "noautoreload".
	 */
	boolean isAutoReload();
	
	/**
	 * Set the "autoreload/noautoreload" status for this label.
	 * 
	 * @param autoreload true iff the label is "autoreload" else it is "noautoreload".
	 */
	void setAutoReload(boolean autoreload);

	/**
	 * Get the optional revision specification for this label.
	 * 
	 * @return possibly-null revision spec string.
	 */
	String getRevisionSpec();
	
	/**
	 * Set the optional revision specification for this label.
	 * 
	 * @param revisionSpec new revision spec string
	 */
	void setRevisionSpec(String revisionSpec);

	/**
	 * Return the "unloaded" status for this label.
	 * 
	 * @return true iff the label is unloaded.
	 */
	boolean isUnloaded();
}
