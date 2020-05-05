/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import com.perforce.p4java.Log;
import com.perforce.p4java.admin.IProperty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Default implementation of the IProperty interface.
 */
public class Property implements IProperty {

	public static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
	
	private String name = null;
	private String sequence = null;
	private String value = null;
	private long time = 0;
	private Date modified = null;
	private String modifiedBy = null;

	/**
	 * Instantiates a new property.
	 * 
	 * @param name
	 *            the property name
	 * @param sequence
	 *            the sequence number
	 * @param value
	 *            the property value
	 * @param time
	 *            the time in milliseconds
	 * @param modified
	 *            the modified date
	 * @param modifiedBy
	 *            the modified by user
	 */
	public Property(String name, String sequence, String value,
			long time, Date modified, String modifiedBy) {

		this.name = name;
		this.sequence = sequence;
		this.value = value;
		this.time = time;
		this.modified = modified;
		this.modifiedBy = modifiedBy;
	}

	/**
	 * Constructs a Property from the passed-in map; this map must have come
	 * from a Perforce IServer method call or it may fail. If map is null,
	 * equivalent to calling the default constructor.
	 * 
	 * @param map
	 *            the map
	 */
	public Property(Map<String, Object> map) {
		if (map != null) {
			try {
				if (map.containsKey("name")) {
					this.name = (String) map.get("name");
				}
				if (map.containsKey("sequence")) {
					this.sequence = (String) map.get("sequence");
				}
				if (map.containsKey("value")) {
					this.value = (String) map.get("value");
				}
				if (map.containsKey("time")) {
					this.time = new Long((String) map.get("time"));
				}
				if (map.containsKey("modified")) {
		            DateFormat df = new SimpleDateFormat(DATE_PATTERN);
		            this.modified = df.parse((String) map.get("modified"));
				}
				if (map.containsKey("modifiedBy")) {
					this.modifiedBy = (String) map.get("modifiedBy");
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}
		}
	}

	/**
	 * Gets the name of the property.
	 * 
	 * @return the name
	 * @see com.perforce.p4java.admin.IProperty#getName()
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the sequence number of the property.
	 * 
	 * @see com.perforce.p4java.admin.IProperty#getSequence()
	 * @return the sequence number
	 */
	public String getSequence() {
		return this.sequence;
	}

	/**
	 * Gets the value of the property.
	 * 
	 * @see com.perforce.p4java.admin.IProperty#getValue()
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Gets the time in milliseconds.
	 * 
	 * @see com.perforce.p4java.admin.IProperty#getTime()
	 * @return the time in milliseconds
	 */
	public long getTime() {
		return this.time;
	}

	/**
	 * Gets the modified date.
	 * 
	 * @see com.perforce.p4java.admin.IProperty#getModified()
	 * @return the modified date
	 */
	public Date getModified() {
		return this.modified;
	}
	
	/**
	 * Gets the modified by user.
	 * 
	 * @see com.perforce.p4java.admin.IProperty#getModifiedBy()
	 * @return the modified by user
	 */
	public String getModifiedBy() {
		return this.modifiedBy;
	}
}
