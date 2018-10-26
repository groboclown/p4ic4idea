/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.tests.dev.annotations.TestId;

import static com.perforce.p4java.P4JavaUtil.dummyServerErrorMessage;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("RequestExceptionTest")
public class RequestExceptionTest extends BaseThrowableTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new RequestException(dummyServerErrorMessage("error"));
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	return new RequestException(message);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new RequestException(cause);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String,
     *      Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new RequestException(message, cause);
    }

}