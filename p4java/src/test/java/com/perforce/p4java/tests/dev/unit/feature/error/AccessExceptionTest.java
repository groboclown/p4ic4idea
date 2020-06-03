/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.tests.dev.annotations.TestId;
import org.junit.Test;

import static com.perforce.p4java.P4JavaUtil.dummyServerErrorMessage;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("AccessExceptionTest")
public class AccessExceptionTest extends BaseThrowableTestHelper {
    // p4 ic4idea: doesn't work this way anymore, so override
    @Test
    public void testEmpty() {
        Throwable error = createThrowable();
        assertNotNull(error.getMessage());
        assertNull(error.getCause());

        error = createThrowable((String) null);
        assertNull(error.getMessage());
        assertNull(error.getCause());

        //error = createThrowable((Throwable) null);
        //assertNull(error.getMessage());
        //assertNull(error.getCause());

        error = createThrowable(null, null);
        assertNull(error.getMessage());
        assertNull(error.getCause());
    }


    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new TestableAccessException(dummyServerErrorMessage(""));
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	    return new TestableAccessException(dummyServerErrorMessage(message));
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
        AccessException ex = new TestableAccessException(dummyServerErrorMessage(cause.toString()));
        ex.initCause(cause);
        return ex;
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String,
     *      Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
        AccessException ex = new TestableAccessException(dummyServerErrorMessage(message));
        ex.initCause(cause);
        return ex;
    }

    private static class TestableAccessException extends AccessException {

        TestableAccessException(IServerMessage iServerMessage) {
            super(iServerMessage);
        }

        TestableAccessException(AccessException ex) {
            super(ex);
        }
    }
}
