/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Very basic tests to see if setting the command name checking
 * option mentioned in job035253 is "effective", where "effective"
 * is very loosely defined as "the command is passed on to the
 * server".
 */

@TestId("Features102_Job035253Test")
public class Job035253Test extends P4JavaTestCase {
	
	private final static String FAKE_CMD = "zzyzx"; // and why not?!
	private final static String EXPECTED_MSG = "command name '" + FAKE_CMD + "' unimplemented";
	private final static String PING_CMD = "ping";

	public Job035253Test() {
	}

	@Test
	public void testRelaxedCommandNameChecking() {
		IOptionsServer server = null;
		IOptionsServer server2 = null;
		
		try {
			Properties props = new Properties();
			server = getServer(this.getServerUrlString(), props,
								this.getUserName(), this.getPassword());
			
			try {
				Map<String, Object>[] results = server.execMapCmd(FAKE_CMD, new String[] {}, null);
				assertNotNull(results);
				fail("did not receive expected exception");
			} catch (RequestException exc) {
				assertNotNull(exc.getLocalizedMessage());
				assertTrue(exc.getLocalizedMessage().startsWith(EXPECTED_MSG));
			}
			
			props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
			server2 = getServer(this.getServerUrlString(), props,
					this.getUserName(), this.getPassword());
			try {
				Map<String, Object>[] results = server2.execMapCmd(FAKE_CMD, new String[] {}, null);
				assertNotNull(results);
				assertEquals("wrong size in results array", 1, results.length);
				String errMsg = ((com.perforce.p4java.impl.mapbased.server.Server) server2)
									.getErrorStr(results[0]);
				assertNotNull("null error message from server", errMsg);
				assertTrue("did not see expected server error message",
								errMsg.startsWith("Unknown command."));	// That'll do...
			} catch (Throwable exc) {
				assertNotNull(exc.getLocalizedMessage());
				assertTrue(exc.getLocalizedMessage().startsWith(EXPECTED_MSG));
			}
			
			// Now try with a real command; should get an access error
			// with 'ping':
			
			try {
				Map<String, Object>[] results = server2.execMapCmd(PING_CMD, new String[] {}, null);
				assertNotNull(results);
				assertEquals("wrong size in results array", 1, results.length);
				String errMsg = ((com.perforce.p4java.impl.mapbased.server.Server) server2)
									.getErrorStr(results[0]);
				assertNotNull("null error message from server", errMsg);
				assertTrue("did not see expected server error message",
								errMsg.startsWith("You don't have permission for this operation"));
			} catch (Throwable exc) {
				assertNotNull(exc.getLocalizedMessage());
				assertTrue(exc.getLocalizedMessage().startsWith(EXPECTED_MSG));
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
			if (server2 != null) {
				this.endServerSession(server2);
			}
		}
	}
}
