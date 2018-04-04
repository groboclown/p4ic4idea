/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

import java.util.Date;

/**
 * Provides storage of property values for use by applications that wish to
 * persistently store their configuration settings and other property data in
 * the server.
 */
public interface IProperty {

	/**
	 * Gets the name of the property.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Gets the sequence number of the property.
	 * 
	 * @return the sequence number
	 */
	String getSequence();

	/**
	 * Gets the value of the property.
	 * 
	 * @return the value
	 */
	String getValue();

	/**
	 * Gets the time in milliseconds.
	 * 
	 * @return the time in milliseconds
	 */
	long getTime();

	/**
	 * Gets the modified date.
	 * 
	 * @return the modified date
	 */
	Date getModified();
	
	/**
	 * Gets the modified by user.
	 * 
	 * @return the modified by user
	 */
	String getModifiedBy();
}
