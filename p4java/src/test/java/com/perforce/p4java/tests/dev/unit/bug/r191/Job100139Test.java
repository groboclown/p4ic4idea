package com.perforce.p4java.tests.dev.unit.bug.r191;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Job100139Test extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r18.1", Job100139Test.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			setupServer(p4d.getRSHURL(), superUserName, superUserPassword, false, null);
			client = createClient(server, "Job100139TestClient");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() {
		// cleanup code (after each test).
		attemptCleanupFiles(client);
		if (server != null) {
			this.endServerSession(server);
		}
	}

	@Test
	public void testSymLinkSubmitAndSync() throws Exception {
		// Create target file
		String root = client.getRoot();
		Path sourceSymlinkTestFilePath = Paths.get(root + File.separator + "testroot/target1.txt");
		createFileOnDisk(sourceSymlinkTestFilePath.toString());
		File sourceSymlinkTestFile = createFileObject(sourceSymlinkTestFilePath.toString());

		//Create symlink
		Path symlinkTestFilePath = Paths.get(root + File.separator + "testroot/symlink");
		Files.createSymbolicLink(symlinkTestFilePath, sourceSymlinkTestFilePath);
		File symlinkTestFile = createFileObject(symlinkTestFilePath.toString());

		// Submit files
		IChangelist change = createNewChangelist(client, "test");
		AddFileToChangelist(sourceSymlinkTestFile, change, client);
		AddFileToChangelist(symlinkTestFile, change, client);
		submitChangelist(change);

		//Delete symbolic link
		deleteDepotFileFromServerSideIfExist(FileSpecBuilder.makeFileSpecList(symlinkTestFilePath.toString()));
		deleteFile(symlinkTestFilePath.toString(), server, client, "delete symbolic link");
		//Change another target
		Path newSymlinkTargetFilePath = Paths.get(root + File.separator + "testroot/target2.txt");
		createFileOnDisk(newSymlinkTargetFilePath.toString());
		File newSymlinkTargetFile = createFileObject(newSymlinkTargetFilePath.toString());

		//Create another symlink to point ot new target
		Files.createSymbolicLink(symlinkTestFilePath, newSymlinkTargetFilePath);
		File newSymlinkTestFile = createFileObject(symlinkTestFilePath.toString());

		IChangelist change2 = createNewChangelist(client, "test2");
		AddFileToChangelist(newSymlinkTargetFile, change2, client);
		AddFileToChangelist(newSymlinkTestFile, change2, client);
		submitChangelist(change2);

		//Sync to different changes and ensure no clobber error is thrown. The test will fail if there are errors.
		String[] filePaths = new String[]{symlinkTestFilePath + "@" + change.getId(), sourceSymlinkTestFilePath + "@" + change.getId()};
		List<IFileSpec> fileSpecs = client.sync(FileSpecBuilder.makeFileSpecList(filePaths), null);

		//Check the fileSpecs are not null and valid.
		assertNotNull(fileSpecs);
		assertEquals(FileSpecOpStatus.VALID, fileSpecs.get(0).getOpStatus());
		// p4ic4idea: IServerMessage
		assertTrue(fileSpecs.get(1).getStatusMessage().hasMessageFragment("target1.txt"));
		// p4ic4idea: IServerMessage
		assertTrue(fileSpecs.get(1).getStatusMessage().hasMessageFragment("file(s) up-to-date"));

		String[] filePaths2 = new String[]{symlinkTestFilePath + "@" + change2.getId(), newSymlinkTargetFilePath + "@" + change2.getId()};
		fileSpecs = client.sync(FileSpecBuilder.makeFileSpecList(filePaths2), null);

		//Check the fileSpecs are not null and valid.
		assertNotNull(fileSpecs);
		assertEquals(FileSpecOpStatus.VALID, fileSpecs.get(0).getOpStatus());
		// p4ic4idea: IServerMessage
		assertTrue(fileSpecs.get(1).getStatusMessage().hasMessageFragment("target2.txt"));
		// p4ic4idea: IServerMessage
		assertTrue(fileSpecs.get(1).getStatusMessage().hasMessageFragment("file(s) up-to-date"));

		String filePaths3 = "//depot/testroot/..." + "@" + change.getId();
		fileSpecs = client.sync(FileSpecBuilder.makeFileSpecList(filePaths3), null);

		//Check the fileSpecs are not null and valid.
		assertNotNull(fileSpecs);
		assertEquals(2, fileSpecs.size());
		assertEquals(FileSpecOpStatus.VALID, fileSpecs.get(0).getOpStatus());
		assertEquals(FileSpecOpStatus.VALID, fileSpecs.get(1).getOpStatus());

		String filePaths4 = "//depot/testroot/..." + "@" + change2.getId();
		fileSpecs = client.sync(FileSpecBuilder.makeFileSpecList(filePaths4), null);

		//Check the fileSpecs are not null and valid.
		assertNotNull(fileSpecs);
		assertEquals(2, fileSpecs.size());
		assertEquals(FileSpecOpStatus.VALID, fileSpecs.get(0).getOpStatus());
		assertEquals(FileSpecOpStatus.VALID, fileSpecs.get(1).getOpStatus());

		String filePaths5 = "//depot/testroot/..." + "@" + change2.getId();
		fileSpecs = client.sync(FileSpecBuilder.makeFileSpecList(filePaths5), null);

		//Check the fileSpecs are not null and valid.
		assertNotNull(fileSpecs);
		assertEquals(1, fileSpecs.size());
		assertEquals(FileSpecOpStatus.ERROR, fileSpecs.get(0).getOpStatus());

	}
}
