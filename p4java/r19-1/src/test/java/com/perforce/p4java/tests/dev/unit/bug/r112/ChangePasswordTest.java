/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 passwd'. Specifying a username as an argument to 'p4 passwd'
 * requires 'super' access granted by 'p4 protect'.
 */
@Jobs({ "job044472", "job044417" })
@TestId("Dev112_ChangePasswordTest")
public class ChangePasswordTest extends P4JavaRshTestCase {

	IClient client = null;
	IOptionsServer superServer = null;
	IClient superClient = null;
	IUser newUser = null;

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ChangePasswordTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).

		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
			superServer = getSuperConnection(p4d.getRSHURL());
			assertNotNull(superServer);
			superClient = superServer.getClient("p4TestSuperWS20112");
			superServer.setCurrentClient(superClient);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test 'p4 passwd'.
	 */
	@Test
	public void testChangePassword() {
		try {
			// Create a new user, password not set.
			int randNum = getRandomInt();
			String newUserName = "testuser" + randNum;
			String email = newUserName + "@localhost.localdomain";
			String fullName = "New P4Java Test User " + randNum;
			newUser = new User(newUserName, email, fullName, null, null, null,
					null, UserType.STANDARD, null);
			String message = superServer.createUser(newUser, true);
			assertNotNull(message);
			assertTrue(message.contentEquals("User " + newUserName + " saved."));

			newUser = server.getUser(newUserName);
			assertNotNull(newUser);

			// Set the user
			server.setUserName(newUserName);

			// Add the user in the p4users group
			IUserGroup userGroup = superServer.getUserGroup("p4users");
			assertNotNull(userGroup);
			userGroup.getUsers().add(newUserName);
			message = superServer.updateUserGroup(userGroup, new UpdateUserGroupOptions());
			assertNotNull(message);
			assertTrue(message.contentEquals("Group p4users updated."));
			
			// Login with a empty password.
			server.login("");
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contains("'login' not necessary, no password set for this user."));
			
			// Change password
			String password1 = "abc ' \" # @ 12' \" \" 3";
			message = server.changePassword(null, password1, null);
			assertNotNull(message);
			assertTrue(message.contains("Password updated."));
			
			List<IDepot> depots = null;

			// Should get an error message
			try {
				depots = server.getDepots();
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Perforce password (P4PASSWD) invalid or unset."));
			}

			// Login using a partial password
			// Should get an error message
			try {
				server.login("abc ' \"");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Password invalid."));
			}
			
			// Login with the new password
			server.login(password1);
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contains("User " + newUserName + " logged in."));

			// Should get a list of depots
			depots = server.getDepots();
			assertNotNull(depots);
			assertTrue(depots.size() > 0);

			// Set another password
			String password2 = "abc123";
			message = server.changePassword(password1, password2, "");
			assertNotNull(message);

			// Login again
			server.login(password2);
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contains("User " + newUserName + " logged in."));

			// Delete the password
			String password3 = null;
			message = server.changePassword(password2, password3, "");
			assertNotNull(message);
			assertTrue(message.contains("Password deleted."));
			
			// Login again
			server.login(password3);
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contains("'login' not necessary, no password set for this user."));

			// Use the super user to change the password to something else
			String password4 = "abcd1234";
			message = superServer.changePassword(null, password4, newUserName);
			assertNotNull(message);
			assertTrue(message.contains("Password updated."));

			// Login using the old password
			// Should get an error message
			try {
				server.login(password2);
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Password invalid."));
			}

			// Login using the new password
			server.login(password4);
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contains("User " + newUserName + " logged in."));

			// Get a list of depots
			depots = server.getDepots();
			assertNotNull(depots);
			assertTrue(depots.size() > 0);

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				if (superServer != null) {
					if (newUser != null) {
						String message = superServer.deleteUser(
								newUser.getLoginName(), true);
						assertNotNull(message);
						// Remove the user in the p4users group
						IUserGroup userGroup = superServer.getUserGroup("p4users");
						assertNotNull(userGroup);
	                    for (Iterator<String> it = userGroup.getUsers().iterator(); it.hasNext();) {
	                        String s = it.next();
	                        if (s.contentEquals(newUser.getLoginName())) {
	                            it.remove();
	                        }
	                    }
						message = superServer.updateUserGroup(userGroup, new UpdateUserGroupOptions());
						assertNotNull(message);
					}
				}
			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}
	}
}
