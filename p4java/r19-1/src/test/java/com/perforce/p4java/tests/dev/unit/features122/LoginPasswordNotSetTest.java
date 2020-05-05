/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 login' with user's password not set.
 */
@Jobs({ "job055297" })
@TestId("Dev122_LoginPasswordNotSetTest")
public class LoginPasswordNotSetTest extends P4JavaRshTestCase {

	IOptionsServer superServer = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LoginPasswordNotSetTest.class.getSimpleName());
    
   	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
	        setupServer(p4d.getRSHURL(), userName, password, true, null);
		    superServer = getSuperConnection(p4d.getRSHURL());
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test 'p4 login' with user's password not set.
	 */
	@Test
	public void testLoginWithPasswordNotSet() {
		IUser user = null;
		final String userName = "p4jtestjob055297";
		final String fullName = "Test user created by junit test " + this.getTestId();
		final String userPassword = null;
		final String expectedStatus = "User " + userName + " saved.";
		
		try {
			user = User.newUser(userName, "invalid@invalid.invalid", fullName, userPassword);
			assertNotNull(user);
			String createStr = superServer.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			assertNotNull("null status string from createUser", createStr);
			assertEquals("user not created on superServer: " + createStr, expectedStatus, createStr);


			server = getServer(p4d.getRSHURL(), null, null, null);
			assertNotNull(server);
			server.setUserName(userName);
			try {
				// Login with some password. Since the user's password is not
				// set we should get an access exception.
				server.login("mmdsaofjsdiofjsao", new LoginOptions());
			} catch (P4JavaException p4je) {
				assertTrue(p4je instanceof AccessException);
				assertEquals(p4je.getMessage(), "'login' not necessary, no password set for this user.");
			}
			server.logout();
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if ((superServer != null) && (user != null)) {
				try {
				    superServer.deleteUser(userName, new UpdateUserOptions().setForceUpdate(true));
				} catch (P4JavaException e) {
					// ignore
				}
			}
		}
	}
}
