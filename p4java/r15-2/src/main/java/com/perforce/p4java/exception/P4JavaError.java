package com.perforce.p4java.exception;

/**
 * Class intended to be used to signal unrecoverable errors that a client
 * or other package should probably not handle or that signal serious
 * errors without known fixes.<p>
 * 
 * The intention here is to use this for things like null pointer
 * errors, etc., that methods may check for but that should not have to
 * be explicitly mentioned in the "throws" clause or dealt with by all
 * callers.<p>
 * 
 * Note that P4JavaError and derived classes don't use the P4JavaException resource
 * bundle scheme for error messages -- the thinking here is that errors are thrown
 * when bundles, etc. may fail and all hell is breaking loose....
 * 
 *
 */

public class P4JavaError extends Error {

	private static final long serialVersionUID = 1L;

	public P4JavaError() {
	}

	public P4JavaError(String message) {
		super(message);
	}

	public P4JavaError(Throwable cause) {
		super(cause);
	}

	public P4JavaError(String message, Throwable cause) {
		super(message, cause);
	}
}
