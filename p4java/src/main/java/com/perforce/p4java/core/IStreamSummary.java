/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

import java.util.Date;

import com.perforce.p4java.Log;

/**
 * Defines the summary Perforce stream metadata typically returned by the
 * getStreamSummaryList() method, corresponding to "p4 streams" and similar.
 * <p>
 * 
 * In general, stream summary information excludes the stream paths, remapped,
 * ignored, and no server-side operations can be performed against them; for
 * full stream functionality you should use the full IStream interface.
 * <p>
 * 
 * Stream summaries are complete and not refreshable or updateable.
 */
public interface IStreamSummary extends IServerResource {

	/**
	 * Types of streams include 'mainline', 'release', 'development', 'virtual'
	 * and 'task'. The default is 'development'.<p>
	 * 
	 * Defines the role of a stream: A 'mainline' may not have a parent. A
	 * 'virtual' stream is not a stream but an alternate view of its parent
	 * stream. The 'development' and 'release' streams have controlled flow. Can
	 * be changed. A 'task' stream is a lightweight short-lived stream that only
	 * promotes modified content to the repository, branched data is stored in
	 * shadow tables that are removed when the task stream is deleted or
	 * unloaded.
	 */
	public enum Type {
		MAINLINE,
		RELEASE,
		DEVELOPMENT,
		VIRTUAL,
		TASK,
		UNKNOWN;

		/**
		 * Return a suitable Stream type as inferred from the passed-in
		 * string, which is assumed to be the string form of a Stream type.
		 * Otherwise return the UNKNOWN Stream type
		 */
		public static Type fromString(String str) {
			if (str == null) {
				return null;
			}

			try {
				return Type.valueOf(str.toUpperCase());
			} catch (IllegalArgumentException iae) {
				Log.error("Bad conversion attempt in Type.fromString; string: "
						+ str + "; message: " + iae.getMessage());
				Log.exception(iae);
				return UNKNOWN;
			}
		}
	};

	/**
	 * Stream options are flags to configure stream behavior.
	 * <p>
	 * 
	 * unlocked (default) / locked: Indicates whether the stream spec is locked
	 * against modifications. If locked, the spec may not be deleted, and only
	 * its owner may modify it.
	 * <p>
	 * 
	 * allsubmit (default) / ownersubmit: Indicates whether all users or only
	 * the of the stream may submit changes to the stream path.
	 * 
	 * toparent (default) / notoparent: Indicates whether integration from the
	 * stream to its parent is expected to occur.
	 * <p>
	 * 
	 * fromparent (default) / nofromparent: Indicates whether integration to the
	 * stream from its parent is expected to occur.
	 */
	public interface IOptions {
		void setLocked(boolean locked);

		boolean isLocked();

		void setOwnerSubmit(boolean ownerSubmit);

		boolean isOwnerSubmit();

		boolean isNoToParent();

		void setNoToParent(boolean noToParent);

		boolean isNoFromParent();

		void setNoFromParent(boolean noFromParent);
	};

	/**
	 * Get the stream's path in a stream depot.
	 */
	String getStream();

	/**
	 * Get the alternate name of the stream.
	 */
	String getName();

	/**
	 * Get the name of the user who created this stream.
	 */
	String getOwnerName();

	/**
	 * Get the date specification was last modified.
	 */
	Date getUpdated();

	/**
	 * Get the date of the last 'integrate' using this stream.
	 */
	Date getAccessed();

	/**
	 * Get the stream's description (if any).
	 */
	String getDescription();

	/**
	 * Get the stream's parent.
	 */
	String getParent();
	
	/**
	 * Get the stream type
	 */
	Type getType();

	/**
	 * Get the stream options
	 */
	IOptions getOptions();

	/**
	 * Is firmer than parent.
	 */
	boolean isFirmerThanParent();

	/**
	 * Is change flows to parent.
	 */
	boolean isChangeFlowsToParent();

	/**
	 * Is change flows from parent.
	 */
	boolean isChangeFlowsFromParent();

	/**
	 * Get the stream's base parent.
	 */
	String getBaseParent();
	
	/**
	 * Set the stream's path. This will not change the associated stream spec on
	 * the Perforce server unless you arrange for the update to server.
	 * 
	 * @param stream
	 *            new stream's path
	 */
	void setStream(String stream);

	/**
	 * Set the name of this stream. This will not change the associated stream
	 * spec on the Perforce server unless you arrange for the update to server.
	 * 
	 * @param name
	 *            new stream spec name
	 */
	void setName(String name);

	/**
	 * Set the owner's name for this stream. This will not change the associated
	 * stream spec on the Perforce server unless you arrange for the update to
	 * server.
	 * 
	 * @param ownerName
	 *            new owner's name
	 */
	void setOwnerName(String ownerName);

	/**
	 * Set the last-updated date. This generally has no effect on the associated
	 * Perforce server version of this spec.
	 * 
	 * @param updated
	 *            new updated date.
	 */
	void setUpdated(Date updated);

	/**
	 * Set the last-accessed date. This generally has no effect on the
	 * associated Perforce server version of this spec.
	 * 
	 * @param accessed
	 *            new accessed date.
	 */
	void setAccessed(Date accessed);

	/**
	 * Set the stream spec description. This will not change the associated
	 * stream spec on the Perforce server unless you arrange for the update to
	 * server.
	 * 
	 * @param description
	 *            new description string.
	 */
	void setDescription(String description);

	/**
	 * Set the stream parent. This will not change the associated stream spec on
	 * the Perforce server unless you arrange for the update to server.
	 * 
	 * @param parent
	 *            new stream parent.
	 */
	void setParent(String parent);

	/**
	 * Set the stream type. This will not change the associated stream spec on
	 * the Perforce server unless you arrange for the update to server.
	 * 
	 * @param type
	 *            new stream type.
	 */
	void setType(Type type);

	/**
	 * Set the stream options. This will not change the associated stream spec
	 * on the Perforce server unless you arrange for the update to server.
	 * 
	 * @param options
	 *            new stream options.
	 */
	void setOptions(IOptions options);

	/**
	 * Set (true/false) the stream is firmer than parent.
	 */
	void setFirmerThanParent(boolean firmerThanParent);

	/**
	 * Set (true/false) the stream's change flows to parent.
	 */
	void setChangeFlowsToParent(boolean changeFlowsToParent);

	/**
	 * Set (true/false) the stream's change flows from parent.
	 */
	void setChangeFlowsFromParent(boolean changeFlowsFromParent);

	/**
	 * Set the stream base parent. This will not change the associated stream
	 * spec on the Perforce server unless you arrange for the update to server.
	 * 
	 * @param baseParent
	 *            new stream base parent.
	 */
	void setBaseParent(String baseParent);
	
	/**
	 * Return the "unloaded" status for this stream.
	 * 
	 * @return true iff the stream is unloaded.
	 */
	boolean isUnloaded();
}
