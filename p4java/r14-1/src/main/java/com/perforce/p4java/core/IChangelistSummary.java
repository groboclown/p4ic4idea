/**
 * 
 */
package com.perforce.p4java.core;

import java.util.Date;

import com.perforce.p4java.Log;

/**
 * Defines the methods and operations available on Perforce
 * changelist summaries returned from the server.<p>
 * 
 * Changelist summaries are typically returned from the server's
 * changelist list methods (e.g. getChangelists) and normally
 * contain only the fields returned by the Perforce "p4 changes"
 * command (i.e. they're missing things like files and job lists),
 * and allow only "local" field setter and getter operations. For
 * full changelist functionality, use the IChangelist interface
 * that extends this interface.<p>
 * 
 * Unextended IChangelistSummary objects are complete, and neither
 * refreshable nor updateable.
 */
public interface IChangelistSummary {
	
	/**
	 * Defines the visibility of the changelist. Corresponds to the
	 * server 2010.2 release 'type' field in changelists.
	 * 
	 * @since 2011.1
	 */
	public enum Visibility {
		PUBLIC,
		RESTRICTED,
		UNKNOWN;
		
		/**
		 * Return a suitable Visibility type as inferred from the passed-in
		 * string, which is assumed to be the string form of a Visibility type.
		 * Otherwise return the UNKNOWN type
		 */
		public static Visibility fromString(String str) {
			if (str == null) {
				return null;
			}

			try {
				return Visibility.valueOf(str.toUpperCase());
			} catch (IllegalArgumentException iae) {
				Log.error("Bad conversion attempt in Visibility.fromString; string: "
						+ str + "; message: " + iae.getMessage());
				Log.exception(iae);
				return UNKNOWN;
			}
		}
	};
	
	/**
	 * Return the Perforce changelist's ID.
	 * 
	 * @return changelist ID, or UNKNOWN if unknown or not yet allocated.
	 */
	
	int getId();
	
	/**
	 * Return the description associated with this changelist.
	 * 
	 * @return textual changelist description, or null if no such description.
	 */
	
	String getDescription();
	
	/**
	 * Set the description string for this changelist.
	 * 
	 * @param newDescription non-null new description string.
	 * @return the old description string.
	 */
	
	String setDescription(String newDescription);
	
	/**
	 * Get the status of this changelist, if known.
	 * 
	 * @return IChangelistStatus status, or null if not known.
	 */
	
	ChangelistStatus getStatus();
	
	/**
	 * Get the date the changelist was created or last updated.
	 * 
	 * @return the date the changelist was created or last updated, or null
	 * 			if unknown.
	 */
	
	Date getDate();
	
	/**
	 * Get the ID of the Perforce client workspace associated with this changelist.
	 * 
	 * @return the ID of the client  associated with this changelist, or null if not known.
	 */
	String getClientId();
	
	/**
	 * Get the user name of the user associated with this changelist.
	 * 
	 * @return the user name of the user associated with this changelist,
	 * 			or null if no such name exists or can be determined.
	 */
	
	String getUsername();
	
	/**
	 * Set the changelist ID. Will not cause the associated changelist to
	 * be updated on the Perforce server without a suitable update being performed. 
	 * 
	 * @param id new changelist ID.
	 */
	void setId(int id);
	
	/**
	 * Set the client ID. Will not cause the associated changelist to
	 * be updated on the Perforce server without a suitable update being performed.
	 * 
	 * @param clientId new client ID.
	 */
	void setClientId(String clientId);
	
	/**
	 * Set the changelist owner ID. Will not cause the associated changelist to
	 * be updated on the Perforce server without a suitable update being performed.
	 * 
	 * @param username new owner's user name.
	 */
	void setUsername(String username);
	
	/**
	 * Set the changelist status. Will not cause the associated changelist to
	 * be updated on the Perforce server without a suitable update being performed.
	 * 
	 * @param status
	 */
	void setStatus(ChangelistStatus status);
	
	/**
	 * Set the changelist date. Will not cause the associated changelist to
	 * be updated on the Perforce server without a suitable update being performed.
	 * 
	 * @param date new changelist date
	 */
	void setDate(Date date);
	
	/**
	 * Does this changelist contain at least one shelved file?
	 * 
	 * This only applies to changelists whose {@link #getStatus()} is
	 * {@link ChangelistStatus#PENDING} and that have been returned as
	 * IChangelistSummary objects. The value here is unreliable for
	 * full IChangelist objects returned from getChangelist, etc.
	 * 
	 * @return - true if changelist contains shelved files, false otherwise
	 */
	boolean isShelved();
	
	/**
	 * Set the shelved status of of this changelist (see isShelved()).
	 * 
	 * @param shelved new shelved value.
	 */
	void setShelved(boolean shelved);
	
	/**
	 * Get the visibility associated with this changelist. May be null if no
	 * visibility is associated with this changelist.
	 * 
	 * @since 2011.1
	 * @return possibly-null visibility.
	 */
	Visibility getVisibility();
	
	/**
	 * Set the visibility associated with this changelist.
	 * 
	 * @since 2011.1
	 * @param visibility
	 */
	void setVisibility(Visibility visibility);
}
