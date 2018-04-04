/**
 * 
 */
package com.perforce.p4java.client;

import com.perforce.p4java.core.IMapEntry;

/**
 * Defines an individual Perforce client view mapping between a depot file
 * and a local Perforce client file. Basically just a simple name-translation
 * convenience extension of the basic IMapEntry interface; see that interface
 * for detailed semantics.
 */

public interface IClientViewMapping extends IMapEntry {
	String getDepotSpec();
	String getDepotSpec(boolean quoteBlanks);
	String getClient();
	String getClient(boolean quoteBlanks);
	void setDepotSpec(String depotSpec);
	void setClient(String client);
};
