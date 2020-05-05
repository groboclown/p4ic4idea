/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

import java.util.List;

/**
 * Describes a Perforce protections table.
 */
public interface IProtectionsTable {
	
	/**
	 * Gets the list of protection entries.
	 * 
	 * @return the list of protection entries
	 */
	List<IProtectionEntry> getEntries();
	
	/**
	 * Sets the list of protection entries.
	 * 
	 * @param entries list of protection entries
	 */
	void setEntries(List<IProtectionEntry> entries);
}
