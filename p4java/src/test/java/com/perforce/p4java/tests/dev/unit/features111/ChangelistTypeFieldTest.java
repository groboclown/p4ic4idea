/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import com.perforce.p4java.tests.dev.UnitTestDevServerManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IChangelistSummary.Visibility;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests implementation of the changelist type field. Not to be
 * confused with the IChangelist 'Type' type...
 */

@TestId("Features111_ChangelistTypeFieldTest")
public class ChangelistTypeFieldTest extends P4JavaTestCase {

	public ChangelistTypeFieldTest() {
	}

	// p4ic4idea: use local server
	@BeforeClass
	public static void oneTimeSetUp() {
		UnitTestDevServerManager.INSTANCE.startTestClass();
	}
	@AfterClass
	public static void oneTimeTearDown() {
		UnitTestDevServerManager.INSTANCE.endTestClass();
	}

	/**
	 * Looks for a known restricted changelist; will fail if
	 * this moves or is deleted, etc.
	 */
	@Test
	public void testChangelistTypeFieldUsageBasics() {
		IOptionsServer server = null;
		// p4ic4idea: mock server setup has different changelist number (was: 20910)
		final int restrictedChangelistId = 1;
		final String expectedDescription = "<description: restricted, no permission to view>\n";
		
		try {
			server = getServer();
			IChangelist changelist = server.getChangelist(restrictedChangelistId);
			assertNotNull("can't find restricted changelist", changelist);
			assertEquals("visibility mismatch", Visibility.RESTRICTED, changelist.getVisibility());
			assertEquals("description mismatch", expectedDescription, changelist.getDescription());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	@Test
	public void testCreateRestrictedChangelist() {
		IOptionsServer server = null;
		IOptionsServer server2 = null;
		IClient client = null;
		final String description = "Changelist for test " + getTestId();
		IChangelist changelist = null;
		final String expectedDescription = "<description: restricted, no permission to view>\n";
		
		try {
			server = getServer(null, this.getSuperUserName(),
											this.getSuperUserPassword());
			assertNotNull("null super-user server", server);
			client = getDefaultClient(server);
			assertNotNull("null super-user client", client);
			server.setCurrentClient(client);
			changelist = Changelist.newChangelist(client, description);
			assertNotNull("unable to create new changelist", changelist);
			changelist.setVisibility(Visibility.RESTRICTED);
			changelist = client.createChangelist(changelist);
			assertNotNull("null changelist returned after creation", changelist);
			assertEquals("super-user changelist visibility mismatch",
											Visibility.RESTRICTED, changelist.getVisibility());
			
			// Now see if we can get it back as normal user...
			
			server2 = getServer();
			IChangelist changelist2 = server2.getChangelist(changelist.getId());
			assertNotNull("unable to retrieve changelist", changelist2);
			assertEquals("visibility mismatch", Visibility.RESTRICTED, changelist2.getVisibility());
			assertEquals("description mismatch", expectedDescription, changelist2.getDescription());
			
			// See if it turns up properly in a summary list:
			
			List<IChangelistSummary> changes = server.getChangelists(null,
											new GetChangelistsOptions().setMaxMostRecent(20));
			assertNotNull(changes);
			boolean found = false;
			for (IChangelistSummary change : changes) {
				assertNotNull("null change in changes list", change);
				if (change.getId() == changelist2.getId()) {
					found = true;
					assertEquals("summary change visibility mismatch",
									Visibility.RESTRICTED, change.getVisibility());
				}
			}
			assertTrue("changelist not found", found);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if ((server != null) && (changelist != null)) {
				try {
					server.deletePendingChangelist(changelist.getId());
				} catch (Exception e) {
					// ignore
				}
			}
			if (server != null) {
				this.endServerSession(server);
			}
			if (server2 != null) {
				this.endServerSession(server);
			}
		}
	}
}
