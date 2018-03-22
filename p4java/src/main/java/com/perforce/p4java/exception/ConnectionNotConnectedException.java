/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Special subclass of ConnectionException to signal the case of
 * attempting to issue a Perforce server command with an IServer
 * that hasn't been explicitly connected to that Perforce server.<p>
 * 
 * Usually, if you get one of these, you've either forgotten to
 * connect the server, or the server's become disconnected on
 * its own, typically as a result of the underlying protocol
 * timing out or the Perforce server at the other end going
 * away, etc.
 * 
 *
 */

public class ConnectionNotConnectedException extends ConnectionException {

	private static final long serialVersionUID = 1L;

	public ConnectionNotConnectedException() {
		super();
	}

	public ConnectionNotConnectedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionNotConnectedException(String message) {
		super(message);
	}

	public ConnectionNotConnectedException(Throwable cause) {
		super(cause);
	}
}
