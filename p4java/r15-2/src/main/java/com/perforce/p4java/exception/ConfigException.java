/**
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Exception class for P4Java configuration-related exceptions.
 * 
 *
 */

public class ConfigException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public ConfigException() {
		super();
	}

	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(Throwable cause) {
		super(cause);
	}
}
