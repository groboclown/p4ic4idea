/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for successful logins with very long passwords.
 */
@TestId("Bugs111_Job043468Test")
public class Job043468Test extends P4JavaTestCase {

	public Job043468Test() {
	}

	@Test
	public void testLongPasswords() {
		IOptionsServer server = null;
		IOptionsServer server2 = null;
		IUser user = null;
		final String userName = "p4jtestjob043468";
		final String fullName = "Test user created by junit test " + this.getTestId();
		final String userPassword = "infeasiblylongpasswordinfeasiblylongpassword";
		final String expectedStatus = "User " + userName + " saved.";
		
		try {
			server = this.getServerAsSuper();
			user = User.newUser(userName, "invalid@invalid.invalid", fullName, userPassword);
			assertNotNull(user);
			String createStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			assertNotNull("null status string from createUser", createStr);
			assertEquals("user not created on server: " + createStr, expectedStatus, createStr);
			
			server2 = getServer(serverUrlString, null, null, null);
			assertNotNull(server2);
			server2.setUserName(userName);
			server2.login(userPassword, new LoginOptions());
			server2.logout();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if ((server != null) && (user != null)) {
				try {
					server.deleteUser(userName, new UpdateUserOptions().setForceUpdate(true));
				} catch (P4JavaException e) {
					// ignore
				}
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
