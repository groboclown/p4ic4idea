/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'describe -s -S <pending changelist>'. Get a list of shelved files from
 * a pending changelist.
 */
@Jobs({ "job072689" })
@TestId("Dev141_GetShelvedFilesTest")
public class GetShelvedFilesTest extends P4JavaTestCase {

	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

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
			server = getServer(this.serverUrlString, null, "p4jtestuser",
					"p4jtestuser");
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
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
	 * Test 'describe -s -S <pending changelist>'. Get a list of shelved files
	 * from a pending changelist.
	 */
	@Test
	public void testGetShelvedFiles() {

		IChangelist changelist = null;

		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"	+ dir + "/MessagesBundle_es.properties";

		try {
			// Copy a file to be used for shelving and unshelving
			changelist = getNewChangelist(server, client, "Dev141_GetShelvedFilesTest copy files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(
					targetFile), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Make changes to the file and shelve it
			changelist = getNewChangelist(server, client, "Dev141_GetShelvedFilesTest edit files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			files = client.editFiles(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new EditFilesOptions().setChangelistId(changelist.getId()));

			assertNotNull(files);
			assertTrue(files.size() == 1);
			assertNotNull(files.get(0));

			String localFilePath = files.get(0).getClientPathString();
			assertNotNull(localFilePath);

			writeFileBytes(localFilePath, "// some test text." + LINE_SEPARATOR, true);

			changelist.refresh();
			files = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(targetFile),
					changelist.getId(), new ShelveFilesOptions());
			assertNotNull(files);

			// Get the shelved file
			files = server.getShelvedFiles(changelist.getId());
			assertNotNull(files);
			assertTrue(files.size() > 0);

			// Delete the shelved file
			files = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(targetFile),
					changelist.getId(),
					new ShelveFilesOptions().setDeleteFiles(true));
			assertNotNull(files);
			assertTrue(files.size() > 0);
			assertNotNull(files.get(0) != null);
			assertTrue(files.get(0).getOpStatus() == FileSpecOpStatus.INFO);
			assertEquals("Shelved change " + changelist.getId() + " deleted.", files.get(0).getStatusMessage());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null && server != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							client.revertFiles(
									changelist.getFiles(true),
									new RevertFilesOptions()
											.setChangelistId(changelist.getId()));
							// Delete pending changelist
							server.deletePendingChangelist(changelist.getId());
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
		}
	}
}
