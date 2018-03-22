/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test copy files with "-q" flag.
 */
@Jobs({ "job059637" })
@TestId("Dev131_CCopyFilesQuietTest")
public class CopyFilesQuietTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

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
			server = getServer();
			assertNotNull(server);
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
