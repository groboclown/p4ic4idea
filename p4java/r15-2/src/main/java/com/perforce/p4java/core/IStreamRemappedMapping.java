/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

/**
 * Define (optional) an individual stream view path is to be remapped in client
 * view. Each remapped mapping is of the form:
 * <p>
 * 
 * <view_path_1> <view_path_2>
 * <p>
 * 
 * where <view_path_1> and <view_path_2> are Perforce view paths with no leading
 * slashes and no leading or embedded wildcards. For example:
 * <p>
 * 
 * ... x/...
 * <p>
 * y/* y/z/*
 * <p>
 * 
 * Line ordering in the Remapped field is significant; if more than one line
 * remaps the same files, the later line has precedence. Remapping is inherited
 * by child stream client views.
 */

public interface IStreamRemappedMapping extends IMapEntry {

	/**
	 * Get a stream remapped entry's left remap path; this corresponds to the
	 * left entry of the associated mapping.
	 */
	String getLeftRemapPath();

	/**
	 * Set a stream remapped entry's left remap path; this corresponds to the
	 * left entry of the associated mapping.
	 */
	void setLeftRemapPath(String leftRemapPath);

	/**
	 * Get a stream remapped entry's right remap path; this corresponds to the
	 * right entry of the associated mapping.
	 */
	String getRightRemapPath();

	/**
	 * Set a stream remapped entry's right remap path; this corresponds to the
	 * right entry of the associated mapping.
	 */
	void setRightRemapPath(String rightRemapPath);
};
