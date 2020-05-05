package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SymbolicLinkTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r17.1", SymbolicLinkTest.class.getSimpleName());

	private static final String TEST_DIR = "//depot/symlinks/...";
	private static final String RELATIVE_LINK_PATH = File.separator + "symlinks" + File.separator + "symlink";
	private static final String SETUP_CLIENT_NAME = "setupSymbolicLinkTestClient";

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a class.
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, null);
		assertNotNull(server);
		IClient client = createClient(server, SETUP_CLIENT_NAME);
		assertNotNull(client);

		String target = client.getRoot() + File.separator + "symlinks" + File.separator + "target";
		String link = client.getRoot() + RELATIVE_LINK_PATH;
		String directoryLink = client.getRoot() + File.separator + "directorySymlink";
		String targetDirectory = client.getRoot() + File.separator + "symlinks";
		String nestedSymlink = client.getRoot() + File.separator + "nestedSymlinks" + File.separator + "nestedSymlink";
		String toBeDeletedSymlink = client.getRoot() + File.separator + "deleteSymlinkTest" + File.separator + "toBeDeletedSymlink";
		String toBeEditedSymlink = client.getRoot() + File.separator + "editSymlinkTest" + File.separator + "toBeEdited";
		String toBeDeletedDirSymlink = client.getRoot() + File.separator + "deleteDirectorySymlinkTest" + File.separator + "toBeDeletedDirSymlink";

		// Create target file on disk
		createFileOnDisk(target);

		// Create symbolic link on disk
		String path = SymbolicLinkHelper.createSymbolicLink(link, target);
		boolean isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create directory symbolic link on disk
		path = SymbolicLinkHelper.createSymbolicLink(directoryLink, targetDirectory);
		isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create nested symlink
		path = SymbolicLinkHelper.createSymbolicLink(nestedSymlink, link);
		isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create symlink that is to be deleted
		path = SymbolicLinkHelper.createSymbolicLink(toBeDeletedSymlink, target);
		isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create symlink that is to be edited
		path = SymbolicLinkHelper.createSymbolicLink(toBeEditedSymlink, target);
		isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create directory symlink that is to be deleted
		path = SymbolicLinkHelper.createSymbolicLink(toBeDeletedDirSymlink, targetDirectory);
		isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create changelist
		IChangelist changelist = getNewChangelist(server,
				client,
				"add symbolic link.");
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);
		assertNotNull(changelist);

		// Add files to changelist
		List<IFileSpec> files = client.addFiles(
				FileSpecBuilder.makeFileSpecList(link, target, directoryLink, nestedSymlink, toBeDeletedSymlink, toBeEditedSymlink, toBeDeletedDirSymlink),
				new AddFilesOptions().setChangelistId(changelist.getId()));
		assertNotNull(files);

		// Submit changelist
		changelist.refresh();
		files = changelist.submit(new SubmitOptions());
		assertNotNull(files);

	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
	}

	@Test
	public void testSyncSymlinksSeveralTimes() throws Exception {
		IClient multipleSyncTestClient = createClient(server, "TestSymbolicLinkMultipleSyncTestClient");
		assertNotNull(multipleSyncTestClient);

		// Sync files
		List<IFileSpec> files = multipleSyncTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Sync files
		files = multipleSyncTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR + "#0"),
				new SyncOptions().setClientBypass(true));
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Sync files
		files = multipleSyncTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions().setForceUpdate(true));
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 2);

		// Sync files with no changes (check digest and have-list are ok)
		files = multipleSyncTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in workspace
		assertEquals(1, files.size());
		assertTrue(files.get(0).getStatusMessage().contains("file(s) up-to-date."));
	}

	@Test
	public void testSyncSymlinksCanOverwriteOfflineChanges() throws Exception {
		IClient offlineChangesTestClient = createClient(server, "TestSymbolicLinkOfflineChangesTestClient");
		assertNotNull(offlineChangesTestClient);

		// Sync files
		List<IFileSpec> files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		/**
		 * Test syncing a link over a text file
		 */
		// Clear have list - Sync files #0
		files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR + "#0"),
				new SyncOptions().setClientBypass(true));
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Make offline changes(replace link with a text file)
		String link = offlineChangesTestClient.getRoot() + RELATIVE_LINK_PATH;
		Path linkPath = Paths.get(link);
		Files.delete(linkPath);
		createFileOnDisk(linkPath.toString());

		// Force Sync files
		files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions().setForceUpdate(true));
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 2);

		/**
		 * Test syncing a file over a symlink
		 */
		// Clear have list - Sync files #0
		files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR + "#0"),
				new SyncOptions().setClientBypass(true));
		assertNotNull(files);

		// Make offline changes(replace target text file with link)
		String target = offlineChangesTestClient.getRoot() + File.separator + "symlinks" + File.separator + "target";
		Path targetPath = Paths.get(target);
		File targetFile = new File(target);
		targetFile.setWritable(true);
		Files.delete(targetPath);
		SymbolicLinkHelper.createSymbolicLink(target, offlineChangesTestClient.getRoot() + File.separator + "symlinks" + File.separator + "fakeTarget");

		// Force Sync files
		files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions().setForceUpdate(true));
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 2);

		/**
		 * Test syncing a link over a directory file
		 */
		// Clear have list - Sync files #0
		files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR + "#0"),
				new SyncOptions().setClientBypass(true));
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Make offline changes(replace link with a text file)
		String dir = offlineChangesTestClient.getRoot() + RELATIVE_LINK_PATH;
		Files.delete(Paths.get(dir));
		Files.createDirectories(Paths.get(dir));

		// Force Sync files
		files = offlineChangesTestClient.sync(
				FileSpecBuilder.makeFileSpecList(TEST_DIR),
				new SyncOptions().setForceUpdate(true));
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 2);
	}

	@Test
	public void testSyncDirectorySymlinks() throws Exception {
		final String directoryTestDir = "//depot/directorySymlink";


		// Create a client to test directory symlinks
		IClient syncDirectorySymlinkClient = createClient(server, "syncDirectorySymlinkClient");
		assertNotNull(syncDirectorySymlinkClient);

		// Sync files
		List<IFileSpec> files = syncDirectorySymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(directoryTestDir, TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);

		// Sync files
		files = syncDirectorySymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(directoryTestDir + "#0", TEST_DIR + "#0"),
				new SyncOptions().setClientBypass(true));
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Sync files
		files = syncDirectorySymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(directoryTestDir, TEST_DIR),
				new SyncOptions().setForceUpdate(true));
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);
	}

	@Test
	public void testSyncNestedSymlinks() throws Exception {
		final String nestedSymlinkTestDir = "//depot/nestedSymlinks/...";

		// Create a client to test directory symlinks
		IClient syncNestedSymlinkClient = createClient(server, "syncNestedSymlinkClient");
		assertNotNull(syncNestedSymlinkClient);

		// Sync files
		List<IFileSpec> files = syncNestedSymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(nestedSymlinkTestDir, TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);

		// Sync files
		files = syncNestedSymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(nestedSymlinkTestDir + "#0", TEST_DIR + "#0"),
				new SyncOptions().setClientBypass(true));
		assertNotNull(files);

		// Check for errors in sync
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Sync files
		files = syncNestedSymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(nestedSymlinkTestDir, TEST_DIR),
				new SyncOptions().setForceUpdate(true));
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);
	}

	@Test
	public void testDeleteSymlinks() throws Exception {
		String deleteTestSymlinkDir = "//depot/deleteSymlinkTest/...";

		// Create a client to test directory symlinks8
		IClient deleteSymlinkClient = createClient(server, "deleteSymlinkClient");
		assertNotNull(deleteSymlinkClient);

		// Sync files
		List<IFileSpec> files = deleteSymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(deleteTestSymlinkDir, TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);

		// Creating changelist for delete
		IChangelist deleteChangelist = getNewChangelist(server, deleteSymlinkClient, "deleting symlink");
		deleteChangelist = deleteSymlinkClient.createChangelist(deleteChangelist);
		assertNotNull(deleteChangelist);

		// Delete Symlink
		List<IFileSpec> changelistFiles = deleteSymlinkClient.deleteFiles(FileSpecBuilder.makeFileSpecList(deleteTestSymlinkDir),
				new DeleteFilesOptions()
						.setChangelistId(deleteChangelist.getId()));
		assertEquals(1, changelistFiles.size());
		deleteChangelist.refresh();
		List<IFileSpec> deleteSymlinkResponse = deleteChangelist.submit(null);
		assertNotNull(deleteSymlinkResponse);
		assertFalse(Files.exists(Paths.get(deleteSymlinkClient.getRoot() + File.separator + "deleteSymlinkTest" + File.separator + "toBeDeletedSymlink")));
		IClient client = server.getClient(SETUP_CLIENT_NAME);
		assertTrue(Files.exists(Paths.get(client.getRoot() + File.separator + "symlinks" + File.separator + "target")));

		// Check for errors
		assertEquals(2, deleteSymlinkResponse.size());

		for (IFileSpec message : deleteSymlinkResponse) {
			assertTrue(!message.getOpStatus().toString().equals("ERROR"));
		}
	}

	@Test
	public void testEditSymlink() throws Exception {
		String editTestSymlinkDir = "//depot/editSymlinkTest/...";

		// Create a client to test directory symlinks8
		IClient editSymlinkClient = createClient(server, "editSymlinkClient");
		assertNotNull(editSymlinkClient);

		// Sync files
		List<IFileSpec> files = editSymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(editTestSymlinkDir, TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);

		// Creating changelist for edit
		IChangelist editChangelist = getNewChangelist(server, editSymlinkClient, "editing symlink");
		editChangelist = editSymlinkClient.createChangelist(editChangelist);
		assertNotNull(editChangelist);

		// Open for symlink for  edit
		List<IFileSpec> editedFiles = editSymlinkClient.editFiles(FileSpecBuilder.makeFileSpecList(editTestSymlinkDir), new EditFilesOptions().setChangelistId(editChangelist.getId()));
		assertEquals(1, editedFiles.size());

		// Edit the symlink to point at another file
		String toBeEditedSymlink = editSymlinkClient.getRoot() + File.separator + "editSymlinkTest" + File.separator + "toBeEdited";
		Path targetPath = Paths.get(toBeEditedSymlink);
		Files.delete(targetPath);
		SymbolicLinkHelper.createSymbolicLink(toBeEditedSymlink, editSymlinkClient.getRoot() + File.separator + "symlinks" + File.separator + "fakeTarget");

		// Submit changelist
		editChangelist.refresh();
		List<IFileSpec> editSymlinkResponse = editChangelist.submit(null);
		assertNotNull(editSymlinkResponse);

		// Check for errors
		assertEquals(2, editSymlinkResponse.size());

		for (IFileSpec message : editSymlinkResponse) {
			assertTrue(!message.getOpStatus().toString().equals("ERROR"));
		}
	}

	@Test
	public void testDeleteDirectorySymlinks() throws Exception {
		String deleteDirectoryTestSymlinkDir = "//depot/deleteDirectorySymlinkTest/...";

		// Create a client to test directory symlinks8
		IClient deleteDirectorySymlinkClient = createClient(server, "deleteDirectorySymlinkClient");
		assertNotNull(deleteDirectorySymlinkClient);

		// Sync files
		List<IFileSpec> files = deleteDirectorySymlinkClient.sync(
				FileSpecBuilder.makeFileSpecList(deleteDirectoryTestSymlinkDir, TEST_DIR),
				new SyncOptions());
		assertNotNull(files);

		// Check for errors in workspace
		checkWorkspaceState(files, 3);

		// Creating changelist for delete
		IChangelist deleteChangelist = getNewChangelist(server, deleteDirectorySymlinkClient, "deleting symlink");
		deleteChangelist = deleteDirectorySymlinkClient.createChangelist(deleteChangelist);
		assertNotNull(deleteChangelist);

		// Delete Symlink
		List<IFileSpec> changelistFiles = deleteDirectorySymlinkClient.deleteFiles(FileSpecBuilder.makeFileSpecList(deleteDirectoryTestSymlinkDir),
				new DeleteFilesOptions()
						.setChangelistId(deleteChangelist.getId()));
		assertEquals(1, changelistFiles.size());
		deleteChangelist.refresh();
		List<IFileSpec> deleteSymlinkResponse = deleteChangelist.submit(null);
		assertNotNull(deleteSymlinkResponse);
		assertFalse(Files.exists(Paths.get(deleteDirectorySymlinkClient.getRoot() + File.separator + "deleteDirectorySymlinkTest" + File.separator + "toBeDeletedDirSymlink")));
		IClient client = server.getClient(SETUP_CLIENT_NAME);
		assertTrue(Files.exists(Paths.get(client.getRoot() + File.separator + "symlinks" + File.separator + "symlink")));
		assertTrue(Files.exists(Paths.get(client.getRoot() + File.separator + "symlinks" + File.separator + "target")));

		// Check for errors
		assertEquals(2, deleteSymlinkResponse.size());

		for (IFileSpec message : deleteSymlinkResponse) {
			assertTrue(!message.getOpStatus().toString().equals("ERROR"));
		}
	}

	/**
	 * Check target file is a symbolic link.
	 */
	private void checkWorkspaceState(List<IFileSpec> files, int expectedNoOfMessages) throws P4JavaException {
		// check that both files are synced
		assertEquals(expectedNoOfMessages, files.size());

		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Read the target path of the symbolic link and verify the symbolic link has the correct target path
		GetExtendedFilesOptions extendedFilesOptions = new GetExtendedFilesOptions();
		List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(files, extendedFilesOptions);
		assertNotNull(extendedFiles);
		for (IExtendedFileSpec extendedFileSpec : extendedFiles) {
			Path path = Paths.get(extendedFileSpec.getClientPathString());
			LinkOption linkOption = LinkOption.NOFOLLOW_LINKS;
			assertTrue(Files.exists(path, linkOption));
			if (extendedFileSpec.getHeadType().toLowerCase().contains("symlink")) {
				assertTrue(SymbolicLinkHelper.isSymbolicLink(path.toString()));

			} else {
				assertTrue(!SymbolicLinkHelper.isSymbolicLink(path.toString()));
			}
		}
	}

}
