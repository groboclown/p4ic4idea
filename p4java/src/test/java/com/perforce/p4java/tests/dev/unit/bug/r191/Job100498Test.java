package com.perforce.p4java.tests.dev.unit.bug.r191;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

// p4ic4idea: IServerMessage
import com.perforce.p4java.server.IServerMessage;


// p4ic4idea: this test is very flaky
public class Job100498Test extends P4JavaRshTestCase {

	@Rule
	public SimpleServerRule p4d = new SimpleServerRule("r18.1", Job100498Test.class.getSimpleName());

	@Test
	public void testCheckFilePathOK() throws Exception {
		String clientName = "testCheckFilePathOK";
		String clientRoot = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String[] clientViews = {"//depot/basic/readonly/sync/... //" + clientName + "/..."};

		Properties props = new Properties();
		props.put(PropertyDefs.CLIENT_PATH_KEY, clientRoot);

		setupServer(p4d.getRSHURL(), userName, password, true, props);
		client = createClient(server, clientName, "Description", clientRoot, clientViews);

		// Sync files
		List<IFileSpec> files = client.sync(null, null);

		// Check synced files for errors
		assertFileSpecError(files);
		validateFileSpecs(files);
	}

	@Test
	public void testCheckSymlinkWithRestrictSymlinkEnabled() throws Exception {
		String clientName = "testCheckSymlinkWithRestrictSymlinkEnabled";
		String clientRoot = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String falseRoot = p4d.getPathToRoot() + File.separator + "false";
		String[] clientViews = {"//depot/basic/readonly/sync/... //" + clientName + "/..."};

		Properties props = new Properties();
		props.put(PropertyDefs.CLIENT_PATH_KEY, clientRoot);
		props.put(PropertyDefs.FILESYS_RESTRICTSYMLINKS, "1");

		setupServer(p4d.getRSHURL(), userName, password, true, props);
		client = createClient(server, clientName, "Description", clientRoot, clientViews);

		// Create target file
		String root = client.getRoot();
		Path sourceSymlinkTestFilePath = Paths.get(root + File.separator + "testsym/target.txt");
		createFileOnDisk(sourceSymlinkTestFilePath.toString());
		File sourceSymlinkTestFile = createFileObject(sourceSymlinkTestFilePath.toString());

		// Create symlinks
		Path goodSymlink = Paths.get(root + File.separator + "testsym/good_symlink");
		Files.createSymbolicLink(goodSymlink, sourceSymlinkTestFilePath);
		File goodSymlinkFile = createFileObject(goodSymlink.toString());

		Path badSymlink = Paths.get(root + File.separator + "testsym/bad_symlink");
		Files.createSymbolicLink(badSymlink, Paths.get(falseRoot));
		File badSymlinkFile = createFileObject(badSymlink.toString());

		// Submit files
		IChangelist change = createNewChangelist(client, "test");
		AddFileToChangelist(sourceSymlinkTestFile, change, client);
		AddFileToChangelist(goodSymlinkFile, change, client);
		AddFileToChangelist(badSymlinkFile, change, client);
		submitChangelist(change);

		// Remove files
		cleanupFiles(client);

		// Sync files
		SyncOptions syncOpts = new SyncOptions();
		syncOpts.setForceUpdate(true);
		List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList("//..."), syncOpts);

		// Check that bad symlink wasn't synced
		assertFalse(badSymlinkFile.exists());

		// Check that 1 file was unsuccessful in the sync
		// p4ic4idea: IServerMessage
		List<IServerMessage> errors = getErrorsFromFileSpecList(files);
		assertNotNull(errors);
		assertTrue(errors.size() == 1);
		assertTrue(errors.get(0).hasMessageFragment("is not inside permitted filesystem path"));

		// Check that some files were successful in the sync
		List<IServerMessage> valids = getMessagesFromFileSpecList(FileSpecOpStatus.VALID, files);
		assertNotNull(valids);
		assertTrue(valids.size() > 2);
		assertTrue(files.size() == valids.size() + errors.size());
	}

	@Test
	public void testCheckSymlinkWithRestrictSymlinkDisabled() throws Exception {
		String clientName = "testCheckSymlinkWithRestrictSymlinkDisabled";
		String clientRoot = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String falseRoot = p4d.getPathToRoot() + File.separator + "false";
		String[] clientViews = {"//depot/basic/readonly/sync/... //" + clientName + "/..."};

		Properties props = new Properties();
		props.put(PropertyDefs.CLIENT_PATH_KEY, clientRoot);
		props.put(PropertyDefs.FILESYS_RESTRICTSYMLINKS, "0");

		setupServer(p4d.getRSHURL(), userName, password, true, props);
		client = createClient(server, clientName, "Description", clientRoot, clientViews);

		// Create target file
		String root = client.getRoot();
		Path sourceSymlinkTestFilePath = Paths.get(root + File.separator + "testsym/target.txt");
		createFileOnDisk(sourceSymlinkTestFilePath.toString());
		File sourceSymlinkTestFile = createFileObject(sourceSymlinkTestFilePath.toString());

		// Create symlinks
		Path goodSymlink = Paths.get(root + File.separator + "testsym/good_symlink");
		Files.createSymbolicLink(goodSymlink, sourceSymlinkTestFilePath);
		File goodSymlinkFile = createFileObject(goodSymlink.toString());

		Path badSymlink = Paths.get(root + File.separator + "testsym/bad_symlink");
		Files.createSymbolicLink(badSymlink, Paths.get(falseRoot));
		File badSymlinkFile = createFileObject(badSymlink.toString());

		// Submit files
		IChangelist change = createNewChangelist(client, "test");
		AddFileToChangelist(sourceSymlinkTestFile, change, client);
		AddFileToChangelist(goodSymlinkFile, change, client);
		AddFileToChangelist(badSymlinkFile, change, client);
		submitChangelist(change);

		// Sync files
		SyncOptions syncOpts = new SyncOptions();
		syncOpts.setForceUpdate(true);
		List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList("//..."), syncOpts);

		// Check synced files for errors
		assertFileSpecError(files);
		validateFileSpecs(files);
	}

	@Test
	public void testCheckFilePathBAD() throws Exception {
		String clientName = "testCheckFilePathBAD";
		String clientRoot = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String falseRoot = p4d.getPathToRoot() + File.separator + "false";
		String[] clientViews = {"//depot/basic/readonly/sync/... //" + clientName + "/..."};

		Properties props = new Properties();
		props.put(PropertyDefs.CLIENT_PATH_KEY, falseRoot);

		setupServer(p4d.getRSHURL(), userName, password, true, props);
		client = createClient(server, clientName, "Description", clientRoot, clientViews);

		// Sync files
		List<IFileSpec> files = client.sync(null, null);

		// Check that sync was unsuccessful
		// p4ic4idea: IServerMessage
		List<IServerMessage> errors = getErrorsFromFileSpecList(files);
		assertNotNull(errors);
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).hasMessageFragment("is not inside permitted filesystem path"));
	}

	@Test
	public void testCheckFilePathOKWithMultiplePaths() throws Exception {
		String clientName = "testCheckFilePathOKWithMultiplePaths";
		String clientRoot = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String falseRoot = p4d.getPathToRoot() + File.separator + "false";
		String[] clientViews = {"//depot/basic/readonly/sync/... //" + clientName + "/..."};
		String clientPath = falseRoot + ";" + clientRoot;

		Properties props = new Properties();
		props.put(PropertyDefs.CLIENT_PATH_KEY, clientPath);

		setupServer(p4d.getRSHURL(), userName, password, true, props);
		client = createClient(server, clientName, "Description", clientRoot, clientViews);

		// Sync files
		List<IFileSpec> files = client.sync(null, null);

		// Check synced files for errors
		assertFileSpecError(files);
		validateFileSpecs(files);
	}

	@Test
	public void testCheckFilePathOKWithSubDirectories() throws Exception {
		String clientName = "testCheckFilePathOKWithSubDirectories";
		String clientRoot = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String clientRootSubDir1 = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString() + File.separator + "p4cmd" + File.separator + "bin";
		String clientRootSubDir2 = Paths.get(p4d.getPathToRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString() + File.separator + "p4cmd" + File.separator + "src";
		String[] clientViews = {"//depot/basic/readonly/sync/... //" + clientName + "/..."};
		String clientPath = clientRootSubDir1 + ";" + clientRootSubDir2;

		Properties props = new Properties();
		props.put(PropertyDefs.CLIENT_PATH_KEY, clientPath);

		setupServer(p4d.getRSHURL(), userName, password, true, props);
		client = createClient(server, clientName, "Description", clientRoot, clientViews);

		// Sync files
		List<IFileSpec> files = client.sync(null, null);

		// Check that 3 files were unsuccessful in the sync
		// p4ic4idea: IServerMessage
		List<IServerMessage> errors = getErrorsFromFileSpecList(files);
		assertNotNull(errors);
		assertTrue(errors.size() == 3);

		// Check that some files were successful in the sync
		// p4ic4idea: IServerMessage
		List<IServerMessage> valids = getMessagesFromFileSpecList(FileSpecOpStatus.VALID, files);
		assertNotNull(valids);
		assertTrue(valids.size() > 20);
		assertTrue(files.size() == valids.size() + errors.size());	}
}
