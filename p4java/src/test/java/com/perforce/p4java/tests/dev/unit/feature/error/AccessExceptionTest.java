/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("AccessExceptionTest")
public class AccessExceptionTest extends BaseThrowableTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new AccessException(dummyServerErrorMessage(""));
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	    return new AccessException(dummyServerErrorMessage(message));
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
        AccessException ex = new AccessException(dummyServerErrorMessage(cause.getMessage()));
        ex.initCause(cause);
        return new AccessException(ex);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String,
     *      Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
        AccessException ex = new AccessException(dummyServerErrorMessage(message));
        ex.initCause(cause);
        return new AccessException(ex);
    }

}
