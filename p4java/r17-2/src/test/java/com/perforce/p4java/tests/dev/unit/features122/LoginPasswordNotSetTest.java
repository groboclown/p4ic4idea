/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 login' with user's password not set.
 */
@Jobs({ "job055297" })
@TestId("Dev122_LoginPasswordNotSetTest")
public class LoginPasswordNotSetTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IOptionsServer server2 = null;
	String serverMessage = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = this.getServerAsSuper();
			server2 = getServer(serverUrlString, null, null, null);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
		if (server2 != null) {
			this.endServerSession(server2);
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
			String createStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			assertNotNull("null status string from createUser", createStr);
			assertEquals("user not created on server: " + createStr, expectedStatus, createStr);
			
			server2 = getServer(serverUrlString, null, null, null);
			assertNotNull(server2);
			server2.setUserName(userName);
			try {
				// Login with some password. Since the user's password is not
				// set we should get an access exception.
				server2.login("mmdsaofjsdiofjsao", new LoginOptions());
			} catch (P4JavaException p4je) {
				assertTrue(p4je instanceof AccessException);
				assertEquals(p4je.getMessage(), "'login' not necessary, no password set for this user.");
			}
			server2.logout();
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if ((server != null) && (user != null)) {
				try {
					server.deleteUser(userName, new UpdateUserOptions().setForceUpdate(true));
				} catch (P4JavaException e) {
					// ignore
				}
			}
		}
	}
}
