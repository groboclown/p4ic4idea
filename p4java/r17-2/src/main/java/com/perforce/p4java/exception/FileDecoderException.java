/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Exception class for P4Java file decoding exceptions.
 */

public class FileDecoderException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	public FileDecoderException() {
		super();
	}

	public FileDecoderException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileDecoderException(String message) {
		super(message);
	}

	public FileDecoderException(Throwable cause) {
		super(cause);
	}
}
