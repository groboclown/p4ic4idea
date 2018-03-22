/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

import java.util.List;

import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.server.IOptionsServer;

/**
 * Defines a full Perforce stream specification. A stream specification ('spec')
 * names a path in a stream depot to be treated as a stream. (See 'p4 help
 * streamintro'.) The spec also defines the stream's lineage, its view, and its
 * expected flow of change.
 * <p>
 * 
 * Full stream specs in the current implementation are always complete.
 */

public interface IStream extends IStreamSummary {

	/**
	 * Defines an "extraTag*" field
	 */
	public interface IExtraTag {
		String getName();
		void setName(String name);
		
		String getType();
		void setType(String type);
		
		String getValue();
		void setValue(String value);
	};

	/**
	 * Return the view map associated with this stream. One or more mappings
	 * that define file paths in the stream view. Each line is of the form:
	 * <path_type> <view_path> [<depot_path>]
	 * 
	 * @return non-null list of IStreamViewMapping mappings for this stream.
	 */
	ViewMap<IStreamViewMapping> getStreamView();

	/**
	 * Set the view map associated with this stream spec. This will not
	 * change the associated stream spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param streamView
	 *            new view mappings for the stream.
	 */
	void setStreamView(ViewMap<IStreamViewMapping> streamView);

	/**
	 * Return the remapped view map associated with this stream. Optional; one
	 * or more mappings that define how stream view paths are to be remapped in
	 * client views. Each line is of the form: <view_path_1> <view_path_2>
	 * 
	 * @return possibly-null (optional) list of IStreamRemappedMapping mappings
	 *         for this stream.
	 */
	ViewMap<IStreamRemappedMapping> getRemappedView();

	/**
	 * Set the remapped view map associated with this stream spec. This will
	 * not change the associated stream spec on the Perforce server unless you
	 * arrange for the update to server.
	 * 
	 * @param remappedView
	 *            new remapped view mappings for the stream.
	 */
	void setRemappedView(ViewMap<IStreamRemappedMapping> remappedView);

	/**
	 * Return the ignored view map associated with this stream. Optional; a list
	 * of file or directory names to be ignored in client views. mappings in the
	 * "Ignored" field may appear in any order. Ignored names are inherited by
	 * child stream client views.
	 * 
	 * @return possibly-null (optional) list of IStreamIgnoredMapping mappings
	 *         to be ignored for this stream.
	 */
	ViewMap<IStreamIgnoredMapping> getIgnoredView();

	/**
	 * Set the ignored view map associated with this stream spec. This will
	 * not change the associated stream spec on the Perforce server unless you
	 * arrange for the update to server.
	 */
	void setIgnoredView(ViewMap<IStreamIgnoredMapping> ignoredView);

	/**
	 * Return the automatically generated client view map associated with this
	 * stream. Maps files in the depot to files in your client workspace.
	 * 
	 * @return possibly-null list of automatically generated IClientViewMapping
	 *         mappings associated with this stream.
	 */
	ViewMap<IClientViewMapping> getClientView();

	/**
	 * Set the automatically generated client view map associated with this
	 * stream spec. This will not change the associated stream spec on the
	 * Perforce server unless you arrange for the update to server.
	 */
	void setClientView(ViewMap<IClientViewMapping> clientView);

	/**
	 * Return a list of extra tags associated with this stream.
	 * 
	 * @return possibly-null list of extra tags associated with this stream.
	 */
	List<IExtraTag> getExtraTags();
	
	/**
	 * Set the extra tags associated with this stream. This will not change
	 * the associated stream spec on the Perforce server unless you arrange for
	 * the update to server.
	 */
	void setExtraTags(List<IExtraTag> extraTags);
	
	/**
	 * Set the server to type of IOptionsServer, overriding the default IServer.
	 */
	void setServer(IOptionsServer server);
}
