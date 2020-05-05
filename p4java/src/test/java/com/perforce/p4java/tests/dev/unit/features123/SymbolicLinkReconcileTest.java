package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SymbolicLinkReconcileTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r17.1", SymbolicLinkReconcileTest.class.getSimpleName());

	private static IClient client = null;
	private static final String clientName = "SymbolicLinkReconcileTestClient";
	private static final String targetFilePath = File.separator + "symlinks" + File.separator + "target";
	private static final String linkPath = File.separator + "symlinks" + File.separator + "link";
	private static final String depotTestPath = "//depot/symlinks/...";

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a class.
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, null);
		assertNotNull(server);
		client = createClient(server, clientName);
		assertNotNull(client);

		String target = client.getRoot() + targetFilePath;
		String link = client.getRoot() + linkPath;

		// Create target file
		createFileOnDisk(target);

		// Create symbolic link
		String path = SymbolicLinkHelper.createSymbolicLink(link, target);

		boolean isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
		assertTrue(isSymlink);

		// Create changelist
		IChangelist changelist = getNewChangelist(server,
				client,
				"add symbolic link.");
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);
		assertNotNull(changelist);

		// Add a file specified as "binary" even though it is "text"
		List<IFileSpec> files = client.addFiles(
				FileSpecBuilder.makeFileSpecList(link, target),
				new AddFilesOptions().setChangelistId(changelist.getId()));
		assertNotNull(files);

		changelist.refresh();
		files = changelist.submit(new SubmitOptions());
		assertNotNull(files);

		// Check target file id a symbolic link
		// Read the target path of the symbolic link
		// Verify the symbolic link has the correct target path
		GetExtendedFilesOptions extendedFilesOptions = new GetExtendedFilesOptions();
		List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(files, extendedFilesOptions);
		assertNotNull(extendedFiles);

		for (IExtendedFileSpec ifs : extendedFiles) {
			Path p = Paths.get(ifs.getClientPathString());
			LinkOption lo = LinkOption.NOFOLLOW_LINKS;
			assertTrue(Files.exists(p, lo));
			if (ifs.getHeadType().toLowerCase().contains("symlink")) {
				assertTrue(SymbolicLinkHelper.isSymbolicLink(ifs.getClientPathString()));
				String linkTarget = SymbolicLinkHelper.readSymbolicLink(path);
				assertNotNull(linkTarget);
				assertEquals(target, linkTarget);
			}
		}
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
	}

	@Test
	public void testSymlinkDeleteAndReconcileWorkspace() throws Exception {

		String linkPathString = client.getRoot() + linkPath;

		// Delete the local symbolic link
		File delFile = new File(linkPathString);
		assertTrue(delFile.delete());

		// Run reconcile.
		List<IFileSpec> files = client.reconcileFiles(
				FileSpecBuilder.makeFileSpecList(depotTestPath),
				new ReconcileFilesOptions().setUpdateWorkspace(true));
		assertNotNull(files);

		// Check for errors in reconcile
		for (IFileSpec file : files) {
			assertTrue(!file.getOpStatus().toString().equals("ERROR"));
		}

		// Check target file id a symbolic link
		// Read the target path of the symbolic link
		// Verify the symbolic link has the correct target path
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

	@Test
	public void testSyncSymlinksAndReconcileWorkspaceWithoutChanges() throws Exception {

		List<IFileSpec> files = null;

		client.sync(FileSpecBuilder.makeFileSpecList(depotTestPath),new SyncOptions().setForceUpdate(true));
		// Reconcile files
		for (int i = 0; i < 10; i++) {
			files = client.reconcileFiles(
					FileSpecBuilder.makeFileSpecList(depotTestPath),
					new ReconcileFilesOptions().setUpdateWorkspace(true));
			assertNotNull(files);
			// p4ic4idea: IServerMessage
			assertTrue(files.get(0).getStatusMessage().hasMessageFragment("//depot/symlinks/... - no file(s) to reconcile."));
		}

		// Check target file id a symbolic link
		// Read the target path of the symbolic link
		// Verify the symbolic link has the correct target path
		GetExtendedFilesOptions extendedFilesOptions = new GetExtendedFilesOptions();
		List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(FileSpecBuilder.makeFileSpecList(depotTestPath), extendedFilesOptions);
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

	@Test
	public void testReconcileWorkspaceDoesNotChangeFolderPermissions() throws Exception {

		attemptCleanupFiles(client);
		Path folderName = Paths.get(client.getRoot() + File.separator + "folder" + File.separator + "subfolder");
		Path testFilePath = Paths.get(folderName + File.separator + "test1.txt");
		createFileOnDisk(testFilePath.toString());
		File testFile = createFileObject(testFilePath.toString());

		// Create symbolic link
		Path symlinkFilePath = Paths.get(client.getRoot() + File.separator + File.separator + "folder" + File.separator + "symlink");

		Files.createSymbolicLink(symlinkFilePath, folderName);
		File symlinkFile = createFileObject(symlinkFilePath.toString());

		IChangelist change = createNewChangelist(client, "Create sym link, a folder and a file in that folder");
		AddFileToChangelist(testFile, change, client);
		AddFileToChangelist(symlinkFile, change, client);
		submitChangelist(change);

		// Run reconcile.
		List<IFileSpec> files = client.reconcileFiles(
				FileSpecBuilder.makeFileSpecList(depotTestPath),
				new ReconcileFilesOptions().setUpdateWorkspace(true));
		assertNotNull(files);

		// Check for errors in reconcile
		for (IFileSpec file : files) {
			assertTrue(file.getOpStatus().toString().equals("VALID"));
		}
		Path testP = Paths.get(client.getRoot() + File.separator + File.separator + "folder");
		assertTrue(testP.toFile().isDirectory());
		assertTrue(testP.toFile().canExecute());

	}

	@Test
	public void testSyncSymlinksAndReconcileWithoutChanges() throws Exception {
		List<IFileSpec> files = null;
		ReconcileFilesOptions recOpts = new ReconcileFilesOptions();
		recOpts.setOutsideEdit(true);
		recOpts.setOutsideAdd(true);
		recOpts.setUseWildcards(true);

		// Reconcile files
		for (int i = 0; i < 10; i++) {
			files = client.reconcileFiles(
					FileSpecBuilder.makeFileSpecList(depotTestPath),
					recOpts);
			assertNotNull(files);
			// p4ic4idea: IServerMessage
			assertTrue(files.get(0).getStatusMessage().hasMessageFragment("//depot/symlinks/... - no file(s) to reconcile."));
		}

		// Check target file is a symbolic link
		// Read the target path of the symbolic link
		// Verify the symbolic link has the correct target path
		GetExtendedFilesOptions extendedFilesOptions = new GetExtendedFilesOptions();
		List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(FileSpecBuilder.makeFileSpecList(depotTestPath), extendedFilesOptions);
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

	@Test
	public void testReconcileDoesNotChangeFolderPermissions() throws Exception {
		attemptCleanupFiles(client);
		Path folderName = Paths.get(client.getRoot() + File.separator + "symlinks" + File.separator + "subfolder");
		Path testFilePath = Paths.get(folderName + File.separator + "test1.txt");
		createFileOnDisk(testFilePath.toString());
		File testFile = createFileObject(testFilePath.toString());

		// Create symbolic link
		Path symlinkFilePath = Paths.get(client.getRoot() + File.separator + "symlinks" + File.separator + "symlink");
		Files.createSymbolicLink(symlinkFilePath, folderName);
		File symlinkFile = createFileObject(symlinkFilePath.toString());
		IChangelist change = createNewChangelist(client, "Create sym link, a folder and a file in that folder");
		AddFileToChangelist(testFile, change, client);
		AddFileToChangelist(symlinkFile, change, client);
		submitChangelist(change);

		// Run reconcile.
		List<IFileSpec> files = client.reconcileFiles(
				FileSpecBuilder.makeFileSpecList(depotTestPath),
				new ReconcileFilesOptions().setUseWildcards(true).setOutsideAdd(true).setOutsideEdit(true));
		assertNotNull(files);

		assertTrue(files.get(0).getOpStatus().toString().equals("ERROR"));

		Path testP = Paths.get(client.getRoot() + File.separator + File.separator + "symlinks");
		assertTrue(testP.toFile().isDirectory());
		assertTrue(testP.toFile().canExecute());
	}

}
