/**
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Exception class to use to signal missing objects within p4java; this
 * is <i>not</i> used for missing objects on the Perforce server side.
 * 
 *
 */

public class NoSuchObjectException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public NoSuchObjectException() {
		super();
	}

	public NoSuchObjectException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoSuchObjectException(String message) {
		super(message);
	}

	public NoSuchObjectException(Throwable cause) {
		super(cause);
	}
}
