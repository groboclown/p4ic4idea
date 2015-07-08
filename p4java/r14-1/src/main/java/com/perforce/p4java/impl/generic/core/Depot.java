/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Simple default implementation class for the IDepot interface.
 * 
 * @version	$Id$
 */

public class Depot extends ServerResource implements IDepot {

	private String name = null;
	private String ownerName = null;
	private Date modDate = null;
	private String description = null;
	private DepotType depotType = null;
	private String address = null;
	private String suffix = null;
	private String map = null;
	
	/**
	 * Default constructor. Sets up default (typically null) metadata values.
	 */
	public Depot() {
		super(false, false);
	}
	
	/**
	 * Explicit-value constructor.
	 */
	public Depot(String name, String ownerName, Date modDate,
			String description, DepotType depotType, String address,
			String suffix, String map) {
		this.name = name;
		this.ownerName = ownerName;
		this.modDate = modDate;
		this.description = description;
		this.depotType = depotType;
		this.address = address;
		this.suffix = suffix;
		this.map = map;
	}

	/**
	 * Construct a Perforce depot object from a suitable map passed back
	 * from the Perforce server as the result of a depot list command.
	 * 
	 * @param map map passed back from the Perforce server as a result of the depot list
	 * 				or depot -o commands; if null, fields will have default values.
	 */
	
	public Depot(Map<String, Object> map) {
		super(false, false);
		if (map != null) {
			
			// Note that the interpretation of map values depends to some extent on
			// the type of the depot. Note also the wildly-annoying way the server sends
			// back map keys with different case depending on whether it's the result of
			// an individual depot listing or all depots... hence all the second guessing
			// going on below.
			
			try {
				this.name = (String) map.get(MapKeys.NAME_LC_KEY);
				if (this.name == null) {
					this.name = (String) map.get(MapKeys.DEPOT_KEY);
				}
				this.ownerName = (String) map.get(MapKeys.OWNER_LC_KEY);
				if (this.ownerName == null) {
					this.ownerName = (String) map.get(MapKeys.OWNER_KEY);
				}
				try {
					if (map.get(MapKeys.TIME_LC_KEY) != null) {
						this.modDate = new Date(new Long((String) map.get(MapKeys.TIME_LC_KEY)));
					} else if (map.get(MapKeys.DATE_KEY) != null) {
						this.modDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse((String) map.get(MapKeys.DATE_KEY));
					}
				} catch (Throwable thr) {
					Log.error("Unexpected exception in Depot constructor: "
							+ thr.getLocalizedMessage());
					Log.exception(thr);
				}
				this.description = (String) map.get(MapKeys.DESC_LC_KEY);
				if (this.description == null) {
					this.description = (String) map.get(MapKeys.DESCRIPTION_KEY);
				}
				if ((this.description != null) && (this.description.length() > 1) && this.description.endsWith("\n")) {
					this.description = this.description.substring(0, this.description.length() - 1);
				}
				if (map.get(MapKeys.TYPE_LC_KEY) != null) {
					this.depotType = DepotType.fromString(((String) map.get(MapKeys.TYPE_LC_KEY)).toUpperCase());
				} else if (map.get(MapKeys.TYPE_KEY) != null) {
					this.depotType = DepotType.fromString(((String) map.get(MapKeys.TYPE_KEY)).toUpperCase());
				}
				switch (this.depotType) {
					case REMOTE:
						this.address = (String) map.get(MapKeys.EXTRA_LC_KEY);
						if (this.address == null) {
							this.address = (String) map.get(MapKeys.ADDRESS_KEY);
						}
						break;
					case SPEC:
						this.suffix = (String) map.get(MapKeys.EXTRA_LC_KEY);
						if (this.suffix == null) {
							this.suffix = (String) map.get(MapKeys.SUFFIX_KEY);
						}
						break;
				}
				this.map = (String) map.get(MapKeys.MAP_LC_KEY);
				if (this.map == null) {
					this.map = (String) map.get(MapKeys.MAP_KEY);
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception in Depot constructor: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOwnerName() {
		return ownerName;
	}
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	public Date getModDate() {
		return modDate;
	}
	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public DepotType getDepotType() {
		return depotType;
	}
	public void setDepotType(DepotType depotType) {
		this.depotType = depotType;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	public String getMap() {
		return map;
	}
	public void setMap(String map) {
		this.map = map;
	}
}
