/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.client;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests very simple client deletes as normal user and super user.
 * Based on job036916 which turned out to be a non-bug anyway....
 * 
 * @testid ClientDeleteTest
 * @job job036916
 */

@TestId("ClientDeleteTest")
@Jobs({"job036916"})
public class ClientDeleteTest extends P4JavaRshTestCase {

    IOptionsServer superServer =null;
    
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ClientDeleteTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, props);
            assertNotNull(server);
           superServer = getSuperConnection(p4d.getRSHURL());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
    
	@Test
	public void testAsNormalUser() {
		IClient client = null;
		final String excStr = "You don't have permission for this operation";
		
		try {
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
		IClient client = null;
		
		try {
			assertNotNull("Null server returned", superServer);
			Client clientImpl = makeTempClient(null, superServer);
			assertNotNull("Null client impl", clientImpl);
			clientImpl.setOwnerName(getSuperUserName());
			String rsltStr = superServer.createClient(clientImpl);
			assertNotNull(rsltStr);
			client = superServer.getClient(clientImpl.getName());
			assertNotNull("couldn't retrieve new client", client);
			rsltStr = superServer.deleteClient(client.getName(), false);
			assertNotNull(rsltStr);
			clientImpl = makeTempClient(null, superServer);
			assertNotNull("Null client impl", clientImpl);
			clientImpl.setOwnerName(getSuperUserName());
			rsltStr = superServer.createClient(clientImpl);
			assertNotNull(rsltStr);
			client = superServer.getClient(clientImpl.getName());
			assertNotNull("couldn't retrieve new client", client);
			rsltStr = superServer.deleteClient(client.getName(), true);
			assertNotNull(rsltStr);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			
		}
	}
}
