/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

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
import org.junit.ClassRule;
import org.junit.Ignore;
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
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test shelve changelist from another client that is not the server's current client.
 */
@Jobs({ "job062825" })
@TestId("Dev152_ShelveChangelistClientTest")
public class ShelveChangelistClientTest extends P4JavaRshTestCase {

	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	String serverMessage = null;
	long completedTime = 0;

	private static IClient client = null;
	private static IClient client2 = null;
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ShelveChangelistClientTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", false, null);
    
    }

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			// Another client for testing
			client2 = server.getClient("testClient731676005");
			assertNotNull(client2);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}


	/**
	 * Test shelve changelist from another client that is not the server's current client.
	 */
	@Test
	@Ignore("Clients do not exist anymore")
	public void testShelveChangelistClient() {

		IChangelist changelist = null;

		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"	+ dir + "/MessagesBundle_es.properties";

		try {
			// Copy a file to be used for shelving
			changelist = getNewChangelist(server, client, "Dev152_ShelveChangelistClientTest copy files");
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
			changelist = getNewChangelist(server, client, "Dev152_ShelveChangelistClientTest edit files");
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
			
			// Set the server's current to client2 (which is not the current client)
			server.setCurrentClient(client2);
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
