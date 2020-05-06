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
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new ConnectionNotConnectedException();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.String)
     */
    protected Throwable createThrowable(String message) {
	return new ConnectionNotConnectedException(message);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new ConnectionNotConnectedException(cause);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.String,
     *      java.lang.Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new ConnectionNotConnectedException(message, cause);
    }

}
