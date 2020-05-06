/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test progress indicator (p4 -I <command>) using the streaming method.
 * Note that currently, only certain Perforce commands have progress indicator
 * support. And, it only works with Perforce server 12.2 or above.
 */
@Jobs({ "job057603" })
@TestId("Dev123_StreamingProgressIndicatorTest")
public class StreamingProgressIndicatorTest extends P4JavaRshTestCase     {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", StreamingProgressIndicatorTest.class.getSimpleName());

	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();
			props.put("enableProgress", "true");
		    setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, props);
			assertNotNull(server);
			client = getClient(server);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

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
	 * Test progress indicator (p4 -I <command>) using execStreamingMapCommand().
	 * 
	 * test "p4 -I sync -q"
	 */
	@Test
	public void testProgressIndicatorStreamingMapCmd() {

		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);
			server.execStreamingMapCommand(CmdSpec.SYNC.toString(), new String[] { "-q", "-f",
			"//depot/112Dev/CopyFilesTest/..." }, null, handler, key);
			assertNotNull(resultsList);
			assertTrue(resultsList.size() >= 1);
			assertNotNull(resultsList.get(resultsList.size()-1));
			assertTrue(resultsList.get(resultsList.size()-1).containsKey("handle"));
			assertNotNull(resultsList.get(resultsList.size()-1).get("handle"));
			assertEquals((String)resultsList.get(resultsList.size()-1).get("handle"), "progress");

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

	protected void fails(String msg) {
		fail(msg);
	}
}
