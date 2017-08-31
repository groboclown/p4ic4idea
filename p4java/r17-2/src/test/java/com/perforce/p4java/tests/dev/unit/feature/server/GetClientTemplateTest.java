/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple tests for the IServer.getClientTemplate methods.
 */
@TestId("Server_GetClientTemplateTest")
public class GetClientTemplateTest extends P4JavaTestCase {

	public GetClientTemplateTest() {
	}

	@Test
	public void testGetClientTemplates() {
		IOptionsServer server = null;
		IClient client = null;
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			IClient clientTemplate = server.getClientTemplate(client.getName(), false);
			assertNull(clientTemplate);
			clientTemplate = server.getClientTemplate(client.getName(), true);
			assertNotNull(clientTemplate);
			clientTemplate = server.getClientTemplate(this.getRandomClientName("XyZ"), false);
			assertNotNull(clientTemplate);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
