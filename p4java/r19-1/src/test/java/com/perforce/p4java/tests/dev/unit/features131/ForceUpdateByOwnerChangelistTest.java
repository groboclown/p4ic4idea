/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test force update by owner of submitted changelists.
 */
@Jobs({ "job064491" })
@TestId("Dev131_ForceUpdateByOwnerChangelistTest")
public class ForceUpdateByOwnerChangelistTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", ForceUpdateByOwnerChangelistTest.class.getSimpleName());

	private static IOptionsServer superServer = null;
	private static IClient superClient = null;
	private static IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// initialization code (before each test).
		final String superClientName = "p4TestSuperWS20112";
		final String clientName = "p4TestUserWS20112";

		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = createClient(server,clientName);
			assertNotNull(client);
			server.setCurrentClient(client);

			superServer = getServerAsSuper(p4d.getRSHURL());
			superClient = createClient(superServer, superClientName);
			assertNotNull(superClient);
			superServer.setCurrentClient(superClient);

			createTextFileOnServer(client, "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java", "test");
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(superServer);
		afterEach(server);
	}

	/**
	 * Test force update by owner of submitted changelists.
	 */
	@Test
	public void testForceUpdateByOwnerChangelist() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ "p4cmd/P4CmdDispatcher.java";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher.java";

		try {
			// Copy the source file to target
			changelist = getNewChangelist(server, client,
					"Dev131_ForceUpdateByOwnerChangelistTest copy files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			files = client.copyFiles(new FileSpec(sourceFile), new FileSpec(
					targetFile), null, new CopyFilesOptions()
					.setChangelistId(changelist.getId()));
			assertNotNull(files);
			assertTrue(files.get(0).getOpStatus().toString().contains("VALID"));
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Update the submitted changelist description fields
			changelist.refresh();
			changelist.setDescription("New description test " + randNum);
			
			changelist.update(new ChangelistOptions().setForceUpdateByOwner(true));
			changelist.refresh();

			// The changelist description should be updated
			assertTrue(changelist.getDescription().contains("New description test " + randNum));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (superClient != null && superServer != null) {
				try {
					// Delete submitted test files
					IChangelist deleteChangelist = getNewChangelist(superServer,
							superClient,
							"Dev131_ForceUpdateByOwnerChangelistTest delete submitted files");
					deleteChangelist = superClient
							.createChangelist(deleteChangelist);
					superClient.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { targetFile }),
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
