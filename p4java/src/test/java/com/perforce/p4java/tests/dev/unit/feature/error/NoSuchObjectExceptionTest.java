/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("NoSuchObjectExceptionTest")
public class NoSuchObjectExceptionTest extends BaseThrowableTestHelper {

    /**
     * @see BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new NoSuchObjectException();
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	return new NoSuchObjectException(message);
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new NoSuchObjectException(cause);
    }

    /**
     * @see BaseThrowableTestHelper#createThrowable(String,
     *      Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new NoSuchObjectException(message, cause);
    }

}
