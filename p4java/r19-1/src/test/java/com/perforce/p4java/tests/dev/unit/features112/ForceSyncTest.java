/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test force sync ('sync -f') so that it always re-deletes a deleted revision.
 */
@Jobs({ "job046828" })
@TestId("Dev112_ForceSyncTest")
public class ForceSyncTest extends P4JavaRshTestCase {

	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ForceSyncTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
            server.setCurrentClient(client);
		    createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/bin/gnu/getopt/branch10100/ToBeDeleted_forceSyncTest.properties", "desc"); //make global
		    deleteFile("//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/branch10100/ToBeDeleted_forceSyncTest.properties", server, client, "desc");
			createClient(server, "testClient");
            client = server.getClient("testClient"); //create a variable
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
	 * Users expect 'sync -f' to force their client state to match the
	 * corresponding state of the revision, but 'sync -f' wasn't deleting the
	 * client file when the db.have table indicated that the client already had
	 * that delete. Since the point of '-f' is to force sync to take the action,
	 * even if we think the client already has the action, sync -f should
	 * re-delete the deleted revision.
	 */
	@Test
	public void testForceSync() {

		List<IFileSpec> files = null;
		String deletedFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/branch10100/ToBeDeleted_forceSyncTest.properties";

		try {
		    
			// sync deletedFile@30155
			// Get a revision of the file before it was deleted
			files = client.sync(
					FileSpecBuilder.makeFileSpecList(deletedFile + "@24471"),
					new SyncOptions());
			assertNotNull(files);
			assertTrue(files.size() == 1);

			// The action should be "added"
			assertNotNull(files.get(0).getAction());
			assertEquals(files.get(0).getAction(), FileAction.ADDED);

			// The file should now be in the local client workspace
			File localFile = new File(files.get(0).getClientPathString());
			assertNotNull(localFile);
			assertTrue(localFile.exists());

			// sync -k deletedFile
			// Updates server metadata without syncing files
			files = client.sync(FileSpecBuilder.makeFileSpecList(deletedFile),
					new SyncOptions().setClientBypass(true));
			assertNotNull(files);

			// sync -f deletedFile
			// Force sync to the head revision should remove the deleted file
			files = client.sync(FileSpecBuilder.makeFileSpecList(deletedFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);
			assertTrue(files.size() == 1);
          
			// The action should be "deleted"
			assertNotNull(files.get(0).getAction());
			assertEquals(files.get(0).getAction(), FileAction.DELETED);

			// The file should now be deleted from the local client workspace
			localFile = new File(files.get(0).getClientPathString());
			assertNotNull(localFile);
			assertFalse(localFile.exists());

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
