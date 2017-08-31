/**
 * 
 */
package com.perforce.p4java.exception;

/**
 * Error class used to signal null pointers.
 * 
 *
 */

public class NullPointerError extends P4JavaError {

	private static final long serialVersionUID = 1L;

	public NullPointerError() {
		super();
	}

	public NullPointerError(String message, Throwable cause) {
		super(message, cause);
	}

	public NullPointerError(String message) {
		super(message);
	}

	public NullPointerError(Throwable cause) {
		super(cause);
	}
}
