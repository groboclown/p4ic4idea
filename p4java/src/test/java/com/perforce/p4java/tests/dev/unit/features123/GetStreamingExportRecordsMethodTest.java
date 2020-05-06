/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.option.server.ExportRecordsOptions;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test IOptionsServer.GetStreamingExportRecordsMethodTest(...) method.
 */
@Jobs({ "job040480" })
@TestId("Dev123_GetStreamingExportRecordsMethodTest")
public class GetStreamingExportRecordsMethodTest extends P4JavaRshTestCase {

	IClient client = null;
	Integer journal = 0;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetStreamingExportRecordsMethodTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
			setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", false, props);
			assertNotNull(server);
			client = createClient(server, "GetStreamingExportRecordsMethodTestClient");
			assertNotNull(client);
			server.setCurrentClient(client);
			journal = new Integer(server.getCounter("journal"));
			p4d.rotateJournal();
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
					.setFilter("table=db.counters"), handler, key);
			
			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			Map<String, Object> dataMap = resultsList.get(0);
			assertNotNull(dataMap);
			Object dataObject = dataMap.get("COvalue");
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
			.setFilter("table=db.counters").setSkipDataConversion(true)
			.setSkipStartField("op")
			.setSkipStopField("func"), handler, key);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			Map<String, Object> dataMap = resultsList.get(0);
			assertNotNull(dataMap);
			Object dataObject = dataMap.get("COvalue");
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
			.setFilter("table=db.counters").setSkipDataConversion(true)
			.setSkipFieldPattern("^[A-Z]{2}\\w+"), handler, key);
			
			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);

			Map<String, Object> dataMap = resultsList.get(0);
			assertNotNull(dataMap);
			Object dataObject = dataMap.get("COvalue");
			assertNotNull(dataObject);
			assertTrue(dataObject instanceof byte[]);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
