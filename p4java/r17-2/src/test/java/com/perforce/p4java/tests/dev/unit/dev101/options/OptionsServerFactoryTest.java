/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple test scaffold for 10.1 IOptionsServer development. Probably
 * not a particularly useful test under normal conditions.
 */

@Jobs({"job039408"})
@TestId("Dev101_OptionsServerFactoryTest")
public class OptionsServerFactoryTest extends P4JavaTestCase {
	
	public OptionsServerFactoryTest() {
		super();
	}

	@Test
	public void testServerFactoryBasics() {
		try {
			IServer server = ServerFactory.getServer(this.serverUrlString, null);
			assertNotNull(server);
			server.connect();
			server.disconnect();
			
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull(server);
			server.connect();
			server.disconnect();
			
			IOptionsServer optsServer = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull(optsServer);
			optsServer.connect();
			optsServer.disconnect();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
