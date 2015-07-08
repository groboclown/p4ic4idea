/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Defines known Perforce message error subsystem codes as returned from the P4Java
 * message and exception system. The subsystem code signals where a specific message
 * originated in the Perforce server or client; it is not always exact, in that
 * the message may have more than one real origin, but in general P4Java returns
 * the code the server returned when it can get this information.<p>
 * 
 * Note that this information is not always available, and is typically only
 * visible through the RequestException mechanism if it's available at all.
 * Most of these codes will not be encountered in normal use by P4Java users, but
 * ES_INFO, ES_SERVER, and ES_CLIENT (at least) may be seen in everyday usage.
 * 
 *
 */

public class MessageSubsystemCode {
	
	/**
	 * Operating system error (i.e., typically an error detected
	 * by the OS).
	 */
	public static final int ES_OS = 0;
	
	/**
	 * Error in miscellaneous support modules or apps.
	 */
	public static final int ES_SUPP = 1;
	
	/**
	 * Error in the server librarian module.
	 */
	public static final int ES_LBR = 2;
	
	/**
	 * Error in the underlying Perforce transport protocol.
	 */
	public static final int ES_RPC = 3;
	
	/**
	 * Error in the server-side metadata database.
	 */
	public static final int ES_DB = 4;
	
	/**
	 * Error in the server-side metadata database support modules.
	 */
	public static final int ES_DBSUPP = 5;
	
	/**
	 * Error in the server-side data manager.
	 */
	public static final int ES_DM = 6;
	
	/**
	 * Error in the generic upper levels of the Perforce server.
	 */
	public static final int ES_SERVER = 7;
	
	/**
	 * Error in the generic upper levels of the Perforce client (including P4Java).
	 */
	public static final int ES_CLIENT = 8;
	
	/**
	 * Pseudo subsystem for information messages.
	 */
	public static final int ES_INFO = 9;
	
	/**
	 * Pseudo subsystem for help messages.
	 */
	public static final int ES_HELP = 10;
	
	/**
	 * Pseudo subsystem for spec / comment messages.
	 */
	public static final int ES_SPEC = 11;
	
	/**
	 * Error occurred in the P4FTP server.
	 */
	public static final int ES_FTPD = 12;
	
	/**
	 * Error occurred in the Perforce broker.
	 */
	public static final int ES_BROKER = 13;
}
