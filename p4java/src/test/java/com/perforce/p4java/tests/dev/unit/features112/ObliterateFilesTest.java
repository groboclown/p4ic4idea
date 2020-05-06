/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.List;

import static com.perforce.p4java.tests.ServerMessageMatcher.isText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test obliterate permanently removes files and their history from the server.
 */
@Jobs({ "job041780", "job041824" })
@TestId("Dev112_ObliterateFilesTest")
public class ObliterateFilesTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ObliterateFilesTest.class.getSimpleName());

	IChangelist changelist = null;
	List<IFileSpec> files = null;

	IOptionsServer server2 = null;
	IClient client2 = null;

	String serverMessage = null;
	long completedTime = 0;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), superUserName, superUserPassword, false, props);

			client = createClient(server, "ObliterateFilesTestClient");


			String[] files = new String[2];
			files[0] = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
					+ "p4cmd/P4CmdDispatcher.java";
			files[1] = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
					+ "p4cmd/random.java";
			createTextFilesOnServer(client, files, "test");

			client = server.getClient("p4TestSuperWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);

			server2 = ServerFactory.getOptionsServer(p4d.getRSHURL(), props);;
			server2.registerCallback(createCommandCallback());
			server2.connect();
			server2.setUserName(userName);
			server2.login(password, new LoginOptions());
			assertNotNull(server2);
			client2 = server2.getClient("p4TestUserWS20112");
			assertNotNull(client2);
			server2.setCurrentClient(client2);
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
		if (server2 != null) {
			this.endServerSession(server2);
		}
	}

	/**
	 * Test obliterate permanently removes files and their history from the
	 * server.
	 */
	@Test
	public void testObliterateFiles() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ "p4cmd/P4CmdDispatcher.java";
		String sourceFile2 = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ "p4cmd/...";

		String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher.java";

		String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher2.java";

		String targetFile3 = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher2.java";

		String targetFile4 = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/p4cmd/...";

		try {
			// Copy the source files to targets
			changelist = getNewChangelist(server, client,
					server.getUser("p4jtestsuper"),
					"Dev112_ObliterateFilesTest copy files");
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(
					targetFile), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);

			files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(
					targetFile2), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);

			files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(
					targetFile3), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);

			files = client.copyFiles(new FileSpec(sourceFile2), new FileSpec(
					targetFile4), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Run obliterate - preview only - no options
			List<IObliterateResult> results = server.obliterateFiles(
					FileSpecBuilder.makeFileSpecList(new String[] { targetFile,
							targetFile2, targetFile3, targetFile4,
							"//depot/client/ResolveFileStreamTest/...",
							"//depot/123456789/123/Foobar.java" }),
					new ObliterateFilesOptions());
			assertNotNull(results);
			// We should get 6 result sets, since there are 6 filespecs
			assertTrue(results.size() == 6);
			// Result 1 to 4 and 6 should not have any returned filespecs
			// ("purgeFile" and "purgeRev").
			assertTrue(results.get(0).getFileSpecs().size() == 0);
			assertTrue(results.get(1).getFileSpecs().size() == 0);
			assertTrue(results.get(2).getFileSpecs().size() == 0);
			assertTrue(results.get(3).getFileSpecs().size() == 0);
			assertTrue(results.get(5).getFileSpecs().size() == 0);
			// Result 5 should have many returned filespecs, since it is a wild
			// card depot path containing many files with many revisions.
			assertTrue(results.get(4).getFileSpecs().size() > 0);
			// These result are in "reportOnly" mode.
			for (IObliterateResult result : results) {
				assertTrue(result.isReportOnly());
			}

			long nonOptimizedTime = completedTime;

			// Run obliterate - preview only - performance enhanced options
			results = server.obliterateFiles(
					FileSpecBuilder.makeFileSpecList(new String[] { targetFile,
							targetFile2, targetFile3, targetFile4,
							"//depot/client/ResolveFileStreamTest/...",
							"//depot/123456789/123/Foobar.java" }),
					new ObliterateFilesOptions()
							.setBranchedFirstHeadRevOnly(true)
							.setSkipArchiveSearchRemoval(true)
							.setSkipHaveSearch(true));
			assertNotNull(results);
			// We should get 6 result sets, since there are 6 filespecs
			assertTrue(results.size() == 6);
			// These result are in "reportOnly" mode.
			for (IObliterateResult result : results) {
				assertTrue(result.isReportOnly());
			}

			long optimizedTime = completedTime;

			// Compare the completion times of the obliterate commands.
			// One without options and one with performance enhancement options.
			// Obliterate "//depot/client/ResolveFileStreamTest/..." should show
			// a difference in completion times (standard vs enhanced options)
			assertTrue(optimizedTime <= nonOptimizedTime);

			// Setting the user to a non-super user and see what happen
			results = server2.obliterateFiles(
					FileSpecBuilder.makeFileSpecList(new String[] { targetFile,
							targetFile2, targetFile3, targetFile4,
							"//depot/123456789/...",
							"//depot/123456789/123/Foobar.java" }),
					new ObliterateFilesOptions());
			assertNotNull(results);
			for (IObliterateResult result : results) {
				assertTrue(result.getFileSpecs().size() > 0);
				assertTrue(result.getFileSpecs().get(0).getOpStatus() == FileSpecOpStatus.ERROR);
				assertThat(result
						.getFileSpecs()
						.get(0)
						.getStatusMessage(),
						isText(
								"You don't have permission for this operation."));
			}

			// Run obliterate for real
			results = server.obliterateFiles(
					FileSpecBuilder.makeFileSpecList(new String[] { targetFile,
							targetFile2, targetFile3, targetFile4,
							"//depot/123456789/...",
							"//depot/123456789/123/Foobar.java" }),
					new ObliterateFilesOptions().setExecuteObliterate(true));
			assertNotNull(results);
			assertTrue(results.size() == 6);
			// The obliterate command should be real, not "reportOnly"
			for (IObliterateResult result : results) {
				assertFalse(result.isReportOnly());
			}

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
			if (client != null && server != null) {
				try {
					// Delete submitted test files
					IChangelist deleteChangelist = getNewChangelist(server,
							client,
							"Dev112_ObliterateFilesTest delete submitted files");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					client.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { targetFile,
									targetFile2, targetFile3, targetFile4 }),
							new DeleteFilesOptions()
									.setChangelistId(deleteChangelist.getId()));
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
