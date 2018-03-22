/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test progress indicator (p4 -I <command>) using the streaming method.
 * Note that currently, only certain Perforce commands have progress indicator
 * support. And, it only works with Perforce server 12.2 or above.
 */
@Jobs({ "job057603" })
@TestId("Dev123_StreamingProgressIndicatorTest")
public class StreamingProgressIndicatorTest extends P4JavaTestCase {

	public static class SimpleCallbackHandler implements IStreamingCallback {
		int expectedKey = 0;
		StreamingProgressIndicatorTest testCase = null;

		public SimpleCallbackHandler(StreamingProgressIndicatorTest testCase, int key) {
			if (testCase == null) {
				throw new NullPointerException(
						"null testCase passed to CallbackHandler constructor");
			}
			this.expectedKey = key;
			this.testCase = testCase;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			if (resultMap == null) {
				this.testCase.fails("null result map in handleResult");
			}
			return true;
		}
	};

	public static class ListCallbackHandler implements IStreamingCallback {

		int expectedKey = 0;
		StreamingProgressIndicatorTest testCase = null;
		List<Map<String, Object>> resultsList = null;

		public ListCallbackHandler(StreamingProgressIndicatorTest testCase, int key,
				List<Map<String, Object>> resultsList) {
			this.expectedKey = key;
			this.testCase = testCase;
			this.resultsList = resultsList;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			if (resultMap == null) {
				this.testCase
						.fails("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);
			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	};

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();

			props.put("enableProgress", "true");

			server = ServerFactory
					.getOptionsServer(this.serverUrlString, props);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					completedTime = millisecsTaken;
				}
			});
			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set the server user
			server.setUserName(this.userName);

			// Login using the normal method
			server.login(this.password, new LoginOptions());

			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
