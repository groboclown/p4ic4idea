/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.tests.dev.annotations.TestId;
import org.junit.Test;

import static com.perforce.p4java.P4JavaUtil.dummyServerErrorMessage;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("RequestExceptionTest")
public class RequestExceptionTest extends BaseThrowableTestHelper {
    // p4ic4idea: doesn't work the standard way anymore.
    @Test
    public void testEmpty() {
        Throwable error = createThrowable();
        assertNotNull(error.getMessage());
        assertNull(error.getCause());

        error = createThrowable((String) null);
        assertNull(error.getMessage());
        assertNull(error.getCause());

        error = createThrowable((Throwable) null);
        assertNull(error.getMessage());
        assertNull(error.getCause());

        error = createThrowable(null, null);
        assertNull(error.getMessage());
        assertNull(error.getCause());
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new RequestException(dummyServerErrorMessage("error"));
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.String)
     */
    protected Throwable createThrowable(String message) {
	return new RequestException(message);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new RequestException(cause);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(java.lang.String,
     *      java.lang.Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new RequestException(message, cause);
    }

}
