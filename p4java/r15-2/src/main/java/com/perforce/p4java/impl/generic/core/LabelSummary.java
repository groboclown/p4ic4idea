/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Default implementation of the ILabelSumamry interface.
 */
public class LabelSummary extends ServerResource implements ILabelSummary {
	
	protected static final String LOCKED_VALUE = "locked";
	protected static final String UNLOCKED_VALUE = "unlocked";
	protected static final String AUTORELOAD_VALUE = "autoreload";
	protected static final String NOAUTORELOAD_VALUE = "noautoreload";
	
	protected String name = null;
	protected String ownerName = null;
	protected Date lastAccess = null;
	protected Date lastUpdate = null;
	protected String description = null;
	protected String revisionSpec = null;
	protected boolean locked = false;
	protected boolean unloaded = false;
	protected boolean autoreload = false;
	
	/**
	 * Default constructor -- set all fields to null or false.
	 */
	public LabelSummary() {
		super(false, false);
	}
	
	
	/**
	 * Construct an empty LabelSummary and appropriately initialize
	 * the ServerResource superclass fields according to whether this
	 * summary a summary only or part of the full Label class.
	 */
	public LabelSummary(boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);
	}
	
	/**
	 * Construct a LabelSummary from a map returned from the Perforce server's
	 * getLabelSummaryList.<p>
	 * 
	 * If the map is null, this is equivalent to calling the default constructor.
	 */
	public LabelSummary(Map<String, Object> map) {
		super(false, false);
		
		if (map != null) {
			try {
				this.name = (String) map.get(MapKeys.LABEL_LC_KEY);
				this.description = (String) map.get(MapKeys.DESCRIPTION_KEY);
				if (this.description != null) {
					this.description = this.description.trim();
				}
				this.ownerName = (String) map.get(MapKeys.OWNER_KEY);
				this.lastUpdate = new Date(Long.parseLong((String) map
						.get(MapKeys.UPDATE_KEY)) * 1000);
				this.lastAccess = new Date(Long.parseLong((String) map
						.get(MapKeys.ACCESS_KEY)) * 1000);
				this.revisionSpec = (String) map.get(MapKeys.REVISION_KEY);

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
				if (map.get("IsUnloaded") != null
						&& ((String) map.get("IsUnloaded")).equals("1")) {
					this.unloaded = true;
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception in LabelSummary constructor: "
								+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#getOwnerName()
	 */
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setOwnerName(java.lang.String)
	 */
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#getLastAccess()
	 */
	public Date getLastAccess() {
		return lastAccess;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setLastAccess(java.util.Date)
	 */
	public void setLastAccess(Date lastAccess) {
		this.lastAccess = lastAccess;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#getLastUpdate()
	 */
	public Date getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setLastUpdate(java.util.Date)
	 */
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#getRevisionSpec()
	 */
	public String getRevisionSpec() {
		return revisionSpec;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setRevisionSpec(java.lang.String)
	 */
	public void setRevisionSpec(String revisionSpec) {
		this.revisionSpec = revisionSpec;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#isLocked()
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setLocked(boolean)
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#isAutoReload()
	 */
	public boolean isAutoReload() {
		return autoreload;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#setAutoReload(boolean)
	 */
	public void setAutoReload(boolean autoreload) {
		this.autoreload = autoreload;
	}

	/**
	 * @see com.perforce.p4java.core.ILabelSummary#isUnloaded()
	 */
	public boolean isUnloaded() {
		return unloaded;
	}
}
