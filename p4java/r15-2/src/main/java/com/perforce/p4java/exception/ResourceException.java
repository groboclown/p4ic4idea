/**
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Exception superclass for all P4Java resource-related exceptions.
 * 
 *
 */

public class ResourceException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public ResourceException() {
		super();
	}

	public ResourceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceException(String message) {
		super(message);
	}

	public ResourceException(Throwable cause) {
		super(cause);
	}
}
