package com.perforce.p4java.tests.dev.unit.feature.core;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

import junit.framework.Assert;

/**
 * Simple non-admin new IUser-based features.
 * 
 * @testid SimpleUserTest
 */

@TestId("SimpleUserTest01")
public class SimpleUserTest extends P4JavaTestCase {
	
	final static String testUserName = "p4jtestSimpleUserTest"; // user doesn't have a password
	final static String testUserPassword = "p4jtestSimpleUserTest";

	public SimpleUserTest() {
	}

	/**
	 * test the basic IServer.getUsers() method.
	 */
	
	@Test
	public void testGetUsers() {
		IServer server = null;
		
		try {
			server = getServer(this.serverUrlString, null, testUserName, "");
			assertNotNull("Null server returned", server);
			
			List<IUserSummary> users = server.getUsers(null, 0);
			assertNotNull("Null user list returned", users);
			assertTrue("Bad user list length: " + users.size(), users.size() > 5);
			boolean found = false;
			for (IUserSummary user : users) {
				assertNotNull("Null user in user summary list", user);
				assertNotNull("Null login name in user list", user.getLoginName());
				if (user.getLoginName().equals(testUserName)) {
					found = true;
				}
			}
			assertTrue("Standard user name '" + testUserName + "' not found in list", found);
			
			users = server.getUsers(null, 2);
			assertNotNull("null user list returned", users);
			assertTrue("zero-length user list returned", users.size() > 0);
			assertEquals("saw " + users.size() + " users", 2, users.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	@Test
	public void testGetUser() {
		IServer server = null;
		
		try {
			server = getServer(this.serverUrlString, null, testUserName, "");
			assertNotNull("Null server returned", server);
			
			IUser user = server.getUser(null);
			assertNotNull("Null user returned", user);
			assertEquals("User name mismatch",
								user.getLoginName(), server.getUserName());

			user = server.getUser(testUserName);
			assertNotNull("Null user returned", user);
			assertEquals("User name mismatch", testUserName, user.getLoginName());
			user = server.getUser(invalidUserName);
			assertNull("Non-null user returned for invalid user", user);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
    @Test
    public void testLoginNotRequired() {
        try {
            getServer(this.serverUrlString, null, testUserName, testUserPassword);
            fail("Expected exception!");
        } catch (Exception exc) {
            Assert.assertEquals("'login' not necessary, no password set for this user.",
                    exc.getMessage());
        }
    }
}
