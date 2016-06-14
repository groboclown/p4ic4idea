/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Default implementation class for the IBranchSpecSummary interface.
 */

public class BranchSpecSummary extends ServerResource implements IBranchSpecSummary {

	protected Date accessed = null;
	protected Date updated = null;
	protected String name = null;
	protected String ownerName = null;
	protected String description = null;
	protected boolean locked = false;
	
	/**
	 * Default constructor -- sets all fields to null or false.
	 */
	public BranchSpecSummary() {
	}
	
	/**
	 * Default constructor; same as no-argument default constructor,
	 * except that it sets the ServerResource superclass fields appropriately
	 * for summary only (everything false) or full branch spec (updateable and
	 * refreshable).
	 */
	public BranchSpecSummary(boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);
	}
	
	/**
	 * Explicit-value constructor. If summaryOnly is true, refreshable
	 * and updeateable are set true in the ServerResource superclass, otherwise
	 * they're set false.
	 */
	public BranchSpecSummary(boolean summaryOnly, Date accessed,
			Date updated, String name, String ownerName, String description,
			boolean locked) {
		super(!summaryOnly, !summaryOnly);
		this.accessed = accessed;
		this.updated = updated;
		this.name = name;
		this.ownerName = ownerName;
		this.description = description;
		this.locked = locked;
	}
	
	/**
	 * Construct a BranchSpecSummary from a map returned by the Perforce server. If
	 * summaryOnly is true, this map was returned by the IServer getBranchSummaryList
	 * or similar summary-only method; otherwise it's assumed to be the full branch
	 * spec.<p>
	 * 
	 * If map is null, this is equivalent to calling the default summaryOnly-argument
	 * constructor.
	 */
	public BranchSpecSummary(Map<String, Object> map, boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);

		if (map != null) {
			if (summaryOnly) {
				this.name = (String) map.get(MapKeys.BRANCH_LC_KEY);
				this.accessed = new Date(Long.parseLong((String) map.get(MapKeys.ACCESS_KEY)) * 1000);
				this.updated = new Date(Long.parseLong((String) map.get(MapKeys.UPDATE_KEY)) * 1000);
			} else {
				try {
					final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
					this.name = (String) map.get(MapKeys.BRANCH_KEY);
					if (map.containsKey(MapKeys.UPDATE_KEY)) {
						this.updated = new SimpleDateFormat(DATE_FORMAT).parse(
														(String) map.get(MapKeys.UPDATE_KEY));
					}
					if (map.containsKey(MapKeys.ACCESS_KEY)) {
						this.accessed = new SimpleDateFormat(DATE_FORMAT).parse(
														(String) map.get(MapKeys.ACCESS_KEY));
					}
				} catch (Throwable thr) {
					Log.warn("Unexpected exception in BranchSpecSummary constructor: "
							+ thr.getMessage());
					Log.exception(thr);
				}
			}
			
			this.description = (String) map.get(MapKeys.DESCRIPTION_KEY);
			this.ownerName = (String) map.get(MapKeys.OWNER_KEY);
			String opts = (String) map.get(MapKeys.OPTIONS_KEY);
			if ((opts != null) && opts.equalsIgnoreCase("locked")) {
				this.locked = true;
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#getAccessed()
	 */
	public Date getAccessed() {
		return accessed;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#setAccessed(java.util.Date)
	 */
	public void setAccessed(Date accessed) {
		this.accessed = accessed;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#getUpdated()
	 */
	public Date getUpdated() {
		return updated;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#setUpdated(java.util.Date)
	 */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#getName()
	 */
	public String getName() {
		return name;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#getOwnerName()
	 */
	public String getOwnerName() {
		return ownerName;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#setOwnerName(java.lang.String)
	 */
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#getDescription()
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#isLocked()
	 */
	public boolean isLocked() {
		return locked;
	}
	/**
	 * @see com.perforce.p4java.core.IBranchSpecSummary#setLocked(boolean)
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
