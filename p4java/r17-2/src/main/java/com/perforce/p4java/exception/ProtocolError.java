/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Signals a serious and probably unrecoverable protocol error
 * in an underlying transport layer.
 * 
 *
 */

public class ProtocolError extends P4JavaError {

	private static final long serialVersionUID = 1L;
	
	public ProtocolError() {
	}

	public ProtocolError(String message) {
		super(message);
	}

	public ProtocolError(Throwable cause) {
		super(cause);
	}

	public ProtocolError(String message, Throwable cause) {
		super(message, cause);
	}
}
