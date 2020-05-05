/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.tests.SSLServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaLocalServerTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test 'p4 export' command. In particular, test the ability to retrieve field
 * values in bytes; skipping chartset translation.
 */
@Jobs({ "job037798" })
@TestId("Dev112_GetExportRecordsTest")
public class GetExportRecordsTest extends P4JavaLocalServerTestCase {

	@ClassRule
	public static SSLServerRule p4d = new SSLServerRule("r16.1", GetExportRecordsTest.class.getSimpleName(),"ssl:localhost:10674");

	private Integer journal = 0;
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@BeforeClass
	public static void beforeAll() throws Exception{
		props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
		setupSSLServer(p4d.getP4JavaUri(), superUserName, superUserPassword, false, props);
		server.execMapCmd("admin", new String[]{"journal"}, null);
		IClient client = createClient(server, "getExportRecordsTestClient");
		String[] filenames = new String[100];
		for (int i=0 ; i <100; i++) {
			filenames[i] = "test" + i;
		}
		createTextFilesOnServer(client, filenames, "test");
		client = server.getClient("p4TestUserWS");
		assertNotNull(client);
		server.setCurrentClient(client);
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() throws Exception{
		journal = new Integer(server.getCounter("journal"));
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@AfterClass
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
				.setUseJournal(true).setSourceNum(journal).setMaxRecs(10000)
				.setFilter("table=db.have"));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 0);

		// Get the first data map and inspect the data is in string format
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("HAdfile");
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
						.setFilter("HAdfile=//depot/test0")
						.setSkipDataConversion(true));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 0);

		// The first data map contains the data in bytes
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("HAdfile");
		assertNotNull(dataObject);

		// Verify the return data is of byte[] and it's size
		assertTrue(dataObject instanceof byte[]);
		assertEquals(13, ((byte[]) dataObject).length);
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
				.setMaxRecs(50).setUseJournal(true).setSourceNum(journal)
				.setSkipDataConversion(true));
		assertNotNull(exportList);
		assertEquals(exportList.size(),51);

		// Get the first data map and inspect the data is in bytes
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
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
				.setSkipDataConversion(true)
				.setFilter("table=db.have")
				.setSkipFieldPattern("^[A-Z]{2}\\w+"));
		assertNotNull(exportList);
		assertTrue(exportList.size() > 0);

		// Get the first data map and inspect the data is in bytes
		Map<String, Object> dataMap = exportList.get(0);
		assertNotNull(dataMap);
		Object dataObject = dataMap.get("HAdfile");
		assertNotNull(dataObject);
		assertTrue(dataObject instanceof byte[]);
	}
}
