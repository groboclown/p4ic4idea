/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Defines known Perforce error message generic codes. These define
 * the broad type of the particular error, and are typically returned
 * from the Perforce server as as simple integers.<p>
 * 
 * These codes are normally made available through the RequestException
 * class, but may not be set for all errors encountered, so EV_NONE does not
 * necessarily mean no error, but only that there's no generic code associated
 * with this specific error.
 * 
 *
 */

public class MessageGenericCode {
	
	/**
	 * No code has been set yet, or no specific code is associated with this error.
	 */
	public static final int EV_NONE 	= 0;

	// The fault of the user

	/**
	 * A usage error.
	 */
	public static final int EV_USAGE	= 0x01;
	
	/**
	 * The accessed or associated entity is unknown.
	 */
	public static final int EV_UNKNOWN	= 0x02;
	
	/**
	 * Using a Perforce entity in the wrong context.
	 */
	public static final int EV_CONTEXT	= 0x03;
	
	/**
	 * Trying to do something prohibited by Perforce.
	 */
	public static final int EV_ILLEGAL	= 0x04;
	
	/**
	 * Something needs to be corrected before this operation can succeed.
	 */
	public static final int EV_NOTYET	= 0x05;
	
	/**
	 * Perforce protections prevented this operation from succeeding.
	 */
	public static final int EV_PROTECT	= 0x06;

	// No fault at all

	/**
	 * Perforce operation returned empty results.
	 */
	public static final int EV_EMPTY	= 0x11;

	// not the fault of the user

	/**
	 * Unexplained or unknown fault in a Perforce program or server.
	 */
	public static final int EV_FAULT	= 0x21;
	
	/**
	 * Client-side program errors.
	 */
	public static final int EV_CLIENT	= 0x22;
	
	/**
	 * Perforce server administrative action required.
	 */
	public static final int EV_ADMIN	= 0x23;
	
	/**
	 * Client configuration inadequate.
	 */
	public static final int EV_CONFIG	= 0x24;
	
	/**
	 * Perforce client or server too old to interact.
	 */
	public static final int EV_UPGRADE	= 0x25;
	
	/**
	 * Communications error.
	 */
	public static final int EV_COMM		= 0x26;
	
	/**
	 * Something passed to the Perforce server is too big.
	 */
	public static final int EV_TOOBIG	= 0x27;
}
