/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.DuplicateRevisionsOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
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
import static org.junit.Assert.fail;

/**
 * Test 'p4 duplicate' (unsupported).
 */
@Jobs({ "job057185" })
@TestId("Dev122_DuplicateRevisionsTest")
public class DuplicateRevisionsTest extends P4JavaRshTestCase {

	IOptionsServer superServer = null;
	IClient superClient = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", DuplicateRevisionsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() throws Exception {
		// initialization code (before each test).
		try {
		    superServer = getSuperConnection(p4d.getRSHURL());
		    superClient = superServer.getClient("p4TestSuperWS20112");
		    assertNotNull(superClient);
			superServer.setCurrentClient(superClient);
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test 'p4 duplicate'.
	 */
	@Test
	public void testDuplicateRevisions() {

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
