/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Basic tests for the P4Java commons Factory class. Transitively tests
 * underlying class-specific factory methods as well.
 */
@TestId("Commons_FactoryCreateClientTest")
public class FactoryCreateClientTest extends P4JavaTestCase {

	public FactoryCreateClientTest() {
	}

	@Test
	public void testCreatClientDefaults() {
		IOptionsServer server = null;
		String clientName = this.getRandomClientName(this.getLocalHostName());
		IClient client = null;
		
		try {
			server = getServer();
			
			client = CoreFactory.createClient(server, clientName, null, null, null, true);
			assertNotNull(client);
			IClient retClient = server.getClient(clientName);
			assertNotNull("client not found on server", retClient);
			assertEquals("description mismatch", Client.DEFAULT_DESCRIPTION, retClient.getDescription());
			assertNotNull("null root in created client", retClient.getRoot());
			assertEquals("user / owner name mismatch", this.getUserName(), retClient.getOwnerName());
			ClientView clientView = retClient.getClientView();
			assertNotNull("null client view in retrieved client", clientView);
			assertEquals("default client view had more than 1 mapping", 1, clientView.getSize());
			assertNotNull(clientView.getEntryList());
			assertNotNull(clientView.getEntryList().get(0));
			assertEquals("mapping entry wrong in default mapping",
						"//depot/... " + "//" + clientName + "/depot/...",
						clientView.getEntryList().get(0).toString());
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				server.deleteClient(clientName, null);
			} catch (Exception exc) {
				
			}
			this.endServerSession(server);
		}
	}
	
	@Test
	public void testCreateClientBasics() {
		IOptionsServer server = null;
		String clientName = this.getRandomClientName(this.getLocalHostName());
		IClient client = null;
		final String[] mapping =  {
					"//depot/dev/... //" + clientName + "/depot/dev/...",
					"//depot/jobs/... //" + clientName + "/jobs/...",
					"//remote/... //" + clientName + "/remotework/..."
				};
		final String description = "Test description " + clientName;
		final String root = "/tmp/" + clientName;
		
		try {
			server = getServer();
			
			client = CoreFactory.createClient(server, clientName, description, root, mapping, true);
			assertNotNull(client);
			IClient retClient = server.getClient(clientName);
			assertNotNull("client not found on server", retClient);
			assertEquals("description mismatch", description, retClient.getDescription());
			assertEquals("root mismatch", root, retClient.getRoot());
			assertEquals("user / owner name mismatch", this.getUserName(), retClient.getOwnerName());
			ClientView clientView = retClient.getClientView();
			assertNotNull("null client view in retrieved client", clientView);
			assertEquals("default client view had wrong number of mappings",
										mapping.length, clientView.getSize());
			assertNotNull(clientView.getEntryList());
			for (int i = 0; i < mapping.length; i++) {
				assertNotNull(clientView.getEntryList().get(i));
				assertEquals("mapping mismatch",
								mapping[i], clientView.getEntryList().get(i).toString());
			}
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			try {
				server.deleteClient(clientName, null);
			} catch (Exception exc) {
				
			}
			this.endServerSession(server);
		}
	}
}
