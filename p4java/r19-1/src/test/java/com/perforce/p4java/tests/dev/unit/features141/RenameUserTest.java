/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.*;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test 'p4 renameuser' command.
 */
@Jobs({ "job071639" })
@TestId("Dev141_RenameUserTest")
public class RenameUserTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", RenameUserTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		final String depotName = this.getRandomName(false, "test");
		final String clientName = "test-rename-user-test-client";
		try {
			setupServer(p4d.getRSHURL(), superUserName, superUserPassword, true, null);
			setupUtf8(server);
			client = new Client();
			client.setName("test-rename-user-test-client");
			assertNotNull(client);
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
	 * Test 'p4 renameuser' command.
	 */
	@Test
	public void testRenameUser() {
		IUser user = null;
		final String userName = "p4jtest-job071639-" + this.getRandomInt();
		final String fullName = "Test user created by junit test " + this.getTestId();
		final String userPassword = null;
		final String expectedStatus = "User " + userName + " saved.";
		final String newUserName = "renamedUser-" + userName;
		final String expectedRenameStatus = "User " + userName + " renamed to " + newUserName + ".";
		final String nonExistingUserName = "nonExistingUserName-" + this.getRandomInt();
		final String expectedNonExistingStatus = "User " + nonExistingUserName + " doesn't exist.";
		final String expectedAlreadyExistsStatus = "User " + this.userName + " already exists.";
		
		try {
			// Create a user
			user = User.newUser(userName, "blah@blah.blah", fullName, userPassword);
			assertNotNull(user);
			String createStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			assertNotNull("null status string from createUser", createStr);
			assertEquals("user not created on server: " + createStr, expectedStatus, createStr);

			// Rename the created user to another user name.
			String renameStr = server.renameUser(userName, newUserName);
			assertEquals("rename user failed", expectedRenameStatus, renameStr);
		
			// Rename the created user to an existing user and check for error message.
			try {
				server.renameUser(newUserName, this.userName);
			} catch (P4JavaException e) {
				assertEquals("rename user failed", expectedAlreadyExistsStatus, e.getLocalizedMessage().trim());
			}
		
			// Rename a non-existing user and check for error message.
			try {
				server.renameUser(nonExistingUserName, newUserName);
			} catch (P4JavaException e) {
				assertEquals("rename user failed", expectedNonExistingStatus, e.getLocalizedMessage().trim());
			}

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if ((server != null) && (user != null)) {
				try {
					server.deleteUser(newUserName, new UpdateUserOptions().setForceUpdate(true));
				} catch (P4JavaException e) {
					// ignore
				}
			}
		}
	}
}
