/**
 * 
 */
package com.perforce.p4java.exception;

/**
 * Error class used to signal an unimplemented feature or method.
 * 
 * @version	$Id$
 *
 */
public class UnimplementedError extends P4JavaError {

	private static final long serialVersionUID = 1L;

	public UnimplementedError() {
		super();
	}

	public UnimplementedError(String message, Throwable cause) {
		super(message, cause);
	}

	public UnimplementedError(String message) {
		super(message);
	}

	public UnimplementedError(Throwable cause) {
		super(cause);
	}
}
