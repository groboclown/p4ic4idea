/**
 * 
 */
package com.perforce.p4java.client;

import com.perforce.p4java.core.IServerResource;

import java.util.Date;
import java.util.List;

/**
 * Lightweight Perforce client interface that defines summary client
 * metadata and associated operations, without providing client
 * views or actual client-related operations. Corresponds closely to
 * the information retruned by a "p4 clients" command.<p>
 * 
 * Lightweight IClientSummary objects are typically returned from client list
 * operations such as IServer.getClientList; further use of these specs for
 * heavier-weight operations (etc.) requires getting the full client
 * (IClient) object from the server via an explicit getClient() or similar
 * operation.<p>
 * 
 * Note that field setter methods defined below have local effect only, and in order to
 * update the corresponding client on the Perforce server, you would need to create a
 * corresponding full IClient object and update that object on the server.
 * 
 * "Pure" IClientSummary objects (those implementing just the IClientSummary interface) are
 * complete but are not refreshable or updateable (full IClient objects, on the other
 * hand, are complete, refreshable, and updateable).
 */

public interface IClientSummary extends IServerResource {
	
	/**
	 * Defines what options are available or set (or whatever) for a specific Perforce Client.<p>
	 * 
	 * Perforce client options are described in more detail elsewhere in the Perforce documentation,
	 * but the individual method descriptions below attempt to give the general idea...
	 */
	
	public interface IClientOptions {
		boolean isAllWrite();
		void setAllWrite(boolean allWrite);
		boolean isClobber();
		void setClobber(boolean clobber);
		boolean isCompress();
		void setCompress(boolean compress);
		boolean isLocked();
		void setLocked(boolean locked);
		boolean isModtime();
		void setModtime(boolean modtime);
		boolean isRmdir();
		void setRmdir(boolean rmdir);
	};
	
	/**
	 * Defines the options to be used when submitting Perforce changelists associated
	 * with this Perforce client. Note that the options here are mutually-exclusive,
	 * but it's left up to users and / or implementors to enforce this (the standard
	 * implementation enforces this under normal circumstances).<p>
	 * 
	 * Perforce changelist submit options are described in more detail elsewhere in the
	 * Perforce documentation, but the individual method descriptions below attempt to give
	 * the general idea...
	 */
	
	public interface IClientSubmitOptions {
		
		/**
		 * REOPEN - +reopen
		 */
		String REOPEN = "+reopen";
		
		/**
		 * SUBMIT_UNCHANGED - submitunchanged
		 */
		String SUBMIT_UNCHANGED = "submitunchanged";
		
		/**
		 * SUBMIT_UNCHANGED_REOPEN - submitunchanged+reopen
		 */
		String SUBMIT_UNCHANGED_REOPEN = SUBMIT_UNCHANGED + REOPEN;
		
		/**
		 * LEAVE_UNCHANGED - leaveunchanged
		 */
		String LEAVE_UNCHANGED = "leaveunchanged";
		
		/**
		 * LEAVE_UNCHANGED_REOPEN - leaveunchanged+reopen
		 */
		String LEAVE_UNCHANGED_REOPEN = LEAVE_UNCHANGED + REOPEN;
		
		/**
		 * REVERT_UNCHANGED - revertunchanged
		 */
		String REVERT_UNCHANGED = "revertunchanged";
		
		/**
		 * REVERT_UNCHANGED_REOPEN - revertunchanged+reopen
		 */
		String REVERT_UNCHANGED_REOPEN = REVERT_UNCHANGED + REOPEN;
		
		boolean isSubmitunchanged();
		void setSubmitunchanged(boolean submitunchanged);
		boolean isSubmitunchangedReopen();
		void setSubmitunchangedReopen(boolean submitunchangedReopen);
		boolean isRevertunchanged();
		void setRevertunchanged(boolean revertunchanged);
		boolean isRevertunchangedReopen();
		void setRevertunchangedReopen(boolean revertunchangedReopen);
		boolean isLeaveunchanged();
		void setLeaveunchanged(boolean leaveunchanged);
		boolean isLeaveunchangedReopen();
		void setLeaveunchangedReopen(boolean leaveunchangedReopen);
	};
	
	/**
	 * Defines the line end options available for text files.
	 */
	
	public enum ClientLineEnd {
		LOCAL,
		UNIX,
		MAC,
		WIN,
		SHARE;
		
		/**
		 * A slightly looser valueOf(String)
		 */
		
		public static ClientLineEnd getValue(String str) {
			if (str != null) {
				for (ClientLineEnd le : ClientLineEnd.values()) {
					if (le.toString().equalsIgnoreCase(str)) {
						return le;
					}
				}
			}
			
			return null;
		}
	};
	
	/**
	 * Get the name of this client.
	 * 
	 * @return the name of this client, if set or known; null otherwise.
	 */
	
	String getName();
	
	/**
	 * Set the name of this client.
	 * 
	 * @param name new client name.
	 */
	void setName(String name);
	
	/**
	 * Get the date the client's specification was last modified.
	 * 
	 * @return the date the client's specification was last modified,
	 * 			or null if not known.
	 */
	
	Date getUpdated();
	
	/**
	 * Set the client's updated date / time.
	 * 
	 * @param updated new updated date.
	 */
	void setUpdated(Date updated);
	
	/**
	 * Gets the date this client was last used in any way. Note that this
	 * is a server-side date and does not reflect client-side usage.
	 * 
	 * @return the date this client was last used in any way, or
	 * 			null if not known.
	 */
	
	Date getAccessed();
	
	/**
	 * Set the client's accessed date / time.
	 * 
	 * @param accessed new accessed date.
	 */
	void setAccessed(Date accessed);
	
	/**
	 * Get the name of the owner of this Perforce client.
	 * 
	 * @return the name of the owner of this Perforce client, if known;
	 * 			null otherwise.
	 */
	
	String getOwnerName();
	
	/**
	 * Set the name of the owner of this client.
	 * 
	 * @param ownerName the name of the owner of this Perforce client.
	 */
	void setOwnerName(String ownerName);
	
	/**
	 * Returns the name of the associated host, if any.
	 * 
	 * @return the name of the associated host, if any; null otherwise.
	 */
	
	String getHostName();
	
	/**
	 * Set the name of the associated host.
	 * 
	 * @param hostName new host name.
	 */
	void setHostName(String hostName);
	
	/**
	 * Returns a short description of the Perforce server client.
	 * 
	 * @return the short description of the client, or null if no such
	 * 				description is available.
	 */
	
	String getDescription();
	
	/**
	 * Set the description associated with this client.
	 * 
	 * @param description new description string.
	 */
	void setDescription(String description);
	
	/**
	 * Returns the root of this Perforce client.
	 * 
	 * @return the root of this client, or null if no such root is available.
	 */
	
	String getRoot();
	
	/**
	 * Set the root of this client.
	 * 
	 * @param root new client root.
	 */
	void setRoot(String root);
	
	/**
	 * Get the alternate roots associated with this Perforce client,
	 * if any.
	 * 
	 * @return list of alternate roots if they exist; null otherwise.
	 */
	
	List<String> getAlternateRoots();
	
	/**
	 * Set the alternate roots associated with this Perforce client.
	 * 
	 * @param alternateRoots new alternate roots list. Note that order
	 * 			within the list is significant.
	 */
	void setAlternateRoots(List<String> alternateRoots);
	
	/**
	 * Get the line end options for this client.
	 * 
	 * @return ClientLineEnd representing the line end options for this client.
	 */
	
	ClientLineEnd getLineEnd();
	
	/**
	 * Set the line end options for this client.
	 * 
	 * @param lineEnd ClientLineEnd representing the line end options for this client.
	 */
	void setLineEnd(ClientLineEnd lineEnd);
	
	/**
	 * Get the Perforce client options associated with this client.
	 * 
	 * @return non-null options
	 */
	
	IClientOptions getOptions();
	
	/**
	 * Set the client options associated with this client.
	 * 
	 * @param options new options.
	 */
	void setOptions(IClientOptions options);
	
	/**
	 * Get the Perforce client changelist submit options associated with this client.
	 * 
	 * @return non-null changelist submit options
	 */
	
	IClientSubmitOptions getSubmitOptions();
	
	/**
	 * Set the client submit options for this client.
	 * 
	 * @param submitOptions new client submit options.
	 */
	void setSubmitOptions(IClientSubmitOptions submitOptions);
	
	/**
	 * Get the stream's path in a stream depot, of the form //depotname/streamname,
	 * to which this client's view will be dedicated.
	 * 
	 * @return the stream's path in a stream depot of this client, or null if this
	 * 			is not a stream client.
	 */
	
	String getStream();
	
	/**
	 * Set the stream's path in a stream depot, of the form //depotname/streamname,
	 * to which this client's view will be dedicated.
	 * 
	 * @param stream new stream's path in a stream depot of this client.
	 */
	void setStream(String stream);
	
	/**
	 * Convenience method to check if this is a stream client
	 */
	boolean isStream();
	
	/**
	 * Get the server id associated with this client.
	 * 
	 * @return the server id associated with this client, or null if this
	 * 			client has no associated server id.
	 */
	
	String getServerId();
	
	/**
	 * Set the server id of this client.
	 * 
	 * @param serverId new server id for this client.
	 */

	void setServerId(String serverId);

	/**
	 * Get the changelist id associated with this dynamically generated
	 * back-in-time stream client.
	 * 
	 * @return the changelist id associated with this dynamically generated
	 *         back-in-time stream client, or IChangelist.UNKNOWN if this is not
	 *         a dynamically generated back-in-time stream client.
	 */

	int getStreamAtChange();

	/**
	 * Set the changelist id associated with this dynamically generated
	 * back-in-time stream client.
	 * 
	 * @param streamAtChange
	 *            new changelist id associated with this dynamically generated
	 *            back-in-time stream client.
	 */

	void setStreamAtChange(int streamAtChange);
	
	/**
	 * Return the "unloaded" status for this client.
	 * 
	 * @return true iff the client is unloaded.
	 */
	boolean isUnloaded();

	/**
	 * Get the client workspace type 'graph' for Graph support
	 *
	 * @return String representation of the type
	 */
	String getType();

	/**
	 * Set the client workspace type 'graph' for Graph support
	 *
	 * @param type the type as a String
	 */
	void setType(String type);

	/**
	 * Get the client's participation in backup enable/disable. If not
	 * specified backup of a writable client defaults to enabled.
	 *
	 * @return String representation of the type
	 */
	String getBackup();

	/**
	 * Set the client's participation in backup enable/disable
	 *
	 * @param backup enable/disable as a String
	 */
	void setBackup(String backup);
}
