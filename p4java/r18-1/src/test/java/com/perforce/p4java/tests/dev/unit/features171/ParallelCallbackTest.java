package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.DefaultParallelSync;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.ParallelSyncOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.qa.Helper;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class ParallelCallbackTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ParallelCallbackTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		setupServer(p4d.getRSHURL(), superUserName, null, false, null);
	}

	@Before
	public void setUp() throws Exception {
		client = server.getClient(SystemInfo.isWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112");
		Assert.assertNotNull(client);
		server.setCurrentClient(client);
		server.setOrUnsetServerConfigurationValue("net.parallel.max", "10");
		server.setOrUnsetServerConfigurationValue("net.parallel.threads", "3");
	}

	@Test
	public void SyncNonParallel() throws Exception {
		String clientName = "test-nonparallel-sync";
		String clientRoot = p4d.getPathToRoot() + "/" + clientName;
		String[] paths = {"//depot/basic/... //" + clientName + "/..."};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing regular sync", clientRoot, paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient(clientName);
		Assert.assertNotNull(testClientFromServer);

		server.setCurrentClient(testClientFromServer);

		boolean pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(false, pathExists);

		// creates client workspace
		Files.createDirectories(Paths.get(clientRoot));

		// regular sync
		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//depot/basic/...");
		List<IFileSpec> resultSpec = testClientFromServer.sync(fileSpec, syncOptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertTrue(resultSpec.size() > 10);
		new Helper().assertFileSpecError(resultSpec);

		// check a file
		Path path = Paths.get(clientRoot, "readonly/labelsync/misc/p4cmd.tar.gz");
		Assert.assertTrue(Files.exists(path));
	}

	/**
	 * Test parallel sync via IClient implementation.
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void syncInParallel() throws Exception {

		// Creates new client
		String clientRoot = p4d.getPathToRoot() + "/test-client";
		String clientName = "test-client";
		String[] paths = {"//depot/....java //" + clientName + "/....java"};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing parallel sync", clientRoot, paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient(clientName);
		Assert.assertNotNull(testClientFromServer);

		server.setCurrentClient(testClientFromServer);

		boolean pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(false, pathExists);

		// creates client workspace
		Files.createDirectories(Paths.get(clientRoot));

		TestParallelSync parallelSync = new TestParallelSync();
		ParallelSyncOptions poptions = new ParallelSyncOptions(0, 0, 1, 2, 4, parallelSync);
		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//depot/basic/...");
		List<IFileSpec> resultSpec = testClientFromServer.syncParallel(fileSpec, syncOptions, poptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertTrue(resultSpec.size() > 10);

		pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(true, pathExists);
		Assert.assertEquals(true, Files.exists(Paths.get(clientRoot + "/basic")));
		Assert.assertEquals(4, parallelSync.getCount());

		// Errors out with message Files are up to date
		poptions = new ParallelSyncOptions(0, 0, 1, 1, 5, new DefaultParallelSync());
		resultSpec = testClientFromServer.syncParallel(fileSpec, syncOptions, poptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertTrue(resultSpec.get(0).getStatusMessage().contains("file(s) up-to-date."));
		Assert.assertEquals(1, resultSpec.size());

		FileUtils.deleteDirectory(new File(clientRoot));

		// Errors out as no parallel sync options defined
		poptions = new ParallelSyncOptions(new DefaultParallelSync());
		resultSpec = testClientFromServer.syncParallel(fileSpec, syncOptions, poptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertTrue(resultSpec.get(0).getStatusMessage().contains("file(s) up-to-date."));

		// Errors out as no parallel sync options defined, this time via the default constructor
		poptions = new ParallelSyncOptions();
		resultSpec = testClientFromServer.syncParallel(fileSpec, syncOptions, poptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertEquals(FileSpecOpStatus.ERROR, resultSpec.get(0).getOpStatus());
		Assert.assertEquals(1, resultSpec.size());
	}

	/**
	 * Tests sync where a directory exists with a matching name to an existing file in depot
	 *
	 * @throws Exception
	 */
	@Test
	public void syncWithDirectoryMatchingDepotFile() throws Exception {
		String clientName = "test-client-duplicate-dir";
		String clientRoot = p4d.getPathToRoot() + "/" + clientName;

		String[] paths = {"//depot/basic/... //" + clientName + "/..."};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing parallel sync", clientRoot, paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient(clientName);
		Assert.assertNotNull(testClientFromServer);

		server.setCurrentClient(testClientFromServer);

		boolean pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(false, pathExists);

		// creates client workspace
		Files.createDirectories(Paths.get(clientRoot));

		//Create directory with a matching name to a depot file and the it should fail.
		createDirectory(clientRoot, "/readonly/sync/p4cmd/src/gnu/getopt/Getopt.java/foo");

		ParallelSyncOptions poptions = new ParallelSyncOptions(0, 0, 1, 1, 4, null);
		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//depot/basic/...");
		List<IFileSpec> resultSpec = testClientFromServer.syncParallel(fileSpec, syncOptions, poptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertTrue(resultSpec.size() > 10);
		new Helper().assertFileSpecError(resultSpec);
	}

	/**
	 * Tests sync where a file exists with a matching name to an existing file in depot
	 *
	 * @throws Exception
	 */
	@Test
	public void syncFileClobberTest() throws Exception {
		String clientName = "test-client-clobber";
		String clientRoot = p4d.getPathToRoot() + "/" + clientName;
		String[] paths = {"//depot/basic/... //" + clientName + "/..."};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing parallel sync", clientRoot, paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient(clientName);
		Assert.assertNotNull(testClientFromServer);

		server.setCurrentClient(testClientFromServer);

		boolean pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(false, pathExists);

		// creates client workspace
		Files.createDirectories(Paths.get(clientRoot));

		//Create directory and file with a matching name to a depot file and the it should fail.
		createDirectory(clientRoot, "/readonly/sync/p4cmd/src/gnu/getopt/");
		createFile(clientRoot, "/readonly/sync/p4cmd/src/gnu/getopt/Getopt.java");

		ParallelSyncOptions poptions = new ParallelSyncOptions(0, 0, 1, 1, 4, null);
		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//depot/basic/...");
		List<IFileSpec> resultSpec = testClientFromServer.syncParallel(fileSpec, syncOptions, poptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertTrue(resultSpec.size() > 10);
		new Helper().assertFileSpecError(resultSpec);
	}

	/**
	 * Tests parallel sync for streaming depots.
	 *
	 * @throws Exception
	 */
	@Test
	public void syncStreamingInParallel() throws Exception {
		// Creates new client
		String clientRoot = "/var/tmp/streaming-test-client";
		String clientName = "streaming-test-client";
		String[] paths = {"//depot/... //" + clientName + "/..."};

		// Deletes workspace
		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing parallel sync", clientRoot, paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient(clientName);
		Assert.assertNotNull(testClientFromServer);

		server.setCurrentClient(testClientFromServer);

		boolean pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(false, pathExists);

		// creates client workspace
		Files.createDirectories(Paths.get(clientRoot));

		ParallelSyncOptions poptions = new ParallelSyncOptions(0, 0, 1, 1, 5, null);
		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//depot/basic/...");

		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		ParallelStreamingHandler parallelStreamingHandler = new ParallelStreamingHandler(1, resultsList);

		testClientFromServer.syncParallel(fileSpec, syncOptions, parallelStreamingHandler, 1, poptions);

		pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(true, pathExists);
		Assert.assertEquals(true, Files.exists(Paths.get(clientRoot + "/basic")));

		Assert.assertNotNull(parallelStreamingHandler.getResultsList());
		Assert.assertTrue(parallelStreamingHandler.getResultsList().size() > 10);

		FileUtils.deleteDirectory(new File(clientRoot));
	}

	private void createDirectory(String root, String path) {
		File file = new File(root + path);
		Assert.assertTrue(file.mkdirs());
	}

	private void createFile(String root, String path) throws IOException {
		File file = new File(root + path);
		Assert.assertTrue(file.createNewFile());
	}

	public static class ParallelStreamingHandler implements IStreamingCallback {

		int expectedKey = 0;

		List<Map<String, Object>> resultsList = null;

		public ParallelStreamingHandler(int key, List<Map<String, Object>> resultsList) {
			this.expectedKey = key;
			this.resultsList = resultsList;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				fail("key mismatch; expected: " + this.expectedKey
						+ "; observed: " + key);
			}
			if (resultMap == null) {
				fail("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);
			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	}

	/**
	 * Test parallel submit via raw execMap.
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void submitInParallel() throws Exception {

		// Creates new client
		String clientRoot = "/var/tmp/test-client";
		String clientName = "test-client";
		String[] paths = {"//depot/... //" + clientName + "/..."};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing parallel sync", clientRoot, paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient(clientName);
		Assert.assertNotNull(testClientFromServer);

		server.setCurrentClient(testClientFromServer);

		boolean pathExists = Files.exists(Paths.get(clientRoot));
		Assert.assertEquals(false, pathExists);

		// creates client workspace
		List<IFileSpec> toAdd = new ArrayList<IFileSpec>();
		Files.createDirectories(Paths.get(clientRoot, "test-psubmit"));
		for (int i = 0; i < 10; i++) {
			File tmp = Paths.get(clientRoot, "test-psubmit", "file" + i).toFile();
			FileWriter writer = new FileWriter(tmp);
			String data = "test" + i;
			writer.write(data, 0, data.length());
			writer.close();
			toAdd.addAll(FileSpecBuilder.makeFileSpecList("//" + clientName + "/test-psubmit/file" + i));
		}

		IChangelist change = new Changelist();
		change.setDescription("test change");
		change = testClient.createChangelist(change);
		List<IFileSpec> res0 = testClient.addFiles(toAdd, new AddFilesOptions().setChangelistId(change.getId()));

		List<Map<String, Object>> res1 = server.execMapCmdList("submit", new String[]{"-c", "" + change.getId(), "--parallel=threads=4"}, null, new DefaultParallelSync());

		for (Map<String, Object> item : res1) {
			if (item.containsKey("code0")) {
				String output = RpcMessage.interpolateArgs((String) item.get("fmt0"), item);
				fail(output);
			}
		}
	}

	private class TestParallelSync extends DefaultParallelSync {

		private int threadCount = 0;

		@Override
		public boolean transmit(CommandEnv cmdEnv, int threads, HashMap<String, String> flags, ArrayList<String> args) {
			threadCount = threads;
			return super.transmit(cmdEnv, threads, flags, args);
		}

		public int getCount() {
			return threadCount;
		}
	}
}
