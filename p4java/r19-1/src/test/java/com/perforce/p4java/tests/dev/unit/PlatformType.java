/**
 * 
 */
package com.perforce.p4java.tests.dev.unit;

/**
 * Simple host platform type specifier. Note
 * that "Linux" includes Unix types here...
 */

public enum PlatformType {
	UNKNOWN,
	MACOSX,		// and NOT Mac OS 9...
	WINDOWS,	// all variants, at least so far...
	LINUX
}
