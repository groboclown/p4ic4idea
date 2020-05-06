/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 login' with bad password; expects an AccessException.
 */
@Jobs({ "job049985" })
@TestId("LoginDev112_ExceptionTest")
public class LoginExceptionTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LoginExceptionTest.class.getSimpleName());


	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
	    try {
            setupServer(p4d.getRSHURL(), userName, password, true, props);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } 
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test 'p4 login' with bad password; expects an AccessException.
	 */
	@Test
	public void testLoginBadPassword() {
		String password = "badpassword123";
		try {
			// Set the server user
			server.setUserName(userName);
			try {
				// Login using a bad password
				server.login(password, new LoginOptions());
			} catch (P4JavaException e) {
				// Expect an AccessException
				assertTrue(e instanceof AccessException);
				assertTrue(e.getMessage().contains("Password invalid."));
			}
		    } catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}
}
