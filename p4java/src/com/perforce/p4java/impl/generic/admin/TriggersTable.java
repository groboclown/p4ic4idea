/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import java.util.List;

import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.admin.ITriggersTable;

/**
 * Default ITriggersTable implementation class.
 */
public class TriggersTable implements ITriggersTable {

	/**
	 * List of protection entries
	 */
	private List<ITriggerEntry> entries = null;

	/**
	 * Default constructor.
	 */
	public TriggersTable() {
	}

	/**
	 * Explicit-value constructor.
	 */
	public TriggersTable(List<ITriggerEntry> entries) {
		this.entries = entries;
	}
	
	/**
	 * @see com.perforce.p4java.admin.ITriggersTable#getEntries()
	 */
	public List<ITriggerEntry> getEntries() {
		return this.entries;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggersTable#setEntries(java.util.List)
	 */
	public void setEntries(List<ITriggerEntry> entries) {
		this.entries = entries;
	}
}
