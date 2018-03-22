/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple tests for the 11.1 PasswordTimeout feature. Not
 * intended to be comprehensive... and doesn't test group creation,
 * as that's a real pain to clean up after (and it's basically tested
 * elsewhere in CreatDeleteUserGroupTest).
 */

@TestId("Features111_UserGroupPasswordTimeoutTest")
public class UserGroupPasswordTimeoutTest extends P4JavaTestCase {

	public UserGroupPasswordTimeoutTest() {
	}
	
	@Test
	public void testUserGroupPasswordTimeoutBasics() {
		IOptionsServer server = null;
		final String groupName01 = "p4jtestgroup";
		final String groupName02 = "p4jtestgroup01"; // confusing, I know...
		final int group01PWTimeout = IUserGroup.UNLIMITED;
		final int group02PWTimeout = 43002;
		
		try {
			server = getServer();
			// First just test that nothing blows up and that the fields
			// are correctly set on list retrieval...
			List<IUserGroup> userGroups = server.getUserGroups(null, new GetUserGroupsOptions());
			assertNotNull("null user grpoup list", userGroups);
			assertTrue("too few user groups in list", userGroups.size() > 1);
			boolean found01 = false;
			boolean found02 = false;
			for (IUserGroup group : userGroups) {
				assertNotNull("null user group in list", group);
				if (groupName01.equals(group.getName())) {
					found01 = true;
					assertEquals("password timeout mismatch in list",
							group01PWTimeout, group.getPasswordTimeout());
				}
				if (groupName02.equals(group.getName())) {
					found02 = true;
					assertEquals("password timeout mismatch in list",
							group02PWTimeout, group.getPasswordTimeout());
				}
			}
			assertTrue("didn't find first group", found01);
			assertTrue("didn't find second group", found02);
			// Try an individual retrieval:
			
			IUserGroup group = server.getUserGroup(groupName02);
			assertNotNull("unable to retrieve individual group", group);
			assertEquals("password timeout mismatch in individual retrieval",
							group02PWTimeout, group.getPasswordTimeout());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

}
