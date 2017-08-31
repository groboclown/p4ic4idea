package com.perforce.p4java.exception;

/**
 * Signals a serious and probably unrecoverable client error in an underlying
 * transport layer.
 * 
 */
public class ClientError extends P4JavaError {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create an empty P4Java client error
	 */
	public ClientError() {
	}

	/**
	 * Create a P4Java client error
	 * 
	 * @param message
	 */
	public ClientError(String message) {
		super(message);
	}

	/**
	 * Create a P4Java client error
	 * 
	 * @param cause
	 */
	public ClientError(Throwable cause) {
		super(cause);
	}

	/**
	 * Create a P4Java client error
	 * 
	 * @param message
	 * @param cause
	 */
	public ClientError(String message, Throwable cause) {
		super(message, cause);
	}

}
