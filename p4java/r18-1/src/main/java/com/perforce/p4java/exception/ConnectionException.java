package com.perforce.p4java.exception;


/**
 * Superclass for all P4Java connection-related exceptions.
 * 
 *
 */

public class ConnectionException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public ConnectionException() {
		super();
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionException(String message) {
		super(message);
	}

	public ConnectionException(Throwable cause) {
		super(cause);
	}
}
