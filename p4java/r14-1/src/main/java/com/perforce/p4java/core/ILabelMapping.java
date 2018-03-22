/**
 * 
 */
package com.perforce.p4java.core;

/**
 * Extends IMapEntry to provide a Perforce label-specific
 * view map entry type.<p>
 * 
 * Perforce label views define only the left side of the mapping as
 * significant, so we only supply that here, but users are quite
 * free to use the superclass right side methods to do what they
 * want with...
 */

public interface ILabelMapping extends IMapEntry {
	
	/**
	 * Get a label view entry's "source" spec; this corresponds
	 * to the left entry of the associated mapping.
	 */
	String getViewMapping();
	
	/**
	 * Set a branch view entry's "source" spec; this corresponds
	 * to the left entry of the associated mapping.
	 */
	void setViewMapping(String entry);
}
