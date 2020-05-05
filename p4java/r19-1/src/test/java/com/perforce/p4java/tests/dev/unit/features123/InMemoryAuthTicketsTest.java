/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
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
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test in-memory tickets.
 */
@Jobs({"job059814"})
@TestId("Dev123_InMemoryAuthTicketsTest")
public class InMemoryAuthTicketsTest extends P4JavaTestCase {

    private static Properties serverProps = new Properties();

    private static IClient client = null;
    private static String serverMessage = null;
    private static IOptionsServer superServer = null;
    private static IClient superClient = null;
    private static String superServerMessage = null;
    private static IUser newUser = null;


    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @BeforeClass
    public static void beforeAll() {
        // initialization code (before each test).

        try {
            // Tell the server to use memory to store auth tickets
            serverProps.put("useAuthMemoryStore", "true");

            server = getServer(getServerUrlString(), serverProps, getUserName(),
                    getPassword());
            assertNotNull(server);
            client = server.getClient("p4TestUserWS20112");
            assertNotNull(client);
            server.setCurrentClient(client);
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

            superServer = getServer(getServerUrlString(), serverProps,
                    getSuperUserName(), getSuperUserPassword());
            assertNotNull(superServer);
            superClient = superServer.getClient("p4TestSuperWS20112");
            superServer.setCurrentClient(superClient);
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

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @AfterClass
    public static void tearDown() {
        // cleanup code (after each test).
        afterEach(server);
        afterEach(superServer);
    }

    /**
     * Test in-memory tickets - login user.
     */
    @Test
    public void testInMemoryUserLogin() {

        try {
            // User's password has space and tab characters at the start and end
            server.setUserName("testuser-job059485");

            // Password with 2 tabs at the start and 2 spaces at the end
            server.login("		abc123  ");
            assertNotNull(serverMessage);
            assertTrue(serverMessage.contains("User testuser-job059485 logged in."));
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test in-memory tickets - change password.
     */
    @Test
    public void testInMemoryChangePassword() {
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
            try {
                server.login("");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("'login' not necessary, no password set for this user."));
            }

            // Change password
            String password1 = "		abc ' \" # @ 12' \" \" 3  ";
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
            superServer = getServer(this.getServerUrlString(), serverProps,
                    "p4jtestsuper", "p4jtestsuper");
            assertNotNull(superServer);
            superClient = superServer.getClient("p4TestSuperWS20112");
            superServer.setCurrentClient(superClient);
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
                        for (Iterator<String> it = userGroup.getUsers().iterator(); it.hasNext(); ) {
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
