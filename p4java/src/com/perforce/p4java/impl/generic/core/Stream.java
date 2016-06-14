/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamIgnoredMapping;
import com.perforce.p4java.core.IStreamRemappedMapping;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.IStreamViewMapping;
import com.perforce.p4java.core.IStreamViewMapping.PathType;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;

/**
 * Simple default implementation class for the IStream interface.
 */

public class Stream extends StreamSummary implements IStream {

	protected ViewMap<IStreamViewMapping> streamView = null;
	protected ViewMap<IStreamRemappedMapping> remappedView = null;
	protected ViewMap<IStreamIgnoredMapping> ignoredView = null;
	protected ViewMap<IClientViewMapping> clientView = null;
	protected List<IExtraTag> extraTags = null;

	/**
	 * Default description for use in newStream method when no explicit
	 * description is given.
	 */
	public static final String DEFAULT_DESCRIPTION = "New stream created by P4Java";

	/**
	 * Simple default generic IExtraTag implementation class.
	 */
	public static class ExtraTag implements IExtraTag {

		private String name = null;
		private String type = null;
		private String value = null;

		/**
		 * Default constructor; sets all fields to false.
		 */
		public ExtraTag() {
		}

		/**
		 * Explicit-value constructor.
		 */
		public ExtraTag(String name, String type, String value) {
			if (name == null) {
				throw new NullPointerError("null name in Stream.ExtraTag constructor.");
			}
			if (type == null) {
				throw new NullPointerError("null type in Stream.ExtraTag constructor.");
			}
			if (value == null) {
				throw new NullPointerError("null value in Stream.ExtraTag constructor.");
			}

			this.name = name;
			this.type = type;
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getType() {
			return this.type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
	
	/**
	 * Simple factory / convenience method for creating a new local Stream
	 * object with defult values.
	 */
	public static Stream newStream(IOptionsServer server, String streamPath,
			String type, String parentStreamPath, String name,
			String description, String options, String[] viewPaths,
			String[] remappedPaths, String[] ignoredPaths) {

		return newStream(server, streamPath, type, parentStreamPath, name,
				description, options, viewPaths, remappedPaths, ignoredPaths,
				null);
	}

	/**
	 * Simple factory / convenience method for creating a new local Stream
	 * object with defult values.
	 * 
	 * @param server
	 *            non-null server to be associated with the new stream spec.
	 * @param streamPath
	 *            non-null stream's path in a stream depot, of the form
	 *            //depotname/streamname.
	 * @param type
	 *            non-null stream type of 'mainline', 'development', or
	 *            'release'.
	 * @param parentStreamPath
	 *            parent of this stream. Can be null if the stream type is
	 *            'mainline', otherwise must be set to an existing stream.
	 * @param name
	 *            an alternate name of the stream, for use in display outputs.
	 *            Defaults to the 'streamname' portion of the stream path. Can
	 *            be changed.
	 * @param description
	 *            if not null, used as the new stream spec's description field;
	 *            if null, uses the Stream.DEFAULT_DESCRIPTION field.
	 * @param options
	 *            flags to configure stream behavior: allsubmit/ownersubmit
	 *            [un]locked [no]toparent [no]fromparent.
	 * @param viewPaths
	 *            one or more lines that define file paths in the stream view.
	 *            Each line is of the form: <path_type> <view_path>
	 *            [<depot_path>]
	 * @param remappedPaths
	 *            optional; one or more lines that define how stream view paths
	 *            are to be remapped in client views. Each line is of the form:
	 *            <view_path_1> <view_path_2>
	 * @param ignoredPaths
	 *            optional; a list of file or directory names to be ignored in
	 *            client views. For example:
	 *            /tmp      # ignores files named 'tmp'
	 *            /tmp/...  # ignores dirs named 'tmp'
	 *            .tmp      # ignores file names ending in '.tmp'
	 * @param clientViewPaths
	 *            automatically generated; maps files in the depot to files in
	 *            your client workspace. For example:
	 *            //p4java_stream/dev/... ...
	 *            //p4java_stream/dev/readonly/sync/p4cmd/%%1 readonly/sync/p4cmd/%%1
	 *            -//p4java_stream/.../temp/... .../temp/...
	 *            -//p4java_stream/....class ....class
	 * @return new local Stream object.
	 */
	public static Stream newStream(IOptionsServer server, String streamPath,
			String type, String parentStreamPath, String name,
			String description, String options, String[] viewPaths,
			String[] remappedPaths, String[] ignoredPaths,
			String[] clientViewPaths) {
		if (server == null) {
			throw new NullPointerError("null server in Stream.newStream()");
		}
		if (type == null) {
			throw new NullPointerError("null stream type in Stream.newStream()");
		}
		if (streamPath == null) {
			throw new NullPointerError("null stream in Stream.newStream()");
		}

		ViewMap<IStreamViewMapping> streamView = new ViewMap<IStreamViewMapping>();
		if (viewPaths != null) {
			int i = 0;
			for (String mapping : viewPaths) {
				if (mapping == null) {
					throw new NullPointerError("null view mapping string passed to Stream.newStream");
				}
				streamView.addEntry(new StreamViewMapping(i, mapping));
				i++;
			}
		} else {
			streamView.addEntry(new StreamViewMapping(0, PathType.SHARE, "...", null));
		}

		ViewMap<IStreamRemappedMapping> remappedView = null;
		if (remappedPaths != null) {
			remappedView = new ViewMap<IStreamRemappedMapping>();
			int i = 0;
			for (String mapping : remappedPaths) {
				if (mapping == null) {
					throw new NullPointerError("null remapped mapping string passed to Stream.newStream");
				}
				remappedView.addEntry(new StreamRemappedMapping(i, mapping));
				i++;
			}
		}

		ViewMap<IStreamIgnoredMapping> ignoredView = null;
		if (ignoredPaths != null) {
			ignoredView = new ViewMap<IStreamIgnoredMapping>();
			int i = 0;
			for (String mapping : ignoredPaths) {
				if (mapping == null) {
					throw new NullPointerError("null ignored path string passed to Stream.newStream");
				}
				ignoredView.addEntry(new StreamIgnoredMapping(i, mapping));
				i++;
			}
		}

		List<IClientViewMapping> clientView = null;
		if (clientViewPaths != null) {
			clientView = new ArrayList<IClientViewMapping>();
			int i = 0;
			for (String mapping : clientViewPaths) {
				if (mapping == null) {
					throw new NullPointerError("null client view mapping string passed to Stream.newStream");
				}
				clientView.add(new ClientView.ClientViewMapping(i, mapping));
				i++;
			}
		}

		IOptions streamOptions = new Options();
		if (options != null) {
			streamOptions = new Options(options);
		}

		if (parentStreamPath == null) {
			parentStreamPath = "none";
		}

		if (name == null) {
			int idx = streamPath.lastIndexOf("/");
			if (idx != -1 && idx < (streamPath.length() - 1)) {
				name = streamPath.substring(idx + 1);
			}
		}

		return new Stream(
				streamPath,
				Type.fromString(type.toUpperCase(Locale.ENGLISH)),
				parentStreamPath,
				null,
				null,
				name,
				description == null ? Stream.DEFAULT_DESCRIPTION : description,
				server.getUserName(),
				streamOptions,
				streamView,
				remappedView,
				ignoredView);
	}

	/**
	 * Simple default implementation of the IStreamViewMapping interface.
	 */
	public static class StreamViewMapping extends MapEntry implements
			IStreamViewMapping {

		protected PathType pathType = null;

		/**
		 * Default constructor -- calls super() only.
		 */
		public StreamViewMapping() {
			super();
		}

		/**
		 * Explicit value constructor -- calls super(order, target, targetSpec).
		 */
		public StreamViewMapping(int order, PathType pathType, String viewPath,
				String depotPath) {
			super(order, viewPath, depotPath);
			if (pathType == null) {
				throw new NullPointerError("null stream view path type passed to Stream.StreamViewMapping constructor.");
			}
			this.pathType = pathType;
		}

		/**
		 * Construct a mapping from the passed-in string, which is assumed to be
		 * in the format.
		 */
		public StreamViewMapping(int order, String viewString) {
			this.order = order;
			if (viewString != null) {
				// The first part of the stream path should be the type
				int idx = viewString.indexOf(" ");
				if (idx != -1) {
					this.pathType = PathType.fromString(viewString.substring(0, idx));
					// Remove the type part from the original path
					viewString = viewString.substring(idx + 1);
				}
				String[] entries = parseViewMappingString(viewString);
				this.type = EntryType.fromString(entries[0]);
				this.left = stripTypePrefix(entries[0]);
				this.right = entries[1];
			}
		}

		/**
		 * @see com.perforce.p4java.core.IStreamViewMapping#getPathType()
		 */
		public PathType getPathType() {
			return this.pathType;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamViewMapping#setPathType(java.lang.String)
		 */
		public void setPathType(PathType pathType) {
			this.pathType = pathType;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamViewMapping#getViewPath()
		 */
		public String getViewPath() {
			return this.left;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamViewMapping#setViewPath(java.lang.String)
		 */
		public void setViewPath(String viewPath) {
			this.left = viewPath;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamViewMapping#getDepotPath()
		 */
		public String getDepotPath() {
			return this.right;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamViewMapping#setDepotPath(java.lang.String)
		 */
		public void setDepotPath(String depotPath) {
			this.right = depotPath;
		}
	}

	/**
	 * Simple default implementation of the IStreamRemappedMapping interface.
	 */
	public static class StreamRemappedMapping extends MapEntry implements
			IStreamRemappedMapping {

		/**
		 * Default constructor -- calls super() only.
		 */
		public StreamRemappedMapping() {
			super();
		}

		/**
		 * Explicit value constructor -- calls super(order, target, targetSpec).
		 */
		public StreamRemappedMapping(int order, String leftRemapPath,
				String rightRemapPath) {
			super(order, leftRemapPath, rightRemapPath);
		}

		/**
		 * Construct a mapping from the passed-in string, which is assumed to be
		 * in the format described in MapEntry.parseViewString(String).
		 */
		public StreamRemappedMapping(int order, String viewString) {
			super(order, viewString);
		}

		/**
		 * @see com.perforce.p4java.core.IStreamRemappedMapping#getLeftRemapPath()
		 */
		public String getLeftRemapPath() {
			return this.left;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamRemappedMapping#setLeftRemapPath(java.lang.String)
		 */
		public void setLeftRemapPath(String leftRemapPath) {
			this.left = leftRemapPath;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamRemappedMapping#getRightRemapPath()
		 */
		public String getRightRemapPath() {
			return this.right;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamRemappedMapping#setRightRemapPath(java.lang.String)
		 */
		public void setRightRemapPath(String rightRemapPath) {
			this.right = rightRemapPath;
		}
	}

	/**
	 * Simple default implementation of the IStreamIgnoredMapping interface.
	 */
	public static class StreamIgnoredMapping extends MapEntry implements
			IStreamIgnoredMapping {

		protected PathType pathType = PathType.SHARE;

		/**
		 * Default constructor -- calls super() only.
		 */
		public StreamIgnoredMapping() {
			super();
		}

		/**
		 * Explicit value constructor -- calls super(order, target, targetSpec).
		 */
		public StreamIgnoredMapping(int order, String ignorePath) {
			super(order, ignorePath, null);
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIgnoredMapping#getIgnorePath()
		 */
		public String getIgnorePath() {
			return this.left;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIgnoredMapping#setIgnorePath(java.lang.String)
		 */
		public void setIgnorePath(String ignorePath) {
			this.left = ignorePath;
		}
	}

	/**
	 * Default constructor. All fields set to null or false.
	 */
	public Stream() {
		super();
	}

	/**
	 * Construct a new Stream from explicit field values.
	 */
	public Stream(String stream, Type type, String parent, Date accessed,
			Date updated, String name, String description, String ownerName,
			IOptions options, ViewMap<IStreamViewMapping> streamView,
			ViewMap<IStreamRemappedMapping> remappedView,
			ViewMap<IStreamIgnoredMapping> ignoredView) {

		this(stream, type, parent, accessed, updated, name, description,
				ownerName, options, streamView, remappedView, ignoredView, null);
	}

	/**
	 * Construct a new Stream from explicit field values.
	 */
	public Stream(String stream, Type type, String parent, Date accessed,
			Date updated, String name, String description, String ownerName,
			IOptions options, ViewMap<IStreamViewMapping> streamView,
			ViewMap<IStreamRemappedMapping> remappedView,
			ViewMap<IStreamIgnoredMapping> ignoredView,
			ViewMap<IClientViewMapping> clientView) {
		this.stream = stream;
		this.type = type;
		this.parent = parent;
		this.accessed = accessed;
		this.updated = updated;
		this.name = name;
		this.ownerName = ownerName;
		this.description = description;
		this.options = options;
		this.streamView = streamView;
		this.remappedView = remappedView;
		this.ignoredView = ignoredView;
		this.clientView = clientView;
	}

	/**
	 * Construct a Stream from a map passed back from the Perforce server in
	 * response to a getStream command.
	 */
	public Stream(Map<String, Object> map, IOptionsServer server) {
		super(map, false);

		this.server = server;
		this.streamView = new ViewMap<IStreamViewMapping>();
		this.remappedView = new ViewMap<IStreamRemappedMapping>();
		this.ignoredView = new ViewMap<IStreamIgnoredMapping>();
		this.clientView = new ViewMap<IClientViewMapping>();
		this.extraTags = new ArrayList<IExtraTag>();

		if (map != null) {
			String key = MapKeys.PATHS_KEY;
			for (int i = 0;; i++) {
				if (!map.containsKey(key + i)) {
					break;
				} else if (map.get(key + i) != null) {
					try {
						PathType type = null;
						String path = (String)map.get(key + i);
						
						// The first part of the stream path should be the type
						int idx = path.indexOf(" ");
						if (idx != -1) {
							type = PathType.fromString(path.substring(0, idx));
							// Remove the type part from the original path
							path = path.substring(idx + 1);
						}
						String[] matchStrs = MapEntry
								.parseViewMappingString(path);

						this.streamView.getEntryList().add(
								new StreamViewMapping(i, type,
										matchStrs[0], matchStrs[1]));

					} catch (Throwable thr) {
						Log.error("Unexpected exception in Stream map-based constructor: "
								+ thr.getLocalizedMessage());
						Log.exception(thr);
					}
				}
			}
			key = MapKeys.REMAPPED_KEY;
			for (int i = 0;; i++) {
				if (!map.containsKey(key + i)) {
					break;
				} else if (map.get(key + i) != null) {
					try {
						String path = (String)map.get(key + i);
						String[] matchStrs = MapEntry
								.parseViewMappingString(path);

						this.remappedView.getEntryList().add(
								new StreamRemappedMapping(i, matchStrs[0],
										matchStrs[1]));

					} catch (Throwable thr) {
						Log.error("Unexpected exception in Stream map-based constructor: "
								+ thr.getLocalizedMessage());
						Log.exception(thr);
					}
				}
			}
			key = MapKeys.IGNORED_KEY;
			for (int i = 0;; i++) {
				if (!map.containsKey(key + i)) {
					break;
				} else if (map.get(key + i) != null) {
					try {
						String path = (String)map.get(key + i);
						this.ignoredView.getEntryList().add(
								new StreamIgnoredMapping(i, path));

					} catch (Throwable thr) {
						Log.error("Unexpected exception in Stream map-based constructor: "
								+ thr.getLocalizedMessage());
						Log.exception(thr);
					}
				}
			}
			key = MapKeys.VIEW_KEY;
			for (int i = 0;; i++) {
				if (!map.containsKey(key + i)) {
					break;
				} else if (map.get(key + i) != null) {
					try {
						String path = (String)map.get(key + i);
						this.clientView.getEntryList().add(
								new ClientView.ClientViewMapping(i, path));

					} catch (Throwable thr) {
						Log.error("Unexpected exception in Stream map-based constructor: "
								+ thr.getLocalizedMessage());
						Log.exception(thr);
					}
				}
			}
			key = MapKeys.EXTRATAG_KEY;
			for (int i = 0;; i++) {
				if (!map.containsKey(key + i)) {
					break;
				} else if (map.get(key + i) != null) {
					try {
						String tagName = (String)map.get(key + i);
						String tagType = (String)map.get(MapKeys.EXTRATAGTYPE_KEY + i);
						String tagValue = (String)map.get(tagName);
						this.extraTags.add(new ExtraTag(tagName, tagType, tagValue));

					} catch (Throwable thr) {
						Log.error("Unexpected exception in Stream map-based constructor: "
								+ thr.getLocalizedMessage());
						Log.exception(thr);
					}
				}
			}
		}
	}

	/**
	 * Construct a new Stream from the passed-in summary stream spec. If the
	 * summary is null, this is equivalent to calling the default Stream
	 * constructor; otherwise after name initialization a refresh() is done on
	 * the new (empty) Stream.
	 * 
	 * @throws ConnectionException
	 *             if the Perforce server is unreachable or is not connected.
	 * @throws RequestException
	 *             if the Perforce server encounters an error during its
	 *             processing of the request
	 * @throws AccessException
	 *             if the Perforce server denies access to the caller
	 */

	public Stream(IStreamSummary summary) throws ConnectionException,
			RequestException, AccessException {
		super(false);
		this.streamView = new ViewMap<IStreamViewMapping>();
		if (summary != null) {
			this.setName(summary.getName());

			if (this.getName() != null) {
				this.refresh();
			}
		}
	}

	private void updateFlags() {
	}

	/**
	 * This method will refresh by getting the complete stream model. If this
	 * refresh is successful then this stream will be marked as complete.
	 * 
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException,
			AccessException {
		IOptionsServer refreshServer = (IOptionsServer)this.server;
		String refreshStreamPath = this.stream;
		if (refreshServer != null && refreshStreamPath != null) {
			try {
				IStream refreshedStream = refreshServer.getStream(refreshStreamPath);
				if (refreshedStream != null) {
					this.stream = refreshedStream.getStream();
					this.type = refreshedStream.getType();
					this.parent = refreshedStream.getParent();
					this.accessed = refreshedStream.getAccessed();
					this.updated = refreshedStream.getUpdated();
					this.ownerName = refreshedStream.getOwnerName();
					this.name = refreshedStream.getName();
					this.description = refreshedStream.getDescription();
					this.options = refreshedStream.getOptions();
					this.streamView = refreshedStream.getStreamView();
					this.remappedView = refreshedStream.getRemappedView();
					this.ignoredView = refreshedStream.getIgnoredView();
					this.extraTags = refreshedStream.getExtraTags();
				}
			} catch (P4JavaException exc) {
				throw new RequestException(exc.getMessage(), exc);
			}
		}
		updateFlags();
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	public void update() throws ConnectionException, RequestException,
			AccessException {
		try {
			((IOptionsServer)this.server).updateStream(this, null);
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update(boolean)
	 */
	public void update(boolean force) throws ConnectionException, RequestException, AccessException {
		try {
			((IOptionsServer)this.server).updateStream(this, new StreamOptions().setForceUpdate(force));
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * @see com.perforce.p4java.core.IStream#getStreamView()
	 */
	public ViewMap<IStreamViewMapping> getStreamView() {
		return this.streamView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#setStreamView(com.perforce.p4java.core.ViewMap)
	 */
	public void setStreamView(ViewMap<IStreamViewMapping> streamView) {
		this.streamView = streamView;
	}

	/**
	 * @see com.perforce.p4java.core.IServerResource#setServer(com.perforce.p4java.server.IServer)
	 */
	public void setServer(IOptionsServer server) {
		this.server = server;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#getRemappedView()
	 */
	public ViewMap<IStreamRemappedMapping> getRemappedView() {
		return this.remappedView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#setStreamView(com.perforce.p4java.core.ViewMap)
	 */
	public void setRemappedView(ViewMap<IStreamRemappedMapping> remappedView) {
		this.remappedView = remappedView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#getIgnoredView()
	 */
	public ViewMap<IStreamIgnoredMapping> getIgnoredView() {
		return this.ignoredView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#setIgnoredView(com.perforce.p4java.core.ViewMap)
	 */
	public void setIgnoredView(ViewMap<IStreamIgnoredMapping> ignoredView) {
		this.ignoredView = ignoredView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#getClientView()
	 */
	public ViewMap<IClientViewMapping> getClientView() {
		return this.clientView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#setClientView(com.perforce.p4java.core.ViewMap)
	 */
	public void setClientView(ViewMap<IClientViewMapping> clientView) {
		this.clientView = clientView;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#getExtraTags()
	 */
	public List<IExtraTag> getExtraTags() {
		return this.extraTags;
	}

	/**
	 * @see com.perforce.p4java.core.IStream#setExtraTags(java.util.List)
	 */
	public void setExtraTags(List<IExtraTag> extraTags) {
		this.extraTags = extraTags;
	}
}
