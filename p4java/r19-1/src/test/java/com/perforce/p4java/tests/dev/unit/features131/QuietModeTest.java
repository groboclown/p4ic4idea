/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.CmdSpec;
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
 * Test protocol level quiet mode (p4 -q <command>). Global -q (-quiet) option,
 * suppress ALL info-level output. 
 */
@Jobs({ "job059638" })
@TestId("Dev131_QuietModeTest")
public class QuietModeTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", QuietModeTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			Properties props = new Properties();
			props.put("quietMode", "true");
			server = getServer(p4d.getRSHURL(), props);
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
	 * Test quiet mode sync command (p4 -q sync) using execMapCmd().
	 */
	@Test
	public void testQuietModeSyncMapCmd() {
		try {
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.SYNC.toString(), new String[] { "-n", "-f", "//depot/152Bugs/job085433/1478796344038/..." }, null);
			assertNotNull(result);
			assertTrue(result.length == 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}