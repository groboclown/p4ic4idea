/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.server.IServer;

/**
 * Default implementation class for the ILabel interface.
 */

public class Label extends LabelSummary implements ILabel {

	private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";	
	protected ViewMap<ILabelMapping> viewMapping = null;
	
	/**
	 * The description string used if no description field is passed to
	 * Label.newLabel().
	 */
	public static final String DEFAULT_DESCRIPTION = "New label created by P4Java";
	
	/**
	 * The default mapping used if a null mapping parameter is passed to
	 * Label.newLabel().
	 */
	public static final String DEFAULT_MAPPING = "//depot/...";
	
	/**
	 * Create a new local Label object with the given name, description, and
	 * view mapping. The new object is local only (i.e. it does not exist as 
	 * a "real" Perforce label on the server) and is not locked, has no
	 * revision spec, and has its owner name field set to the current user.
	 * Other defaults are as given for the Label and LabelSummary default constructors.
	 * 
	 * @param server non-null server to be associated with this label.
	 * @param name non-null label name.
	 * @param description if not null, the new label's description field; if null,
	 * 				Label.DEFAULT_DESCRIPTION is used instead.
	 * @param mapping if not null, defines the left hand sides of the label's
	 * 				view map; if null, defaults to a single mapping as defined
	 * 				in Label.DEFAULT_MAPPING.
	 * @return new local Label object.
	 */
	
	public static Label newLabel(IServer server, String name, String description,
													String[] mapping) {		
		ViewMap<ILabelMapping> viewMapping = new ViewMap<ILabelMapping>();
		
		if (server == null) {
			throw new NullPointerError("null server in Label,newLabel()");
		}
		if (name == null) {
			throw new NullPointerError("null label name in Label.newLabel()");
		}
		
		if (mapping == null) {
			mapping = new String[] {DEFAULT_MAPPING};
		}
		
		for (String map : mapping) {
			if (map == null) {
				throw new NullPointerError("null mapping element in Label.newLabel()");
			}
			Label.LabelMapping entry = new Label.LabelMapping();
			entry.setLeft(map);
			viewMapping.addEntry(entry);
		}
		
		Label label = new Label(
					name,
					server.getUserName(),
					null,
					null,
					description == null ? DEFAULT_DESCRIPTION : description,
					null,
					false,
					viewMapping
				);
		
		// Make sure to attach the IServer to the label
		label.setServer(server);
		
		return label;
	}
	
	public static class LabelMapping extends MapEntry implements ILabelMapping {
		
		/**
		 * Default constructor -- calls super() only.
		 */
		public LabelMapping() {
			super();
		}
		
		/**
		 * Explicit value constructor -- calls super(order, labelMapping).
		 * Note that this probably won't do what you expect it to if
		 * there's more than one element in the subscription.
		 */
		public LabelMapping(int order, String labelMapping) {
			super(order, labelMapping);
		}
		
		/**
		 * @see com.perforce.p4java.core.ILabelMapping#getViewMapping()
		 */
		public String getViewMapping() {
			return this.left;
		}

		/**
		 * @see com.perforce.p4java.core.ILabelMapping#setViewMapping(java.lang.String)
		 */
		public void setViewMapping(String entry) {
			this.left = entry;
		}
	};
	
	/**
	 * Default constructor; sets all inherited and local fields to null or false;
	 * calls super(false).
	 */
	public Label() {
		super(false);
	}
	
	/**
	 * Explicit-value constructor. Generally useful for constructing new
	 * label implementations.
	 */
	
	public Label(String name, String ownerName, Date lastAccess,
				Date lastUpdate, String description, String revisionSpec,
				boolean locked, ViewMap<ILabelMapping> viewMapping) {
		super(false);
		this.name = name;
		this.ownerName = ownerName;
		this.lastAccess = lastAccess;
		this.lastUpdate = lastUpdate;
		this.description = description;
		this.revisionSpec = revisionSpec;
		this.locked = locked;
		this.viewMapping = viewMapping;
	}
	
	/**
	 * Construct a new Label from the map passed back from the
	 * IServer's getLabel method or from a similar map, and the current
	 * server object (if any). Will not work properly  with the map returned
	 * from the server getLabelSummaryList method.<p>
	 * 
	 * If the map is null, this is equivalent to calling the default constructor.
	 */
	
	public Label(Map<String, Object> map, IServer server) {
		super(false);

		this.server = server;		

		try {
			this.name = (String) map.get(MapKeys.LABEL_KEY);
			this.description = (String) map.get(MapKeys.DESCRIPTION_KEY);
			if (this.description != null) {
				this.description = this.description.trim();
			}
			this.ownerName = (String) map.get(MapKeys.OWNER_KEY);
			this.revisionSpec = (String) map.get(MapKeys.REVISION_KEY);

			try {
				if (map.containsKey(MapKeys.UPDATE_KEY)) {
					this.lastUpdate = new SimpleDateFormat(DATE_FORMAT)
							.parse((String) map.get(MapKeys.UPDATE_KEY));
				}
				if (map.containsKey(MapKeys.ACCESS_KEY)) {
					this.lastAccess = new SimpleDateFormat(DATE_FORMAT)
							.parse((String) map.get(MapKeys.ACCESS_KEY));
				}
			} catch (ParseException pe) {
				Log
						.error("Date parse error in Label constructor: "
								+ pe.getLocalizedMessage());
				Log.exception(pe);
			}

			String optStr = (String) map.get(MapKeys.OPTIONS_KEY);
			if (optStr != null) {
				String[] optParts = optStr.split("\\s+");
				if (optParts != null && optParts.length > 0) {
					for (String optPart : optParts) {
						if (optPart.equalsIgnoreCase(LOCKED_VALUE)) {
							this.locked = true;
						} else if (optPart.equalsIgnoreCase(UNLOCKED_VALUE)) {
							this.locked = false;
						} else if (optPart.equalsIgnoreCase(AUTORELOAD_VALUE)) {
							this.autoreload = true;
						} else if (optPart.equalsIgnoreCase(NOAUTORELOAD_VALUE)) {
							this.autoreload = false;
						}
					}
				}
			}
			
			// Note: only the left (depot) side is given for label views

			this.viewMapping = new ViewMap<ILabelMapping>();

			for (int i = 0;; i++) {
				String mappingStr = (String) map.get(MapKeys.VIEW_KEY
						+ i);

				if (mappingStr == null) {
					break;
				} else {
					String[] parts = MapEntry.parseViewMappingString(mappingStr);
					this.viewMapping.getEntryList().add(new LabelMapping(i, parts[0]));
				}
			}

		} catch (Throwable thr) {
			Log.error("Unexpected exception in Label constructor: "
							+ thr.getLocalizedMessage());
			Log.exception(thr);
		}
	}
	
	/**
	 * Given an ILabelSummary object, construct a new Label object from it. This
	 * implementation simply gets the label on the Perforce server with the same
	 * name as the labelSummary's name by using the Label.refresh() object.<p>
	 * 
	 * If labelSummary is null this is equivalent to calling the default constructor;
	 * otherwise all LabelSummary fields are copied, and if labelSummary.getName() is
	 * not null, the refresh() is performed.
	 * 
	 * @param labelSummary possibly-null ILabelSummary object.
	 */
	
	public Label(ILabelSummary labelSummary)
							throws ConnectionException, RequestException, AccessException {
		super(false);
		this.viewMapping = new ViewMap<ILabelMapping>();
		if (labelSummary != null) {
			this.name = labelSummary.getName();
			this.ownerName = labelSummary.getOwnerName();
			this.lastAccess = labelSummary.getLastAccess();
			this.lastUpdate = labelSummary.getLastUpdate();
			this.description = labelSummary.getDescription();
			this.revisionSpec = labelSummary.getRevisionSpec();
			this.locked = labelSummary.isLocked();
			
			if (this.name != null) {
				this.refresh();
			}
		}
	}

	/**
	 * This method will refresh by getting the complete label model. If this
	 * refresh is successful then this label will be marked as complete.
	 * 
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException,
			AccessException {
		IServer refreshServer = this.server;
		String refreshName = this.name;
		if (refreshServer != null && refreshName != null) {
			ILabel refreshedLabel = refreshServer.getLabel(refreshName);
			if (refreshedLabel != null) {
				this.description = refreshedLabel.getDescription();
				this.ownerName = refreshedLabel.getOwnerName();
				this.revisionSpec = refreshedLabel.getRevisionSpec();
				this.lastUpdate = refreshedLabel.getLastUpdate();
				this.lastAccess = refreshedLabel.getLastAccess();
				this.locked = refreshedLabel.isLocked();
				this.viewMapping = refreshedLabel.getViewMapping();
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.ILabel#updateOnServer()
	 */
	public String updateOnServer()
						throws ConnectionException, RequestException, AccessException {
		
		if (this.server == null) {
			throw new RequestException("label not associated with any Perforce server");
		}
		
		return this.server.updateLabel(this);
	}
	
	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	public void update() 
						throws ConnectionException, RequestException, AccessException {
		this.server.updateLabel(this);
	}

	/**
	 * @see com.perforce.p4java.core.ILabel#setViewMapping(com.perforce.p4java.core.ViewMap)
	 */
	public void setViewMapping(ViewMap<ILabelMapping> viewMapping) {
		this.viewMapping = viewMapping;
	}
	
	/**
	 * @see com.perforce.p4java.core.ILabel#getViewMapping()
	 */
	public ViewMap<ILabelMapping> getViewMapping() {
		return this.viewMapping;
	}

	/**
	 * @see com.perforce.p4java.core.ILabel#getServer()
	 */
	public IServer getServer() {
		return this.server;
	}

	/**
	 * @see com.perforce.p4java.core.ILabel#setServer(com.perforce.p4java.server.IServer)
	 */
	public void setServer(IServer server) {
		this.server = server;
	}
}
