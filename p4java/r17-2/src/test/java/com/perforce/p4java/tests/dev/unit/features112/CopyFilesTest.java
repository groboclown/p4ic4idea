/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 copy" produce revision ranges like those produced by "p4 integ" in
 * most cases.
 */
@Jobs({ "job046680" })
@TestId("Dev112_CopyTest")
public class CopyFilesTest extends P4JavaTestCase {

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
	 * Test "p4 copy" produce revision ranges like those produced by "p4 integ"
	 * in most cases.
	 */
	@Test
	public void testCopyFiles() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ "p4cmd/P4CmdDispatcher.java";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
				+ dir + "/P4CmdDispatcher.java";

		try {
			// Copy the source file rev #3 (the seed file has 6 revs) to target
			changelist = getNewChangelist(server, client,
					"Dev112_CopyTest copy files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			FileSpec sourceSpec = new FileSpec(sourceFile);
			sourceSpec.setEndRevision(3);
			FileSpec targetSpec = new FileSpec(targetFile);
			files = client.copyFiles(sourceSpec, targetSpec, null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Retrieve the revision history ('filelog') of the copied file
			Map<IFileSpec, List<IFileRevisionData>> fileRevisionHisotryMap = server
					.getRevisionHistory(
							FileSpecBuilder.makeFileSpecList(targetFile),
							new GetRevisionHistoryOptions());
			assertNotNull(fileRevisionHisotryMap);
			assertEquals(1, fileRevisionHisotryMap.size());
			Map.Entry<IFileSpec, List<IFileRevisionData>> entry = fileRevisionHisotryMap
					.entrySet().iterator().next();
			assertNotNull(entry);
			List<IFileRevisionData> fileRevisionDataList = entry.getValue();
			assertNotNull(fileRevisionDataList);
			assertEquals(1, fileRevisionDataList.size());
			IFileRevisionData fileRevisionData = fileRevisionDataList.get(0);
			assertNotNull(fileRevisionData);
			List<IRevisionIntegrationData> revisionIntegrationDataList = fileRevisionData
					.getRevisionIntegrationDataList();
			assertEquals(1, revisionIntegrationDataList.size());
			IRevisionIntegrationData revisionIntegrationData = revisionIntegrationDataList
					.get(0);
			assertNotNull(revisionIntegrationData);

			// Verify the revisions
			assertEquals(-1, revisionIntegrationData.getStartFromRev());
			assertEquals(3, revisionIntegrationData.getEndFromRev());
			assertEquals("branch from", revisionIntegrationData.getHowFrom());
			assertEquals(sourceFile, revisionIntegrationData.getFromFile());

			// Copy the HEAD (rev #6) revision of the same source file to target
			changelist = getNewChangelist(server, client,
					"Dev112_CopyTest copy files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			sourceSpec = new FileSpec(sourceFile);
			files = client.copyFiles(sourceSpec, targetSpec, null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Retrieve the revision history ('filelog') of the copied file
			fileRevisionHisotryMap = server.getRevisionHistory(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new GetRevisionHistoryOptions());
			assertNotNull(fileRevisionHisotryMap);
			assertEquals(1, fileRevisionHisotryMap.size());
			entry = fileRevisionHisotryMap.entrySet().iterator().next();
			assertNotNull(entry);
			fileRevisionDataList = entry.getValue();
			assertNotNull(fileRevisionDataList);

			// Should have 2 revision data
			assertEquals(2, fileRevisionDataList.size());

			// First revision data
			fileRevisionData = fileRevisionDataList.get(0);
			assertNotNull(fileRevisionData);
			revisionIntegrationDataList = fileRevisionData
					.getRevisionIntegrationDataList();
			assertEquals(1, revisionIntegrationDataList.size());
			revisionIntegrationData = revisionIntegrationDataList.get(0);
			assertNotNull(revisionIntegrationData);

			// Verify the revisions
			assertEquals(3, revisionIntegrationData.getStartFromRev());
			assertEquals(6, revisionIntegrationData.getEndFromRev());
			assertEquals("copy from", revisionIntegrationData.getHowFrom());
			assertEquals(sourceFile, revisionIntegrationData.getFromFile());

			// Second revision data
			fileRevisionData = fileRevisionDataList.get(1);
			assertNotNull(fileRevisionData);
			revisionIntegrationDataList = fileRevisionData
					.getRevisionIntegrationDataList();
			assertEquals(1, revisionIntegrationDataList.size());
			revisionIntegrationData = revisionIntegrationDataList.get(0);
			assertNotNull(revisionIntegrationData);

			// Verify the revisions
			assertEquals(-1, revisionIntegrationData.getStartFromRev());
			assertEquals(3, revisionIntegrationData.getEndFromRev());
			assertEquals("branch from", revisionIntegrationData.getHowFrom());
			assertEquals(sourceFile, revisionIntegrationData.getFromFile());

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
							client, "Dev112_CopyTest delete submitted files");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					client.deleteFiles(FileSpecBuilder
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
