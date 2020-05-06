/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 merge -q'.
 */
@Jobs({ "job059637" })
@TestId("Dev131_MergeFilesQuietTest")
public class MergeFilesQuietTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", FilterCallbackTest.class.getSimpleName());

	IChangelist changelist = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, null);
			client = getClient(server);
			createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties", "test");
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
	 * Test merge files with "-q" flag.
	 */
    @Test
    public void testMergeFilesQuiet() {
		int randNum = getRandomInt();
        String dir = "branch" + randNum;

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
                + dir + "/MessagesBundle_es.properties";

        try {
            // Preview merge files
            List<IFileSpec> mergeFiles = client.mergeFiles(
                    new FileSpec(sourceFile),
                    FileSpecBuilder.makeFileSpecList(targetFile),
                    new MergeFilesOptions().setShowActionsOnly(true));

            assertNotNull(mergeFiles);
            
            // Should have an info output
            assertEquals(1, mergeFiles.size());

            // Preview merge files with "-q"
            mergeFiles = client.mergeFiles(
                    new FileSpec(sourceFile),
                    FileSpecBuilder.makeFileSpecList(targetFile),
                    new MergeFilesOptions().setShowActionsOnly(true).setQuiet(true));

            assertNotNull(mergeFiles);
 
            // Should NOT have any info output
            assertEquals(0, mergeFiles.size());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
