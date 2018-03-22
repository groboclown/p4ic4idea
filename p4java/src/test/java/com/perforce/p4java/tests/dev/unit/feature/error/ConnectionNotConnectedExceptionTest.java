/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.ConnectionNotConnectedException;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("ConnectionNotConnectedException")
public class ConnectionNotConnectedExceptionTest extends BaseThrowableTestHelper {

    /**
     * @see BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new ConnectionNotConnectedException();
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	return new ConnectionNotConnectedException(message);
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new ConnectionNotConnectedException(cause);
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(String,
     *      Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new ConnectionNotConnectedException(message, cause);
    }

}
