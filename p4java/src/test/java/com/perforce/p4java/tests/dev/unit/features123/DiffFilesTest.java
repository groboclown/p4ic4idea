/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simulate client side 'diff' using server side 'diff2' with shelving.
 */
@Jobs({"job057847"})
@TestId("Dev123_DiffFilesTest")
public class DiffFilesTest extends P4JavaRshTestCase {

	IClient client = null;
	IChangelist changelist = null;
	List<IFileSpec> files = null;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", DiffFilesTest.class.getSimpleName());

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
			client = createClient(server, "diffFilesTestClient");
			assertNotNull("Could not get client, "
					+ getPlatformClientName("diffFilesTestClient")
					+ " from " + server.getServerInfo().getServerAddress(), client);
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
	 * Simulate client side 'diff' using server side 'diff2' with shelving.
	 */
	@Test
	public void testFileDiff() {
		int randNum = getRandomInt();
		String depotFile = null;

		try {
			List<IServerMessage> messageList = null;
			String path = "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/";
			String name = "P4JCommandCallbackImpl";
			String ext = ".java";
			String file = client.getRoot() + path + name + ext;
			String file2 = client.getRoot() + path + name + "-" + randNum + ext;
			depotFile = "//depot" + path + name + "-" + randNum + ext;

			String testText = "///// added test text " + randNum + " /////" + LINE_SEPARATOR;

			List<IFileSpec> files = client.sync(
					FileSpecBuilder.makeFileSpecList(file),
					new SyncOptions().setForceUpdate(true));
			assertTrue("Sync failed to return anything.", files != null && files.size() > 0);
			messageList = P4JavaTestCase.getErrorsFromFileSpecList(files);
			assertTrue("Sync returned errors, " + messageList, messageList.size() == 0);

			// Copy a file to be used for add
			copyFile(file, file2);

			changelist = getNewChangelist(server, client,
					"Dev112_EditFilesTest add files");
			assertNotNull("New changelist failed to return anything.", changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull("Create changelist failed to return anything.", changelist);

			// Add the file
			files = client.addFiles(FileSpecBuilder.makeFileSpecList(file2),
					new AddFilesOptions().setChangelistId(changelist.getId()));

			assertTrue("Add files returned an empty result.", files != null && files.size() != 0);
			messageList = P4JavaTestCase.getErrorsFromFileSpecList(files);
			assertTrue("Add files returned errors, " + messageList, messageList.size() == 0);


			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			changelist = getNewChangelist(server, client,
					"Dev112_EditFilesTest edit files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Edit the file
			files = client.editFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new EditFilesOptions().setChangelistId(changelist.getId()));

			assertTrue("Edit files returned an empty result.", files != null && files.size() != 0);
			messageList = P4JavaTestCase.getErrorsFromFileSpecList(files);
			assertTrue("Edit files returned errors, " + messageList, messageList.size() == 0);

			// Append text to the edit file
			assertNotNull(files.get(0).getClientPathString());
			writeFileBytes(files.get(0).getClientPathString(), testText, true);

			// Shelve the file to a changelist
			// Try with different options, if not successful
			files = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					changelist.getId(), new ShelveFilesOptions());
			assertTrue("Shelve files returned an empty result.", files != null && files.size() != 0);
			messageList = P4JavaTestCase.getErrorsFromFileSpecList(files);
			assertTrue("Shelve files returned errors, " + messageList, messageList.size() == 0);

			// Run server side 'diff2' with the shelved file and the depot file head
			InputStream is = server.getFileDiffsStream(new FileSpec(depotFile
							+ "@=" + changelist.getId()), new FileSpec(depotFile),
					null, new GetFileDiffsOptions());
			assertNotNull(is);

			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			// Capture diff lines
			List<String> lines = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();

			// Should contain at least one line of diff
			assertTrue(lines.size() > 0);

			// Delete the shelved file
			// Try with different options, if not successful
			files = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					changelist.getId(),
					new ShelveFilesOptions().setDeleteFiles(true));
			assertTrue("Delete shelved files returned an empty result.", files != null && files.size() != 0);
			messageList = P4JavaTestCase.getErrorsFromFileSpecList(files);
			assertTrue("Delete shelved files returned errors, " + messageList, messageList.size() == 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
			e.printStackTrace();
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
			if (client != null && server != null) {
				if (depotFile != null) {
					try {
						// Delete submitted test files
						IChangelist deleteChangelist = getNewChangelist(server,
								client,
								"Dev112_EditFilesTest delete submitted files");
						deleteChangelist = client
								.createChangelist(deleteChangelist);
						client.deleteFiles(FileSpecBuilder
										.makeFileSpecList(new String[]{depotFile}),
								new DeleteFilesOptions()
										.setChangelistId(deleteChangelist
												.getId()));
						deleteChangelist.refresh();
						deleteChangelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
