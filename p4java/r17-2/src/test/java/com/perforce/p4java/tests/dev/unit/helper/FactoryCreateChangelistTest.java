/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Basic Factory createChangelist tests.
 */

@TestId("Commons_FactoryCreateChangelistTest")
public class FactoryCreateChangelistTest extends P4JavaTestCase {

	public FactoryCreateChangelistTest() {
	}

	@Test
	public void testFactoryCreateChangelistDefault() {
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;
		
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull("default test client not found", client);
			server.setCurrentClient(client);
			changelist = CoreFactory.createChangelist(client, null, true);
			assertNotNull("null changelist returned from factory", changelist);
			assertTrue(changelist.getId() != IChangelist.UNKNOWN);
			IChangelist retChange = server.getChangelist(changelist.getId());
			assertNotNull("new changelist not found on server", retChange);
			assertNotNull("null description", retChange.getDescription());
			assertTrue("description mismatch", retChange.getDescription().startsWith(Changelist.DEFAULT_DESCRIPTION));
			assertEquals("owner mismatch", this.getUserName(), retChange.getUsername());
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				if ((server != null) && (changelist != null)) {
					server.deletePendingChangelist(changelist.getId());
				}
			} catch (Exception exc) {
				
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	@Test
	public void testFactoryCreateChangelistBasics() {
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;
		final String description = "Changelist " + this.getRandomInt()
										+ " for " + this.getTestId();
		
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull("default test client not found", client);
			server.setCurrentClient(client);
			changelist = CoreFactory.createChangelist(client, description, true);
			assertNotNull("null changelist returned from factory", changelist);
			assertTrue(changelist.getId() != IChangelist.UNKNOWN);
			IChangelist retChange = server.getChangelist(changelist.getId());
			assertNotNull("new changelist not found on server", retChange);
			assertNotNull("null description", retChange.getDescription());
			assertTrue("description mismatch", retChange.getDescription().startsWith(description));
			assertEquals("owner mismatch", this.getUserName(), retChange.getUsername());
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				if ((server != null) && (changelist != null)) {
					server.deletePendingChangelist(changelist.getId());
				}
			} catch (Exception exc) {
				
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
