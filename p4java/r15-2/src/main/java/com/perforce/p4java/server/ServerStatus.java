/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.server;

/**
 * Used to describe the protocol-independent status of an IServer object.
 * 
 *
 */

public enum ServerStatus {
	/**
	 * State is unknown. Don't use this server!
	 */
	
	UNKNOWN,
	
	/**
	 * Server is in an error state; this may be a connection problem or a problem
	 * with the Perforce server itself.
	 */
	
	ERROR,
	
	/**
	 * Server object is ready for use and error-free.
	 */
	READY,
	
	/**
	 * Server object has been properly disconnected from the associated
	 * Perforce server, either by the user or by the server (usually as the 
	 * result of a timeout). Not considered an error state.
	 */
	
	DISCONNECTED;
}
