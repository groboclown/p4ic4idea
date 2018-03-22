/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("ConfigExceptionTest")
public class ConfigExceptionTest extends BaseThrowableTestHelper {

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable()
     */
    protected Throwable createThrowable() {
	return new ConfigException();
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String)
     */
    protected Throwable createThrowable(String message) {
	return new ConfigException(message);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(Throwable)
     */
    protected Throwable createThrowable(Throwable cause) {
	return new ConfigException(cause);
    }

    /**
     * @see com.perforce.p4java.tests.dev.unit.feature.error.BaseThrowableTestHelper#createThrowable(String,
     *      Throwable)
     */
    protected Throwable createThrowable(String message, Throwable cause) {
	return new ConfigException(message, cause);
    }

}
