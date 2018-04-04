/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 passwd' against an unicode enabled server with security level 2 or 3
 */
@Jobs({ "job044472", "job044417" })
@TestId("Dev112_ChangePasswordTest")
public class HighSecurityLevelPasswordTest extends P4JavaTestCase {

	final static String highSecurityLevelServerURL = "p4java://eng-p4java-vm.perforce.com:30111";
	
	
	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;

	IOptionsServer superServer = null;
	IClient superClient = null;
	String superServerMessage = null;

	IUser newUser = null;

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
			server = ServerFactory.getOptionsServer(highSecurityLevelServerURL,
					null);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			});
			server.connect();
            if (server.isConnected()) {
                if (server.supportsUnicode()) {
                	server.setCharsetName("utf8");
                }
            }
			server.setUserName(getUserName());
			server.login(getPassword());
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);

			// Requires super user
			superServer = ServerFactory.getOptionsServer(highSecurityLevelServerURL,
					null);
			assertNotNull(superServer);

			// Register callback
			superServer.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					superServerMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					superServerMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					superServerMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					superServerMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					superServerMessage = String.valueOf(millisecsTaken);
				}
			});
			superServer.connect();
            if (superServer.isConnected()) {
                if (superServer.supportsUnicode()) {
                	superServer.setCharsetName("utf8");
                }
            }
            superServer.setUserName(getSuperUserName());
			superServer.login(getSuperUserPassword());
			superClient = superServer.getClient("p4TestSuperWS20112");
			assertNotNull(superClient);
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
	 * Test 'p4 passwd' against an unicode enabled server with security level 2 or 3
	 */
	@Test
	public void testChangPassword() {
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
			// The server is set to security level 2, we should get an error.
			try {
				server.login("");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Password must be set before access can be granted."));
			}
			
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
			String password2 = "toyota123";
			message = server.changePassword(password1, password2, "");
			assertNotNull(message);

			// Login again
			server.login(password2);
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contains("User " + newUserName + " logged in."));

			// Use the super user to change the password
			// Should get an error message, because the password length is < 8
			try {
				message = superServer.changePassword(null, "xyz123", newUserName);
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Password should be at least 8 characters in length."));
			}

			// Use the super user to change the password to something else
			// Password length is 8
			String password3 = "abcd1234";
			message = superServer.changePassword(null, password3, newUserName);
			assertNotNull(message);

			// Login using the old password
			// Should get an error message
			try {
				server.login(password2);
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Password invalid."));
			}

			// Login using the new password
			server.login(password3);
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
