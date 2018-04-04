/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 export' command. In particular, test the ability to retrieve field
 * values in bytes; skipping chartset translation.
 */
@Jobs({ "job037798" })
@TestId("Dev112_GetExportRecordsTest")
public class GetExportRecordsTest extends P4JavaTestCase {
    private static final int TIME_OUT_IN_SECONDS = 90;

	private static IClient client = null;
	private Integer journal = 0;
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@BeforeAll
	public static void beforeAll() throws Exception{
		Properties rpcTimeOutProperties = configRpcTimeOut("GetExportRecordsTest", TIME_OUT_IN_SECONDS);

		// initialization code (before each test).
		server = getServerAsSuper(rpcTimeOutProperties);
		assertNotNull(server);
		client = server.getClient("p4TestUserWS");
		assertNotNull(client);
		server.setCurrentClient(client);
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@BeforeEach
	public void setUp() throws Exception{
		journal = new Integer(server.getCounter("journal"));
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@AfterAll
	public static void afterAll() throws Exception {
		afterEach(server);
	}

	/**
	 * Test 'p4 export' command - default, no skipping; normal data conversion.
	 */
	@Test
	public void testExportNoSkip() throws Exception {
		// Set skipDataConversion to false, so we should get data as strings
		List<Map<String, Object>> exportList = server.getExportRecords(new ExportRecordsOptions()
				.setUseJournal(true).setSourceNum(journal).setMaxRecs(1000000)
				.setFilter("table=db.traits"));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 0);

		// Get the first data map and inspect the data is in string format
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("TTvalue");
		assertNotNull(dataObject);
		assertTrue(dataObject instanceof String);
	}
	
	/**
	 * Test 'p4 export' command. Skip charset translation of a range of fields
	 * (inclusive start field and non-inclusive stop field); retrieve those
	 * field values in bytes. <p>
	 * 
	 * Note, by default the export command's start field is set to "op" and stop
	 * field is set to "func".
	 */
	@Test
	public void testExportSkipFieldRange() throws Exception {

		// Set skipDataConversion to true
		// This query takes a little more time to run (filter by attr name)
		List<Map<String, Object>> exportList = server
				.getExportRecords(new ExportRecordsOptions()
						.setUseJournal(true).setSourceNum(journal)
						.setFilter("TTname=test2")
						.setSkipDataConversion(true));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 2);

		// The first data map contains the data in bytes
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("TTvalue");
		assertNotNull(dataObject);

		// Verify the return data is of byte[] and it's size
		assertTrue(dataObject instanceof byte[]);
		assertEquals(40, ((byte[]) dataObject).length);
	}

	/**
	 * Test 'p4 export' command. Skip chartset translation of a range of fields
	 * (inclusive start field and non-inclusive stop field); retrieve those
	 * field values in bytes. <p>
	 * 
	 * Note, by default the export command's start field is set to "op" and stop
	 * field is set to "func".
	 */
	@Test
	public void testExportSkipFieldRange2() throws Exception{
		// Set skipDataConversion to true
		List<Map<String, Object>> exportList = server.getExportRecords(new ExportRecordsOptions()
				.setMaxRecs(1000000).setUseJournal(true).setSourceNum(journal)
				.setFilter("table=db.traits").setSkipDataConversion(true));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 0);

		// Get the first data map and inspect the data is in bytes
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("TTvalue");
		assertNotNull(dataObject);
		assertTrue(dataObject instanceof byte[]);
	}

	/**
	 * Test 'p4 export' command. Skip chartset translation of fields matching a
	 * pattern; retrieve those field values in bytes.
	 */
	@Test
	public void testExportSkipFieldPattern() throws Exception {
		// Set skipDataConversion to true
		List<Map<String, Object>> exportList = server.getExportRecords(new ExportRecordsOptions()
				.setMaxRecs(1000000).setUseJournal(true).setSourceNum(journal)
				.setFilter("table=db.traits").setSkipDataConversion(true)
				.setSkipFieldPattern("^[A-Z]{2}\\w+"));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 0);

		// Get the first data map and inspect the data is in bytes
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("TTvalue");
		assertNotNull(dataObject);
		assertTrue(dataObject instanceof byte[]);
	}
}
