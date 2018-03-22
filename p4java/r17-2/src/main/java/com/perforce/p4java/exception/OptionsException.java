/**
 * 
 */
package com.perforce.p4java.exception;

/**
 * A P4Java extension used to signal to participating
 * users that an error occurred in Options object processing.
 */

public class OptionsException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public OptionsException() {
		super();
	}

	public OptionsException(String message, Throwable cause) {
		super(message, cause);
	}

	public OptionsException(String message) {
		super(message);
	}

	public OptionsException(Throwable cause) {
		super(cause);
	}
}
