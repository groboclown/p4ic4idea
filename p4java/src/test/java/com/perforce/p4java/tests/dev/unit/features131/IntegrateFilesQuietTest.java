/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
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
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test integrate files with the '-q' flag.
 */
@Jobs({ "job059637" })
@TestId("Dev131_IntegrateFilesQuietTest")
public class IntegrateFilesQuietTest extends P4JavaTestCase {

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
	 * Test integrate files with "-q" flag.
	 */
    @Test
    public void testIntegrateFilesQuiet() {
		int randNum = getRandomInt();
        String dir = "branch" + randNum;

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
                + dir + "/MessagesBundle_es.properties";

        try {
            // Preview integrate files
            List<IFileSpec> integrateFiles = client.integrateFiles(
                    new FileSpec(sourceFile),
                    new FileSpec(targetFile),
                    null,
                    new IntegrateFilesOptions().setShowActionsOnly(true));

            assertNotNull(integrateFiles);
            
            // Should have an info output
            assertEquals(1, integrateFiles.size());

            // Preview integrate files with "-q"
            integrateFiles = client.integrateFiles(
                    new FileSpec(sourceFile),
                    new FileSpec(targetFile),
                    null,
                    new IntegrateFilesOptions().setShowActionsOnly(true).setQuiet(true));

            assertNotNull(integrateFiles);
 
            // Should NOT have any info output
            assertEquals(0, integrateFiles.size());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
