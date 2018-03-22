/**
 * 
 */
package com.perforce.p4java.exception;

/**
 * Exception thrown by P4Java methods when access to data or services has
 * been denied by the Perforce server. This is usually a login or
 * permissions issue.
 * 
 *
 */

public class AccessException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public AccessException() {
		super();
	}

	public AccessException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccessException(String message) {
		super(message);
	}

	public AccessException(Throwable cause) {
		super(cause);
	}

}
