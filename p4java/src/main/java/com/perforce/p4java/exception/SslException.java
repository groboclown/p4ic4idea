/*
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Special subclass of ConnectionException to signal an issue connecting to
 * a Perforce server using the SSL protocol.
 *
 */
// p4ic4idea: Created to allow for better precision in pinpointing the cause of an error.
public class SslException extends ConnectionException {
	private static final long serialVersionUID = 1L;

	public SslException(String message) {
		super(message);
	}

	public SslException(String message, Throwable cause) {
		super(message, cause);
	}
}
