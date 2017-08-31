/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

/**
 * Define (optional) a list of file or directory names to be ignored in client
 * views. For example:
 * <p>
 * 
 * /tmp # ignores files named 'tmp'
 * <p>
 * /tmp/... # ignores dirs named 'tmp'
 * <p>
 * .tmp # ignores file names ending in '.tmp'
 * <p>
 * 
 * Mappings in the "Ignored" field may appear in any order. Ignored names are
 * inherited by child stream client views.
 */

public interface IStreamIgnoredMapping extends IMapEntry {

	/**
	 * Get a stream ignored entry's file path; this corresponds to the left
	 * entry of the associated mapping.
	 */
	String getIgnorePath();

	/**
	 * Set a stream ignored entry's left file path; this corresponds to the left
	 * entry of the associated mapping.
	 */
	void setIgnorePath(String ignorePath);
};
