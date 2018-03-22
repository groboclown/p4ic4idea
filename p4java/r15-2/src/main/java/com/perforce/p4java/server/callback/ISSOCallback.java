/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.server.callback;

/**
 * Defines the Perforce Single Sign On (SSO) callback interface.<p>
 * 
 * See the p4 undoc help for a full explanation of the Perforce
 * SSO scheme -- it will not be explained here in any detail.
 * 
 *
 */

public interface ISSOCallback {
	
	/**
	 *  SSO callback return status.
	 */
	enum Status {
		
		PASS,
		FAIL,
		UNSET
		
	}
	
	/**
	 * Maximum length in bytes allowed for the SSO credentials sent
	 * back to the Perforce server. Any StringBuffer passed to the 
	 * method(s) below will be truncated to this length before
	 * transmission.
	 */
	final int MAX_CRED_LENGTH = 131072;
	
	/**
	 * Return the Single Sign On (SSO) credentials for a specific Perforce
	 * server and Perforce user combination.<p>
	 * 
	 * If the method returns true, the associated string buffer contents
	 * are truncated (if necessary) to MAX_CRED_LENGTH bytes, then passed
	 * back to the Perforce server's SSO handler. If the method returns
	 * false, P4Java returns an auth fail to the Perforce server and it
	 * reacts accordingly. No character set translation or any other changes
	 * are made to the contents of the buffer before transmission.<p>
	 * 
	 * Note: as with all P4Java callbacks, providers are responsible for
	 * ensuring that the callback is thread-safe and does not block.
	 * 
	 * @param credBuffer StringBuffer to be filled in with SSO credentials to be
	 * 					sent back to the Perforce server
	 * @param ssoKey possibly-null opaque string associated with this callback
	 * 					and IServer object by a previous register call.
	 * @param userName current Perforce user name for the IServer; may be null
	 * 					if no user name has yet been associated with the server.
	 * @return status of whether the credential buffer was retrieve
	 *         (Status.PASS), not retrieved (Status.FAIL), or not attempted to
	 *         be retrieved (Status.UNSET).
	 */
	
	Status getSSOCredentials(StringBuffer credBuffer, String ssoKey, String userName);
}
