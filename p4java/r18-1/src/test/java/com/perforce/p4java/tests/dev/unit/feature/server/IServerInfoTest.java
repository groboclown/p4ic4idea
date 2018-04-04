package com.perforce.p4java.tests.dev.unit.feature.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple very minimal tests for some of the IServerInfo fields.
 * Not a lot we can really test here in any case...
 * 
 * @testid IServerInfoTest01
 */

@TestId("IServerInfoTest01")
public class IServerInfoTest extends P4JavaTestCase {

	@Test
	public void testGetClientInfo() throws Exception {
		
		IServer server = null;
		IClient client = null;
		
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull("Null client returned", client);
			server.setCurrentClient(client);

			IServerInfo serverInfo = server.getServerInfo();		
	
			assertNotNull("Unexpected Null returned by serverInfo.getClientName()",
								serverInfo.getClientName());
			assertEquals("Client name mismatch",
			             getPlatformClientName(this.getDefaultTestClientName()),
						 serverInfo.getClientName());
			assertEquals("Client root mismatch", client.getRoot(), serverInfo.getClientRoot());
			assertFalse("unicode enabled on non-unicode-enabled server",
							serverInfo.isUnicodeEnabled());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());		
		}
	}
}
