/**
 * Copyright (c) 2018 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features151;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 reconcile -m', checking file modtime instead of digests.
 */
@Jobs({ "job077547" })
@TestId("Dev151_ReconcileUseModTimeTest")
public class ReconcileUseModTimeTest extends P4JavaRshTestCase {

	private static final String clientName = "reconcile-client";
	static long startOfTestEpochTime;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r18.1", ReconcileUseModTimeTest.class.getSimpleName());

	static IClient client;

	/**
	 * @BeforeClass annotation to a method to be run before all tests in a class
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeAll() throws Exception {
		startOfTestEpochTime = new Date().getTime() /1000;
		Properties properties = new Properties();
		properties.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
		setupServer(p4d.getRSHURL(), P4JTEST_SUPERUSERNAME_DEFAULT,
					P4JTEST_SUPERPASSWORD_DEFAULT, false, properties);

		try {
			// Create new client
			String clientRoot = p4d.getPathToRoot() + "/clients/" + clientName;
			String[] paths = {"//depot/rec/... //" + clientName + "/..."};
			IClient testClient = createClient(server, clientName, "reconcile -m test", clientRoot, paths);

			// use super for everything just for simplicity...
			testClient.setOwnerName(P4JTEST_SUPERUSERNAME_DEFAULT);

			// Clean up workspace
			FileUtils.deleteDirectory(new File(clientRoot));
			FileUtils.forceMkdir(new File(clientRoot));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}

	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void cleanup() throws Exception {
		if (server != null) {
			this.endServerSession(server);
		}
	}

	@Test
	public void testReconcileUseModTime() throws Exception {
		p4d.rotateJournal();
		IClient client = server.getClient(clientName);
		String sourceFile = client.getRoot() + File.separator + textBaseFile;
		createTestSourceFile(sourceFile, false);
		IChangelist change = getNewChangelist(server, client, "add test file");
		change.setUsername(P4JTEST_SUPERUSERNAME_DEFAULT);
		change = client.createChangelist(change);
		String journalString = server.getCounter("journal");
		assertNotNull(journalString);
		int journalInt = Integer.parseInt(journalString);

		// ... add to pending change
		List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(sourceFile);
		AddFilesOptions addOpts = new AddFilesOptions();
		addOpts.setChangelistId(change.getId());
		List<IFileSpec> msg = client.addFiles(fileSpecs, addOpts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		String fileType = msg.get(0).getFileType();
		assertTrue(fileType.equals("text") || fileType.equals("text+x"));

		// ... submit file and validate
		msg = change.submit(false);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals(FileAction.ADD, msg.get(0).getAction());

		try {
			// Write something to the file outside of Perforce.
			File testFile = new File(sourceFile);
			testFile.setWritable(true);
			writeFileBytes(sourceFile, "changed file contents", false);

			change = getNewChangelist(server, client, "edit test file");
			change.setUsername(P4JTEST_SUPERUSERNAME_DEFAULT);
			assertNotNull(change);
			change = client.createChangelist(change);
			assertNotNull(change);

			// Reconcile files
			ReconcileFilesOptions options = new ReconcileFilesOptions()
					.setChangelistId(change.getId())
					.setOutsideEdit(true)
					.setCheckModTime(true);
			List<IFileSpec> filespec = FileSpecBuilder.makeFileSpecList(new String[]{sourceFile});
			List<IFileSpec> files = client.reconcileFiles(filespec, options);
			assertNotNull(files);
			assertTrue(files.size() > 0);

			// Check mod time on file
			ExportRecordsOptions exportopt = new ExportRecordsOptions();
			exportopt.setUseJournal(true);
			exportopt.setFormat(false);
			exportopt.setSourceNum(journalInt);
			exportopt.setFilter("table=db.have");
			List<Map<String, Object>> haveDB = server.getExportRecords(exportopt);
			Map<String, Object> testfileHaveRecord = haveDB.get(0);
			Object testFileEpochTimeObject = testfileHaveRecord.get("HAtime");
			long testFileEpochTime = Long.valueOf(testFileEpochTimeObject.toString());
			assertTrue(testFileEpochTime >= startOfTestEpochTime);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (ZipException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if ((client != null) && (change != null) && (change.getStatus() == ChangelistStatus.PENDING)) {
				try {
					// Revert files in pending change
					client.revertFiles(
							change.getFiles(true),
							new RevertFilesOptions()
									.setChangelistId(change.getId()));
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
