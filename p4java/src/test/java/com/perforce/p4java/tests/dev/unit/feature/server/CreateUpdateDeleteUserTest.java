/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.server;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Simple test of server create / update / delete user methods. Not
 * intended to be encyclopedic.
 */
@TestId("Server_CreateUpdateDeleteUserTest")
public class CreateUpdateDeleteUserTest extends P4JavaRshTestCase {

	public CreateUpdateDeleteUserTest() {
	}

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", CreateUpdateDeleteUserTest.class.getSimpleName());

	@Test
	public void testCreateUpdateDeleteUserNewStyle() {
		doTests(false);
		doTests(true);
	}
	
	protected void doTests(boolean useOptions) {
		final int maxTries = 5;
		IOptionsServer server = null;
		IUser user = null;
		try {
			final String email = "p4jtest@invalid.invalid";
			final String fullName = "CreateUpdateDeleteUserTest test user";
			final String password = "password";
			final String jobView1 = "type=bug & ^status=closed";
			final String jobView2 = "priority<=b description=gui";
			
			server = getSuperConnection(p4d.getRSHURL());
			String userName = null;
			String opResultStr = null;
			// Try to ensure it's not already in use:
			for (int i = maxTries; i > 0; i--) {
				userName = this.getRandomName(false, "user");
				assertNotNull(userName);
				user = server.getUser(userName);
				if (user == null) break;
			}
			assertNull("Can't find unique user name after " + maxTries + " tries", user);
			
			user = new User(userName, email, fullName, null, null, password, jobView1, null);
			if (useOptions) {
				opResultStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			} else {
				opResultStr = server.createUser(user, true);
			}
			assertNotNull(opResultStr);
			assertEquals("User " + userName + " saved.", opResultStr);	// This could break sometime...
			
			user = server.getUser(userName);
			assertNotNull(user);
			assertEquals(userName, user.getLoginName());
			assertEquals(email, user.getEmail());
			assertEquals(fullName, user.getFullName());
			assertEquals(jobView1, user.getJobView());
			assertNotNull(user.getUpdate());
			
			user.setJobView(jobView2);
			if (useOptions) {
				server.updateUser(user, new UpdateUserOptions(true));
			} else {
				server.updateUser(user, true);
			}
			assertNotNull(opResultStr);
			assertEquals("User " + userName + " saved.", opResultStr);	// This could break sometime...
			user = server.getUser(userName);
			assertNotNull(user);
			assertEquals(userName, user.getLoginName());
			assertEquals(email, user.getEmail());
			assertEquals(fullName, user.getFullName());
			assertEquals(jobView2, user.getJobView());
			assertNotNull(user.getUpdate());
			
			if (useOptions) {
				opResultStr = server.deleteUser(userName, new UpdateUserOptions(true));
			} else {
				opResultStr = server.deleteUser(userName, true);
			}
			assertNotNull(opResultStr);
			assertEquals("User " + userName + " deleted.", opResultStr);	// This could break sometime...
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
