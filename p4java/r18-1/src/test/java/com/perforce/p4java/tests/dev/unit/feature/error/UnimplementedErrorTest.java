/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("UnimplementedErrorTest")
public class UnimplementedErrorTest extends BaseThrowableTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new UnimplementedError();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.String)
     */
    protected Throwable createThrowable(String message) {
	return new UnimplementedError(message);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new UnimplementedError(cause);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.String,
     *      java.lang.Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new UnimplementedError(message, cause);
    }

}
