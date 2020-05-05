package com.perforce.p4java.exception;

public class InvalidUrlException extends ConnectionException {

	private static final long serialVersionUID = 1L;

	public InvalidUrlException() {
		super("Can't navigate to URL: protocol must either be http or https!");
	}

	public InvalidUrlException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidUrlException(String message) {
		super(message);
	}

	public InvalidUrlException(Throwable cause) {
		super(cause);
	}

}
