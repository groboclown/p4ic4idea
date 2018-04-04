/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests very simple client deletes as normal user and super user.
 * Based on job036916 which turned out to be a non-bug anyway....
 * 
 * @testid ClientDeleteTest
 * @job job036916
 */

@TestId("ClientDeleteTest")
@Jobs({"job036916"})
public class ClientDeleteTest extends P4JavaTestCase {

	@Test
	public void testAsNormalUser() {
		IServer server = null;
		IClient client = null;
		final String excStr = "You don't have permission for this operation";
		
		try {
			server = getServer();
			assertNotNull("Null server returned", server);
			Client clientImpl = makeTempClient(null, server);
			assertNotNull("Null client impl", clientImpl);
			String rsltStr = server.createClient(clientImpl);
			assertNotNull(rsltStr);
			client = server.getClient(clientImpl.getName());
			assertNotNull("couldn't retrieve new client", client);
			rsltStr = server.deleteClient(client.getName(), false);
			assertNotNull(rsltStr);
			clientImpl = makeTempClient(null, server);
			assertNotNull("Null client impl", clientImpl);
			rsltStr = server.createClient(clientImpl);
			assertNotNull(rsltStr);
			client = server.getClient(clientImpl.getName());
			assertNotNull("couldn't retrieve new client", client);
			boolean caughtException = false;
			try {
				// This delete should fail when run as a normal user:
				
				rsltStr = server.deleteClient(client.getName(), true);
			} catch (RequestException rexc) {
				assertTrue(rexc.getMessage().contains(excStr));
				caughtException = true;
			}
			assertTrue(caughtException);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			
		}
	}
	
	@Test
	public void testAsSuperUser() {
		IServer server = null;
		IClient client = null;
		
		try {
			server = getServerAsSuper();
			assertNotNull("Null server returned", server);
			Client clientImpl = makeTempClient(null, server);
			assertNotNull("Null client impl", clientImpl);
			clientImpl.setOwnerName(getSuperUserName());
			String rsltStr = server.createClient(clientImpl);
			assertNotNull(rsltStr);
			client = server.getClient(clientImpl.getName());
			assertNotNull("couldn't retrieve new client", client);
			rsltStr = server.deleteClient(client.getName(), false);
			assertNotNull(rsltStr);
			clientImpl = makeTempClient(null, server);
			assertNotNull("Null client impl", clientImpl);
			clientImpl.setOwnerName(getSuperUserName());
			rsltStr = server.createClient(clientImpl);
			assertNotNull(rsltStr);
			client = server.getClient(clientImpl.getName());
			assertNotNull("couldn't retrieve new client", client);
			rsltStr = server.deleteClient(client.getName(), true);
			assertNotNull(rsltStr);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			
		}
	}
}
