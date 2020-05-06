/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static com.perforce.p4java.tests.ServerMessageMatcher.startsWithText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServerMessage;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features121.GetStreamOptionsTest;

/**
 * Very basic tests to see if setting the command name checking
 * option mentioned in job035253 is "effective", where "effective"
 * is very loosely defined as "the command is passed on to the
 * server".
 */

@TestId("Features102_Job035253Test")
public class Job035253Test extends P4JavaRshTestCase {
	
	private final static String FAKE_CMD = "zzyzx"; // and why not?!
	private final static String EXPECTED_MSG = "command name '" + FAKE_CMD + "' unimplemented";
	private final static String PING_CMD = "ping";
	IOptionsServer server2 = null;

	public Job035253Test() {
	}

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job035253Test.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            Properties properties = new Properties();
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
            assertNotNull(server);
            props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
            server2 = getServer(p4d.getRSHURL(), props, "p4jtestuser", "p4jtestuser");
            assertNotNull(server2);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

	@Test
	public void testRelaxedCommandNameChecking() {
		
		try {
			try {
				Map<String, Object>[] results = server.execMapCmd(FAKE_CMD, new String[] {}, null);
				assertNotNull(results);
				fail("did not receive expected exception");
			} catch (RequestException exc) {
				assertNotNull(exc.getLocalizedMessage());
				assertTrue(exc.getLocalizedMessage().startsWith(EXPECTED_MSG));
			}
			try {
				Map<String, Object>[] results = server2.execMapCmd(FAKE_CMD, new String[] {}, null);
				assertNotNull(results);
				assertEquals("wrong size in results array", 1, results.length);
				IServerMessage errMsg = ((Server) server2)
									.getErrorStr(results[0]);
				assertNotNull("null error message from server", errMsg);
				assertThat("did not see expected server error message", errMsg,
								startsWithText("Unknown command."));	// That'll do...
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
				IServerMessage errMsg = ((com.perforce.p4java.impl.mapbased.server.Server) server2)
									.getErrorStr(results[0]);
				assertNotNull("null error message from server", errMsg);
				assertThat("did not see expected server error message", errMsg,
								startsWithText("You don't have permission for this operation"));
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
				this.endServerSession(server);
			}
		}
	}
}
