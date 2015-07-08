/**
 * Copyright 2014 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.env;

/**
 * Provides access to Perforce environment variables.
 */
public class PerforceEnvironment {

	// Perforce environment variables
	public static final String P4PORT = "P4PORT";
	public static final String P4USER = "P4USER";
	public static final String P4PASSWD = "P4PASSWD";
	public static final String P4CLIENT = "P4CLIENT";
	
	public static final String P4HOST = "P4HOST";
	public static final String P4CHARSET = "P4CHARSET";
	public static final String P4TICKETS = "P4TICKETS";
	public static final String P4TRUST = "P4TRUST";
	public static final String P4IGNORE = "P4IGNORE";

	public static final String P4CONFIG = "P4CONFIG";
	public static final String P4ENVIRO = "P4ENVIRO";

	// Default P4ENVIRO file path
	public static final String DEFAULT_P4ENVIRO_FILE = System.getProperty("user.home") + "/.p4enviro";
	
	public static String getP4Port() {
		return System.getenv(P4PORT);
	}

	public static String getP4User() {
		return System.getenv(P4USER);
	}

	public static String getP4Passwd() {
		return System.getenv(P4PASSWD);
	}

	public static String getP4Client() {
		return System.getenv(P4CLIENT);
	}

	public static String getP4Host() {
		return System.getenv(P4HOST);
	}

	public static String getP4Charset() {
		return System.getenv(P4CHARSET);
	}

	public static String getP4Tickets() {
		return System.getenv(P4TICKETS);
	}

	public static String getP4Trust() {
		return System.getenv(P4TRUST);
	}

	public static String getP4Ignore() {
		return System.getenv(P4IGNORE);
	}

	public static String getP4Config() {
		return System.getenv(P4CONFIG);
	}

	/**
	 * If not set and return the default P4ENVIRO file path.
	 */
	public static String getP4Enviro() {
		if (System.getenv(P4ENVIRO) != null) {
			return System.getenv(P4ENVIRO);
		}
		return DEFAULT_P4ENVIRO_FILE;
	}
}
