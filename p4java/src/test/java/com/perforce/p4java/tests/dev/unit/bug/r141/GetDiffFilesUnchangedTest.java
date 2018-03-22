/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.GetDiffFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test IClient.getDiffFiles() with unchanged files (unicode, unicode+C) under
 * the Windows platform. One large file of 13mb.
 */
@Jobs({ "job071340" })
@TestId("Dev141_GetDiffFilesUnchangedTest")
public class GetDiffFilesUnchangedTest extends P4JavaRshTestCase {

	

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetDiffFilesUnchangedTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    }
	

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			// Connect to a replica server
			//server = getServer("p4java://eng-p4java-vm.perforce.com:40132", null);
			assertNotNull(server);
			// Client with line endings 'win'.
			client = server.getClient(((Server)server).isRunningOnWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}

	

	/**
	 * Test IClient.getDiffFiles() with unchanged files under the Windows platform.
	 * 
	 * Diff local file against depot file to see if there is change.
	 * 
	 * diff -sa (opened file - but no change)
	 * diff -se (unopened file - no change)
	 */
	@Test
	@Ignore("Tries to connect to p4java://eng-p4java-vm.perforce.com:40132 that does not exist")
	public void testGetDiffFilesUnchangedFiles() {

		// This file is type <unicode+C>
		// ~ 13mb file size
		String depotFile = "//depot/132Bugs/job071340/Other/Localization/QA/UserGuide_JA-JP/omegat/project_save.tmx";
		// This file is type <unicode>
		// ~ 1kb
		String depotFile2 = "//depot/132Bugs/job071340/Other/Localization/TestProject/omegat/project_save.tmx";

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		try {
			// Create a changelist
			changelist = getNewChangelist(server, client, "Dev141_GetDiffFilesUnchangedTest get diff files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Sync files
			files = client.sync(FileSpecBuilder.makeFileSpecList(new String[] {depotFile, depotFile2}), new SyncOptions().setForceUpdate(true));
			assertNotNull(files);
			//Map<String, Object>[] results = server.execMapCmd("sync", new String[] { "-f", depotFile }, null);
			//assertNotNull(results);

			// Get diff files (diff -se) - unopened file
			files = client.getDiffFiles(FileSpecBuilder.makeFileSpecList(new String[] {depotFile, depotFile2}), new GetDiffFilesOptions().setUnopenedDifferent(true));
			assertNotNull(files);
			assertTrue(files.isEmpty());

			// Open a file for edit
			files = client.editFiles(FileSpecBuilder.makeFileSpecList(new String[] {depotFile, depotFile2}), new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			assertFalse(files.isEmpty());

			// Get diff files (diff -sa) - opened file
			files = client.getDiffFiles(FileSpecBuilder.makeFileSpecList(new String[] {depotFile, depotFile2}), new GetDiffFilesOptions().setOpenedDifferentMissing(true));
			assertNotNull(files);
			assertTrue(files.isEmpty());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							client.revertFiles(
									changelist.getFiles(true),
									new RevertFilesOptions()
											.setChangelistId(changelist.getId()));
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
		}
	}
}
