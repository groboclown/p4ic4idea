package com.perforce.p4java.tests.dev.unit.features151;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

/**
 * Test 'rsh' hack
 */
@Jobs({ "job034706" })
@TestId("Dev151_RshTest")
@Ignore
public class RshTest extends P4JavaRshTestCase {

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test 'rsh' hack.
	 */
	@Test
	public void testRsh() {

		try {
			File p4root = new File("p4root");
			p4root.mkdir();
		
			IOptionsServer p4 = RshConnection.getRshConnection(p4root.getAbsolutePath());
			p4.connect();
		
			IServerInfo info = p4.getServerInfo();
            debugPrint("Info from RSH connection " + info);
			
			p4.disconnect();
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
