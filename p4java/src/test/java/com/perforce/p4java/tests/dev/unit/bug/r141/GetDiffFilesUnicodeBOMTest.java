/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

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
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.GetDiffFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test IClient.getDiffFiles() - 'p4 diff -sa' with Unicode BOM file.
 */
@Jobs({ "job069733" })
@TestId("Dev141_GetDiffFilesUnicodeBOMTest")
public class GetDiffFilesUnicodeBOMTest extends P4JavaRshTestCase {

	

	@ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", GetDiffFilesUnicodeBOMTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", false, null);
    
    }
	
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			
			assertNotNull(server);
			
			client = server.getClient(SystemInfo.isWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}

	

	/**
	 * Test IClient.getDiffFiles() - 'p4 diff -sa'.
	 */
	@Test
	@Ignore("Tries to connect to p4java://localhost:30131 that does not exist")
	public void testGetDiffFiles() {

		String depotFile = "//depot/111bugs/Bugs111_Job043500Test/src/test-utf8-bom.txt";
		String nonExistingDepotFile = "//depot/111bugs/Bugs111_Job043500Test/src/test01.txt-Non-Existing";

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		try {
			// Create a changelist
			changelist = getNewChangelist(server, client, "Dev132_SubmitShelvedChangelistTest copy files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			/*
			// Sync a file for edit
			files = client.sync(FileSpecBuilder.makeFileSpecList(depotFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);
			 */
			
			// Sync file using the p4 command line client with charset utf8-bom
			p4CmdSyncUnicodeBOMFile(depotFile);
			
			// Open a file for edit
			files = client.editFiles(FileSpecBuilder.makeFileSpecList(depotFile),
					new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			
			// Get diff files (diff -sa)
			files = client.getDiffFiles(FileSpecBuilder.makeFileSpecList(depotFile), new GetDiffFilesOptions().setOpenedDifferentMissing(true));
			assertNotNull(files);
			assertTrue(files.isEmpty());

			// Get diff files (diff -sa) of a non-existing file
			files = client.getDiffFiles(FileSpecBuilder.makeFileSpecList(nonExistingDepotFile), new GetDiffFilesOptions().setOpenedDifferentMissing(true));
			assertNotNull(files);
			assertFalse(files.isEmpty());
			assertNotNull(files.get(0));
			assertEquals(FileSpecOpStatus.ERROR, files.get(0).getOpStatus());

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

	/**
	 * Sync file using the p4 command line client with charset utf8-bom.
	 */
	private void p4CmdSyncUnicodeBOMFile(String filePath) {

		String p4Cmd = "/opt/perforce/p4/bin/p4";
		BufferedReader br = null;
		
		try {
			String[] command = new String[] {"/bin/sh", "-c", p4Cmd + " -p"
					+ server.getServerInfo().getServerAddress() + " -u"
					+ this.userName + " -P" + server.getAuthTicket() + " -c"
					+ this.defaultTestClientName + " -Cutf8-bom"
					+ " sync -f " + filePath};
			
		   ProcessBuilder builder = new ProcessBuilder(command);
		    Map<String, String> env = builder.environment();
		    assertNotNull(env);

		    final Process process = builder.start();
		    br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		    String line;
		    while ((line = br.readLine()) != null) {
		      System.out.println(line);
		    }
		    System.out.println("Program terminated!");
			    
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (ConnectionException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// Can't do anyting else here...
				}
			}
		}
	}
}
