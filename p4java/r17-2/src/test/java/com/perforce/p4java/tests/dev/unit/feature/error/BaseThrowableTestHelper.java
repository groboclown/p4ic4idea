/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public abstract class BaseThrowableTestHelper extends P4JavaTestCase {

    /**
     * Test message
     */
    @Test
    public void testMessage() {
	String message = "message";
	Throwable throwable = createThrowable(message);
	Assert.assertNotNull(throwable);
	Assert.assertNull(throwable.getCause());
	Assert.assertEquals(message, throwable.getMessage());
    }

    /**
     * Test cause
     */
    @Test
    public void testCause() {
	Exception cause = new Exception();
	Throwable throwable = createThrowable(cause);
	Assert.assertNotNull(throwable);
	Assert.assertNotNull(throwable.getCause());
	Assert.assertEquals(cause, throwable.getCause());
	Assert.assertNotNull(throwable.getMessage());
    }

    /**
     * Test empty
     */
    @Test
    public void testEmpty() {
	Throwable error = createThrowable();
	assertNull(error.getMessage());
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
     * Test message and cause
     */
    @Test
    public void testMessageAndCause() {
	String message = "message";
	Exception cause = new Exception();
	Throwable throwable = createThrowable(message, cause);
	Assert.assertNotNull(throwable);
	Assert.assertNotNull(throwable.getMessage());
	Assert.assertEquals(message, throwable.getMessage());
	Assert.assertNotNull(throwable.getCause());
	Assert.assertEquals(cause, throwable.getCause());
    }

    /**
     * Create throwable
     * 
     * @return throwable
     */
    protected abstract Throwable createThrowable();

    /**
     * Create throwable
     * 
     * @param message
     * @return throwable
     */
    protected abstract Throwable createThrowable(String message);

    /**
     * Create throwable
     * 
     * @param cause
     * @return throwable
     */
    protected abstract Throwable createThrowable(Throwable cause);

    /**
     * Create throwable
     * 
     * @param message
     * @param cause
     * @return throwable
     */
    protected abstract Throwable createThrowable(String message, Throwable cause);
}
