/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Defines known Perforce error severity codes. These are basically
 * self-explanatory, and are usually made available through the
 * RequestException class. Not all such exceptions contain meaningful
 * severity codes, so always treat E_EMPTY as meaning "not set" rather
 * than "not a severe error" (or whatever).
 * 
 *
 */

public class MessageSeverityCode {
	/**
	 * No severity code has been set.
	 */
	public static final int E_EMPTY = 0;
	
	/**
	 * Information only -- not an error in the traditional sense.
	 */
	public static final int E_INFO = 1;
	
	/**
	 * A warning message -- probably worth investigating, but not an actual error.
	 */
	public static final int E_WARN = 2;
	
	/**
	 * A failure caused by user error (a "normal" error).
	 */
	public static final int E_FAILED = 3;
	
	/**
	 * A fatal error caused by a problem in the Perforce system; this
	 * almost always means nothing else will work properly from this point on,
	 * and you should abandon further processing.
	 */
	public static final int E_FATAL = 4;
}
