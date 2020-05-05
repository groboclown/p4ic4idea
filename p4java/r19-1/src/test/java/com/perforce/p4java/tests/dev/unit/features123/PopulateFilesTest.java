/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test "p4 populate" branches a set of files (the 'source') into another depot
 * location (the 'target') in a single step.
 */
@Jobs({ "job058523" })
@TestId("Dev123_PopulateFilesTest")
public class PopulateFilesTest extends P4JavaRshTestCase {

	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", PopulateFilesTest.class.getSimpleName());

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
			client = getDefaultClient(server);
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
