/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple test of create / delete Perforce users.
 * Requires super user login setup.
 * 
 * @testid CreateDeleteUser01
 */

@TestId("CreateDeleteUser01")
public class CreateDeleteUser extends P4JavaTestCase {

	@Test
	public void testUserCreateDelete() {
		IServer server = null;
		
		try {
			server = getServerAsSuper();
			assertNotNull("Null server returned", server);
			
			int randNum = (new Random()).nextInt(15000); // Good enough, I hope...
			String newUserName = testId + randNum;
			String email = newUserName + "@invalid.invalid";
			String fullName = testId + "New P4Java Test User " + randNum;
			IUser newUser = new User(newUserName, email, fullName, null, null, null, null, null);
			server.createUser(newUser, true);
			
			IUser retrievedUser = server.getUser(newUserName);
			assertNotNull("Unable to find new user: " + newUserName, retrievedUser);
			assertEquals("user type mismatch", UserType.STANDARD, retrievedUser.getType());

			retrievedUser.setEmail("testEmail@invalid.invalid");
			retrievedUser.update(true);
			
			retrievedUser = server.getUser(newUserName);
			assertNotNull("Unable to find updated user: " + newUserName, retrievedUser);
			
			assertEquals(retrievedUser.getEmail(), "testEmail@invalid.invalid");
			assertEquals("user type mismatch", UserType.STANDARD, retrievedUser.getType());
			server.deleteUser(newUserName, true);
			retrievedUser = server.getUser(newUserName);
			assertNull("Found deleted user: " + newUserName, retrievedUser);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	@Test
	public void testUserCreateDeleteServiceUser() {
		IServer server = null;
		
		try {
			server = getServerAsSuper();
			assertNotNull("Null server returned", server);
			
			int randNum = (new Random()).nextInt(15000); // Good enough, I hope...
			String newUserName = testId + randNum;
			String email = newUserName + "@invalid.invalid";
			String fullName = testId + "New P4Java Test User " + randNum;
			IUser newUser = new User(newUserName, email, fullName, null, null, null, null,
											UserType.SERVICE, null);
			server.createUser(newUser, true);
			
			IUser retrievedUser = server.getUser(newUserName);
			assertNotNull("Unable to find new user: " + newUserName, retrievedUser);
			assertEquals("user type mismatch", UserType.SERVICE, retrievedUser.getType());
			
			retrievedUser.setEmail("testEmail@invalid.invalid");
			retrievedUser.update(true);
			
			retrievedUser = server.getUser(newUserName);
			assertNotNull("Unable to find updated user: " + newUserName, retrievedUser);
			
			assertEquals(retrievedUser.getEmail(), "testEmail@invalid.invalid");
			assertEquals("user type mismatch", UserType.SERVICE, retrievedUser.getType());
			server.deleteUser(newUserName, true);
			retrievedUser = server.getUser(newUserName);
			assertNull("Found deleted user: " + newUserName, retrievedUser);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	@Test
	public void testRetrieveServiceUsers() {
		IOptionsServer server = null;
		final String serviceUserName = "p4jtestserviceuser";
		
		try {
			server = getServer();
			assertNotNull("Null server returned", server);
			List<IUserSummary> users = server.getUsers(null,
												new GetUsersOptions().setIncludeServiceUsers(false));
			assertNotNull("null user list returned", users);
			for (IUserSummary user : users) {
				assertNotNull("null user in user list", user);
				assertFalse("found service user", serviceUserName.equalsIgnoreCase(user.getLoginName()));
			}
			
			users = server.getUsers(null,
					new GetUsersOptions().setIncludeServiceUsers(true));
			assertNotNull("null user list returned", users);
			boolean found = false;
			for (IUserSummary user : users) {
				assertNotNull("null user in user list", user);
				if (serviceUserName.equalsIgnoreCase(user.getLoginName())) {
					found = true;
				}
			}
			assertTrue("service user not found in list", found);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	@Test
	public void testRetrieveUsersLongInfo() {
		IOptionsServer server = null;
		List<String> userNames = new ArrayList<String>();

		try {
			userNames.add("p4jtestsuper");
			userNames.add("p4jtestdummy");
			server = getServerAsSuper();
			assertNotNull("Null server returned", server);
			List<IUserSummary> users = server.getUsers(userNames,
												new GetUsersOptions().setExtendedOutput(true));
			assertNotNull("null user list returned", users);
			for (IUserSummary user : users) {
				assertNotNull("null user in user list", user);
				assertNotNull(user.getLoginName());
				if (user.getLoginName().equalsIgnoreCase("p4jtestsuper")) {
					assertNotNull("null password change field", user.getPasswordChange());
					assertNotNull("null ticket expiration field", user.getTicketExpiration());
				} else if (user.getLoginName().equalsIgnoreCase("p4jtestdummy")) {
					assertNull("non-null password change field", user.getPasswordChange());
					assertNull("non-null ticket expiration field", user.getTicketExpiration());
				} else {
					fail("user not in input list returned");
				}
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
