/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test copy files with "-q" flag.
 */
@Jobs({ "job059637" })
@TestId("Dev131_CCopyFilesQuietTest")
public class CopyFilesQuietTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", CopyFilesQuietTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {

		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
			createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java", "test");
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
	 * Test copy files with "-q" flag.
	 */
	@Test
	public void testCopyFilesQuiet() {

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ "p4cmd/P4CmdDispatcher.java";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher.java";

		try {
			// Preview copy files
			List<IFileSpec> copyFiles = client.copyFiles(
					new FileSpec(sourceFile),
					new FileSpec(targetFile),
					null,
					new CopyFilesOptions().setNoUpdate(true));

			assertNotNull(copyFiles);

			// Should have an info output
            assertEquals(1, copyFiles.size());
            assertTrue(copyFiles.get(0).getOpStatus().toString().contains("VALID"));

			// Preview copy files with "-q"
			copyFiles = client.copyFiles(
					new FileSpec(sourceFile),
					new FileSpec(targetFile),
					null,
					new CopyFilesOptions().setNoUpdate(true).setQuiet(true));

			assertNotNull(copyFiles);

			// Should NOT have any info output
            assertEquals(0, copyFiles.size());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
