/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test triggers using the raw execMapCmd() method.
 */
@Jobs({ "job071702" })
@TestId("Dev141_TriggersExecMapCmdTest")
public class TriggersExecMapCmdTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1",TriggersExecMapCmdTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			Properties properties = new Properties();
			properties.put("relaxCmdNameChecks", "true");
			setupServer(p4d.getRSHURL(), userName, password, true, properties);
			setupUtf8(server);
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
	 * Test triggers command using input string cmd.
	 */
	@Test
	public void testTriggersInputStringCmd() {
		
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("Triggers:").append("\n");
			sb.append("\t").append("example1 change-submit //depot/... \"echo %changelist%\"").append("\n");
			sb.append("\t").append("example2 change-submit //depot/... \"echo %changelist%\"").append("\n");
			sb.append("\t").append("example3 change-submit //depot/... \"echo %changelist%\"");
			
			Map<String, Object>[] resultMaps = server.execInputStringMapCmd("triggers", new String[] {"-i"}, sb.toString());
			assertNotNull(resultMaps);
			assertTrue(resultMaps.length > 0);
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
