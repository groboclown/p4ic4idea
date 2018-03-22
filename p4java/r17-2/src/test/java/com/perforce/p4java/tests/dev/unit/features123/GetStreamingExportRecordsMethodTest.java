/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test IOptionsServer.GetStreamingExportRecordsMethodTest(...) method.
 */
@Jobs({ "job040480" })
@TestId("Dev123_GetStreamingExportRecordsMethodTest")
public class GetStreamingExportRecordsMethodTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	Integer journal = 0;

	public static class SimpleCallbackHandler implements IStreamingCallback {
		int expectedKey = 0;
		GetStreamingExportRecordsMethodTest testCase = null;

		public SimpleCallbackHandler(GetStreamingExportRecordsMethodTest testCase,
				int key) {
			if (testCase == null) {
				throw new NullPointerException(
						"null testCase passed to CallbackHandler constructor");
			}
			this.expectedKey = key;
			this.testCase = testCase;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null result map in handleResult");
			}
			return true;
		}
	};

	public static class ListCallbackHandler implements IStreamingCallback {

		int expectedKey = 0;
		GetStreamingExportRecordsMethodTest testCase = null;
		List<Map<String, Object>> resultsList = null;

		public ListCallbackHandler(GetStreamingExportRecordsMethodTest testCase,
				int key, List<Map<String, Object>> resultsList) {
			this.expectedKey = key;
			this.testCase = testCase;
			this.resultsList = resultsList;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);
			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	};

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
			server = getServerAsSuper();
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
			journal = new Integer(server.getCounter("journal"));
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
	 * Test IOptionsServer.getStreamingExportRecords(...) method.<p>
	 * 
	 * No skipping data conversion.
	 */
	@Test
	public void testNoSkip() {

		try {

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);

			server.getStreamingExportRecords(new ExportRecordsOptions()
					.setMaxRecs(100000).setUseJournal(true).setSourceNum(journal)
					.setFilter("table=db.traits"), handler, key);
			
			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			Map<String, Object> dataMap = resultsList.get(0);
			assertNotNull(dataMap);
			Object dataObject = dataMap.get("TTvalue");
			assertNotNull(dataObject);
			assertTrue(dataObject instanceof String);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test IOptionsServer.getStreamingExportRecords(...) method.<p>
	 * 
	 * Skip field range.
	 */
	@Test
	public void testSkipFieldRange() {

		try {

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);

			server.getStreamingExportRecords(new ExportRecordsOptions()
			.setMaxRecs(100000).setUseJournal(true).setSourceNum(journal)
			.setFilter("table=db.traits").setSkipDataConversion(true)
			.setSkipStartField("op")
			.setSkipStopField("func"), handler, key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			Map<String, Object> dataMap = resultsList.get(0);
			assertNotNull(dataMap);
			Object dataObject = dataMap.get("TTvalue");
			assertNotNull(dataObject);
			assertTrue(dataObject instanceof byte[]);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test IOptionsServer.getStreamingExportRecords(...) method.<p>
	 * 
	 * Skip field pattern.
	 */
	@Test
	public void testSkipFieldPattern() {

		try {

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);

			server.getStreamingExportRecords(new ExportRecordsOptions()
			.setMaxRecs(100000).setUseJournal(true).setSourceNum(journal)
			.setFilter("table=db.traits").setSkipDataConversion(true)
			.setSkipFieldPattern("^[A-Z]{2}\\w+"), handler, key);
			
			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			Map<String, Object> dataMap = resultsList.get(0);
			assertNotNull(dataMap);
			Object dataObject = dataMap.get("TTvalue");
			assertNotNull(dataObject);
			assertTrue(dataObject instanceof byte[]);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
