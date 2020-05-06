/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.PopulateFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test "p4 populate -b branch -s fromFile toFiles".
 */
@Jobs({ "job058523" })
@TestId("Dev123_PopulateBranchFilesTest")
public class PopulateBranchFilesTest extends P4JavaRshTestCase {

	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", PopulateBranchFilesTest.class.getSimpleName());

   	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    Properties properties = new Properties();
	        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
			assertNotNull(server);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);

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
	 * Test 'p4 populate -b branch -s fromFile toFiles'.
	 */
	@Test
	public void testCopyFilesWithBranchView() {
        String targetFiles = "//depot/populate-test-branch/SandboxTest/getopt/testbranch/...";

		try {
			List<IFileSpec> files = client
					.populateFiles(
							null,
							FileSpecBuilder
									.makeFileSpecList(new String[] { targetFiles }),
							new PopulateFilesOptions()
									.setBranch("populate-test-branch")
									.setBidirectional(false)
									.setShowPopulatedFiles(true));
			assertNotNull(files);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null && server != null) {
				try {
					// Delete submitted test files
					IChangelist deleteChangelist = getNewChangelist(server,
							client, "Dev123_PopulateFilesOptionsTest delete submitted files");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					List<IFileSpec> deleteFiles = client.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { targetFiles }),
							new DeleteFilesOptions()
									.setChangelistId(deleteChangelist.getId())
									.setDeleteNonSyncedFiles(true));
					assertNotNull(deleteFiles);
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}

	}
}
