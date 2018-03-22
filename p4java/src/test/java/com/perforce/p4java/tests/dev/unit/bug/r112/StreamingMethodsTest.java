/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test deep (-I) annotate streaming command. Verify the values for the 'upper0'
 * and 'lower0' fields.
 */
@Jobs({ "job046042" })
@TestId("Dev112_StreamingMethodsTest")
public class StreamingMethodsTest extends P4JavaTestCase {

	public static class SimpleCallbackHandler implements IStreamingCallback {
		int expectedKey = 0;
		StreamingMethodsTest testCase = null;

		public SimpleCallbackHandler(StreamingMethodsTest testCase, int key) {
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
		StreamingMethodsTest testCase = null;
		List<Map<String, Object>> resultsList = null;

		public ListCallbackHandler(StreamingMethodsTest testCase, int key,
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

	public StreamingMethodsTest() {
	}

	/**
	 * Test deep (-I) annotate streaming command. Verify the values for the
	 * 'upper0' and 'lower0' fields.
	 */
	@Test
	public void testAnnotateStreamingCommand() {
		IOptionsServer server = null;
		String depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java#3";

		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			server = getServer();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);
			server.execStreamingMapCommand("annotate", new String[] { "-I",
					depotFile }, null, handler, key);
			assertTrue(resultsList.size() > 1);

			// The 'upper0' value should be "3" and 'lower0' value should be "1"
			assertTrue(((String) resultsList.get(1).get("upper0"))
					.contentEquals("3"));
			assertTrue(((String) resultsList.get(1).get("lower0"))
					.contentEquals("1"));

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
