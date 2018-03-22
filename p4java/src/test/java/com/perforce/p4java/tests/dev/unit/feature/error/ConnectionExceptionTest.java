/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("ConnectionExceptionTest")
public class ConnectionExceptionTest extends BaseThrowableTestHelper {

    /**
     * @see BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new ConnectionException();
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	return new ConnectionException(message);
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new ConnectionException(cause);
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(String, Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new ConnectionException(message, cause);
    }

}
