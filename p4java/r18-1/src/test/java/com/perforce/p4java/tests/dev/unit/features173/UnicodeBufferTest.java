package com.perforce.p4java.tests.dev.unit.features173;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.perforce.p4java.server.CmdSpec.RETYPE;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UnicodeBufferTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";
	private static final String clientName = "utf8.buffer-client";

	private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", UnicodeBufferTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		setupServer(p4d.getRSHURL(), userName, userName, false, properties);

		// initialization code (before each test).
		try {
			// Creates new client
			String clientRoot = p4d.getPathToRoot() + FILE_SEP + "client";
			String[] paths = {"//depot/r173/... //" + clientName + "/..."};
			IClient testClient = Client.newClient(server, clientName, "UTF8 to UTF16 buffer test", clientRoot, paths);
			server.createClient(testClient);
			IClient client = server.getClient(clientName);
			assertNotNull(client);
			server.setCurrentClient(client);

			// Clean up directory
			FileUtils.deleteDirectory(new File(clientRoot));
			FileUtils.forceMkdir(new File(clientRoot));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testBuffersUTF8() throws Exception {

		// Add Source file to depot
		client = server.getCurrentClient();
		IChangelist change = new Changelist();
		change.setDescription("Add UTF8 Unicode file");
		change = client.createChangelist(change);

		// ... copy from resource to workspace
		File utf8 = loadFileFromClassPath(CLASS_PATH_PREFIX + "/ko_utf8.xml");
		File utf16 = loadFileFromClassPath(CLASS_PATH_PREFIX + "/ko_utf16.xml");
		File testFile = new File(client.getRoot() + FILE_SEP + "ko_utf8.xml");
		Files.copy(utf8.toPath(), testFile.toPath());

		// ... add to pending change
		List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(testFile.getAbsolutePath());
		AddFilesOptions addOpts = new AddFilesOptions();
		addOpts.setChangelistId(change.getId());
		List<IFileSpec> msg = client.addFiles(fileSpecs, addOpts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals("utf8", msg.get(0).getFileType());

		// ... submit file and validate
		msg = change.submit(false);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals(FileAction.ADD, msg.get(0).getAction());

		// Clean up directory
		String clientRoot = client.getRoot();
		FileUtils.deleteDirectory(new File(clientRoot));
		FileUtils.forceMkdir(new File(clientRoot));

		// Force sync client
		SyncOptions opts = new SyncOptions();
		opts.setForceUpdate(true);
		msg = client.sync(null, opts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals("//depot/r173/ko_utf8.xml", msg.get(0).getDepotPathString());

		// Verify content
		List<String> original = fileToLines(utf8.getAbsolutePath());
		List<String> revised = fileToLines(msg.get(0).getClientPathString());
		Patch patch = DiffUtils.diff(original, revised);
		List deltas = patch.getDeltas();
		assertTrue(deltas.isEmpty());

		Map<String, Object>[] res = server.execMapCmd(RETYPE.name(), new String[]{"-tutf16", msg.get(0).getDepotPathString()}, null);
		assertNotNull(res);

		// Clean up directory
		FileUtils.deleteDirectory(new File(clientRoot));
		FileUtils.forceMkdir(new File(clientRoot));

		// Force sync client
		msg = client.sync(null, opts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals("//depot/r173/ko_utf8.xml", msg.get(0).getDepotPathString());

		// Verify content
		original = fileToLines(utf16.getAbsolutePath());
		revised = fileToLines(msg.get(0).getClientPathString());
		patch = DiffUtils.diff(original, revised);
		deltas = patch.getDeltas();
		assertTrue(deltas.isEmpty());
	}

	@Test
	public void testUTF16File() throws Exception {

		// Add Source file to depot
		client = server.getCurrentClient();
		IChangelist change = new Changelist();
		change.setDescription("Add UTF16 Unicode file");
		change = client.createChangelist(change);

		// ... copy from resource to workspace
		File utf16 = loadFileFromClassPath(CLASS_PATH_PREFIX + "/ko_utf16.xml");
		File testFile = new File(client.getRoot() + FILE_SEP + "ko_utf16.xml");
		Files.copy(utf16.toPath(), testFile.toPath());

		// ... add to pending change
		List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(testFile.getAbsolutePath());
		AddFilesOptions addOpts = new AddFilesOptions();
		addOpts.setChangelistId(change.getId());
		List<IFileSpec> msg = client.addFiles(fileSpecs, addOpts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals("utf16", msg.get(0).getFileType());

		// ... submit file and validate
		msg = change.submit(false);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals(FileAction.ADD, msg.get(0).getAction());

		// Clean up directory
		String clientRoot = client.getRoot();
		FileUtils.deleteDirectory(new File(clientRoot));
		FileUtils.forceMkdir(new File(clientRoot));

		// Force sync client
		SyncOptions opts = new SyncOptions();
		opts.setForceUpdate(true);
		msg = client.sync(null, opts);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals("//depot/r173/ko_utf16.xml", msg.get(0).getDepotPathString());

		// Verify content
		List<String> original = fileToLines(utf16.getAbsolutePath());
		List<String> revised = fileToLines(msg.get(0).getClientPathString());
		Patch patch = DiffUtils.diff(original, revised);
		List deltas = patch.getDeltas();
		assertTrue(deltas.isEmpty());
	}

	private List<String> fileToLines(String filename) throws IOException {
		List<String> lines = new LinkedList<>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = "";
		while ((line = in.readLine()) != null) {
			lines.add(line);
		}
		in.close();
		return lines;
	}
}
