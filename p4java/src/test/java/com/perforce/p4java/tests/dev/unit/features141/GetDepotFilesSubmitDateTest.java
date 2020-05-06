/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'IOptionsServer.getDepotFiles()'. Get depot files with submit dates.
 */
@Jobs({ "job072336" })
@TestId("Dev141_GetDepotFilesSubmitDateTest")
public class GetDepotFilesSubmitDateTest extends P4JavaRshTestCase {

	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetDepotFilesSubmitDateTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, null);
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
			endServerSession(server);
		}
	}

	/**
	 * Test get depot files with submit dates.
	 */
	@Test
	public void testGetDepotFilesSubmitDate() {

		List<IFileSpec> files = null;

		String depotPath = "//depot/basic/readonly/grep/...";

		try {
			files = server.getDepotFiles(FileSpecBuilder.makeFileSpecList(depotPath), new GetDepotFilesOptions());
			assertNotNull(files);
			assertTrue(files.size() > 0);
			assertNotNull(files.get(0));
			assertNotNull(files.get(0).getDate() != null);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
