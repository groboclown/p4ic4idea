/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Very basic user group create / delete test cycle. Mainly in
 * response to job037102; may expand this later...
 * 
 * @testid CreatDeleteUserGroupTest
 * @job 
 */

@TestId("CreatDeleteUserGroupTest")
@Jobs({"job037102"})
public class CreatDeleteUserGroupTest extends P4JavaTestCase {

	@Test
	public void testCreatDelete() {
		IServer server = null;
		String groupName = this.getRandomName("Group");
		try {
			server = getServerAsSuper();
			assertNotNull("Null server returned");
			UserGroup newUserGroup = new UserGroup();
			newUserGroup.setName(groupName);
			List<String>groupOwners = new ArrayList<String>();
			groupOwners.add(this.getUserName());
			groupOwners.add(this.getSuperUserName());
			newUserGroup.setOwners(groupOwners);
			List<String>groupUsers= new ArrayList<String>();
			groupUsers.add(this.getUserName());
			groupUsers.add(this.getSuperUserName());
			groupUsers.add(this.getInvalidUserName());
			newUserGroup.setUsers(groupUsers);
			newUserGroup.setMaxLockTime(100);
			newUserGroup.setMaxResults(500);
			newUserGroup.setMaxScanRows(1000);
			newUserGroup.setSubGroup(false);
			newUserGroup.setTimeout(10000);
			newUserGroup.setServer(server);
			newUserGroup.setPasswordTimeout(IUserGroup.UNLIMITED);
			server.createUserGroup(newUserGroup);
			
			IUserGroup retrievedGroup = server.getUserGroup(groupName);
			assertNotNull("Unable to retrieve new group '" + groupName + "'", retrievedGroup);
			assertEquals(IUserGroup.UNLIMITED, retrievedGroup.getPasswordTimeout());
			String deleteResult = server.deleteUserGroup(retrievedGroup);
			assertNotNull(deleteResult);
			assertTrue(deleteResult.endsWith("deleted."));
		} catch (Exception exc) {
			exc.printStackTrace();
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
