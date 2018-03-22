/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Exception class for P4Java file encoding exceptions.
 */

public class FileEncoderException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public FileEncoderException() {
		super();
	}

	public FileEncoderException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileEncoderException(String message) {
		super(message);
	}

	public FileEncoderException(Throwable cause) {
		super(cause);
	}
}
