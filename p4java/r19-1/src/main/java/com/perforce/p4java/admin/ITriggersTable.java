/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

import java.util.List;

/**
 * Describes a Perforce triggers table.
 */
public interface ITriggersTable {
	
	/**
	 * Gets the list of trigger entries.
	 * 
	 * @return the list of trigger entries
	 */
	List<ITriggerEntry> getEntries();
	
	/**
	 * Sets the list of trigger entries.
	 * 
	 * @param entries
	 *            the list of trigger entries
	 */
	void setEntries(List<ITriggerEntry> entries);
}
