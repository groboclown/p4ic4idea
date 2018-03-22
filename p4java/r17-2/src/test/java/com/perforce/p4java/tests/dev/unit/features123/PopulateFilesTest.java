/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.PopulateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 populate" branches a set of files (the 'source') into another depot
 * location (the 'target') in a single step.
 */
@Jobs({ "job058523" })
@TestId("Dev123_PopulateFilesTest")
public class PopulateFilesTest extends P4JavaTestCase {

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
     * Test populate files
     */
    @Test
    public void testPopulateFiles() {
		int randNum = getRandomInt();
        String sourceFiles = "//depot/112Dev/Attributes/...";
        String targetFiles = "//depot/112Dev/Attributes" + randNum + "/...";

        try {
            PopulateFilesOptions options = new PopulateFilesOptions();
            options.setDescription("test p4 populate");
            options.setMaxFiles(5);
            options.setShowPopulatedFiles(true);

            List<IFileSpec> populateFiles = client.populateFiles(new FileSpec(
                    sourceFiles), FileSpecBuilder.makeFileSpecList(targetFiles), options);
            assertNotNull(populateFiles);
            assertEquals(6, populateFiles.size());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
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
