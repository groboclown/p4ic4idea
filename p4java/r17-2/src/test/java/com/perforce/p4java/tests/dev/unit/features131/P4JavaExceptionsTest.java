/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test different types of P4Java exceptions.
 */
@TestId("Dev131_P4JavaExceptionsTest")
public class P4JavaExceptionsTest extends P4JavaTestCase {

	/**
	 * Test different types of P4Java exceptions.
	 */
	@Test
	public void testP4JavaExceptions() {
		
		try {
			testP4JavaException1();
		} catch (ConnectionException e) {
			System.out.println(e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}

		try {
			testP4JavaException2();
		} catch (ConnectionException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			System.out.println(e.getLocalizedMessage());
		}
	
		try {
			testP4JavaException3();
		} catch (ConnectionException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			System.out.println(e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	
	private void testP4JavaException1() throws ConnectionException, RequestException, AccessException {
		
		try {
			testConnectioinException();
		} catch (ConnectionException e) {
			throw e;
		} catch (AccessException e) {
			throw e;
		} catch (RequestException e) {
			throw e;
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	
	private void testP4JavaException2() throws ConnectionException, RequestException, AccessException {
		
		try {
			testRequestException();
		} catch (ConnectionException e) {
			throw e;
		} catch (AccessException e) {
			throw e;
		} catch (RequestException e) {
			throw e;
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	private void testP4JavaException3() throws ConnectionException, RequestException, AccessException {
		
		try {
			testAccessException();
		} catch (ConnectionException e) {
			throw e;
		} catch (AccessException e) {
			throw e;
		} catch (RequestException e) {
			throw e;
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	private void testConnectioinException() throws P4JavaException {
		
		throwConnectioinException();
	}
	
	private void testRequestException() throws P4JavaException {
		
		throwRequestException();
	}

	private void testAccessException() throws P4JavaException {
		
		throwAccessException();
	}

	private void throwConnectioinException() throws ConnectionException {
		
		throw new ConnectionException("connection exception!");
	}

	private void throwRequestException() throws RequestException {
		
		throw new RequestException("request exception!");
	}

	private void throwAccessException() throws AccessException {
		
		throw new AccessException("access exception!");
	}
}
