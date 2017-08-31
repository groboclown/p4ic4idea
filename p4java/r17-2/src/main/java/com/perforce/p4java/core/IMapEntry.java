/**
 * 
 */
package com.perforce.p4java.core;

/**
 * Defines an individual view map entry. These entries map
 * "left" values to "right" values, where the semantics and
 * usage of "left" and "right" depends on the specific type of view
 * (e.g. left may be "depot", right "client").<p>
 * 
 * Note that the left and right strings are pure paths -- they do
 * not contain (or should not contain) any leading include / exclude
 * prefixes except where this is explicitly allowed (e.g. the special
 * constructor).
 */

public interface IMapEntry {
	
	final int ORDER_UNKNOWN = -1;
	final String EXCLUDE_PREFIX = "-";
	final String OVERLAY_PREFIX = "+";
	
	/**
	 * Defines the specific type of a given view map entry.
	 */
	
	public enum EntryType {
		/**
		 * Specifies this is an "include" mapping; this means that
		 * the map includes this path and its children.
		 */
		INCLUDE,
		
		/**
		 * Specifies this is an "exclude" mapping; this means that
		 * the map excludes this path and its children.
		 */
		EXCLUDE,
		
		/**
		 * Specifies this is an "overlay" mapping; this means that
		 * the map overlays this path and its children.
		 */
		OVERLAY;
		
		/**
		 * Return a suitable EntryType as inferred from the passed-in
		 * string, which is assumed to be a Perforce view map path string.
		 * If str is null, or no such EntryType can be inferred,
		 * returns null.
		 */
		public static EntryType fromString(String str) {
			if (str != null) {
				if (str.startsWith(IMapEntry.EXCLUDE_PREFIX)) {
					return EXCLUDE;
				} else if (str.startsWith(IMapEntry.OVERLAY_PREFIX)) {
					return OVERLAY;
				} else {
					return INCLUDE;
				}
			}
			return null;
		}
		
		/**
		 * Return a more useful string than "EXCLUDE" or "OVERLAY", i.e.
		 * return "-" or "+" respectively. Returns the empty string
		 * (not null) if the type is neither EXCLUDE nor OVERLAY.
		 */
		public String toString() {
			switch (this) {
				case EXCLUDE:
					return EXCLUDE_PREFIX;
				case OVERLAY:
					return OVERLAY_PREFIX;
			}
			return "";
		}
	};
	
	/**
	 * Get the order of this entry in the entry list, if known. Returns
	 * ORDER_UNKNOWN if the order is unknown or this entry is not currently
	 * associated with a map.
	 * 
	 * @return ORDER_UNKNOWN or current order.
	 */
	int getOrder();

	/**
	 * Set the order of this entry in the entry list. Note that this method
	 * has no effect on the actual order within an entry whatsoever, and is
	 * provided for symmetry and for implementation initialization reasons only.
	 * 
	 * @param position new order
	 */
	void setOrder(int position);
	
	/**
	 * Return the view map type of this entry.
	 * 
	 * @return possibly-null EntryType
	 */
    EntryType getType();
    
    /**
     * Set this entry's type.
     * 
     * @param type new entry type. May be null.
     */
    void setType(EntryType type);
    
    /**
     * Get the "left" entry for this mapping; equivalent to
     * getLeft(false).
     * 
     * @return possibly-null left mapping entry.
     */
    String getLeft();
    
    /**
     * Get the "left" entry for this mapping. Will not include
     * any prefixes. If quoteBlanks is true and the left string
     * contains spaces or tabs the entire string is returned
     * surrounded by quote characters.
     * 
     * @param quoteBlanks if true, and the left string
     * 			contains spaces or tabs the entire string is returned
     * 			surrounded by quote characters.
     * @return possibly-null left mapping entry.
     */
    String getLeft(boolean quoteBlanks);
    
    /**
     * Set the "left" entry for this mapping. Will strip off
     * any exclude (etc.) prefix before assigning it.
     * 
     * @param left possibly-null new left mapping entry
     */
    void setLeft(String left);
    
    /**
     * Get the "right" entry for this mapping; equivalent to
     * getRight(false).
     * 
     * @return possibly-null right mapping entry.
     */
    String getRight();
    
    /**
     * Get the "right" entry for this mapping. Will not include
     * any prefixes. If quoteBlanks is true and the right string
     * contains spaces or tabs the entire string is returned
     * surrounded by quote characters.
     * 
     * @param quoteBlanks if true, and the right string
     * 			contains spaces or tabs the entire string is returned
     * 			surrounded by quote characters.
     * @return possibly-null right mapping entry.
     */
    String getRight(boolean quoteBlanks);
    
    /**
     * Set the "right" entry for this mapping. Will strip off
     * any exclude (etc.) prefix before assigning it.
     * 
     * @param right possibly-null new right mapping entry
     */
    void setRight(String right);
    
    /**
     * Alias for toString(" ", false).
     */
    String toString();
    
    /**
     * Return a canonical String representation of this entry. This
     * is in the form [type prefix]leftpath[specstring]rightpath, e.g.
     * "-//depot/dev/test/... //clientname/newpath/..."<p>
     * 
     * If the passed-in string is null, the left and right strings
     * (if they exist) will be concatenated into one long separator-less
     * string.<p>
     * 
     * If the quoteBlanks parameter is true, if either or both the left
     * or right entries contain spaces, the entries are quoted in full, i.e.
     * the mapping //depot/test/space test 01/... //depot/test/space test 02/...
     * becomes "//depot/test/space test 01/..." "//depot/test/space test 02/...".
     */
    String toString(String sepString, boolean quoteBlanks);
}
