/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

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
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 duplicate' (unsupported).
 */
@Jobs({ "job057185" })
@TestId("Dev122_DuplicateRevisionsTest")
public class DuplicateRevisionsTest extends P4JavaTestCase {

	IOptionsServer superServer = null;
	IClient superClient = null;

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
			superServer = getServerAsSuper();
			superClient = superServer.getClient("p4TestSuperWS20112");
			assertNotNull(superClient);
			superServer.setCurrentClient(superClient);
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test 'p4 duplicate'.
	 */
	@Test
	public void tesDuplicateRevisions() {

		int randNum = getRandomInt();

		String sourceFile = "//depot/93bugs/Job039414/test01/...";
		String targetFile = "//depot/93bugs/Job039414/" + randNum + "/...";

		int startRev = 0;
		int endRev = 2;

		try {
			FileSpec sourceSpec = new FileSpec(sourceFile);
			sourceSpec.setStartRevision(startRev);
			sourceSpec.setEndRevision(endRev);
			FileSpec targetSpec = new FileSpec(targetFile);

			List<IFileSpec> files = superServer.duplicateRevisions(sourceSpec,
					targetSpec, new DuplicateRevisionsOptions());
			assertNotNull(files);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (superClient != null && superServer != null) {
				try {
					List<IObliterateResult> obliterateFiles = superServer
							.obliterateFiles(FileSpecBuilder
									.makeFileSpecList(targetFile),
									new ObliterateFilesOptions()
											.setExecuteObliterate(true));
					assertNotNull(obliterateFiles);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
