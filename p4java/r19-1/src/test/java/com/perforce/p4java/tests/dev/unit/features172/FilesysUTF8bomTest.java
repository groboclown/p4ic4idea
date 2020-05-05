package com.perforce.p4java.tests.dev.unit.features172;

import com.perforce.p4java.PropertyDefs;
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
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class FilesysUTF8bomTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";
	private static final String clientName = "filesysUtf8bom-client";

	private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";

	@Rule
	public SimpleServerRule p4d = new SimpleServerRule("r16.1", FilesysUTF8bomTest.class.getSimpleName());

	private void before(String bom) throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.FILESYS_UTF8BOM_SHORT_FORM, bom);
		setupServer(p4d.getRSHURL(), userName, userName, false, properties);

		// initialization code (before each test).
		try {
			// Creates new client
			String clientRoot = p4d.getPathToRoot() + "/client";
			String[] paths = {"//depot/r171/... //" + clientName + "/..."};
			IClient testClient = Client.newClient(server, clientName, "filesys.utf8bom test", clientRoot, paths);
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

	/**
	 * Non-Unicode enabled server:
	 *
	 * For files with type 'uft8'
	 *
	 *   FILESYS_UTF8BOM_SHORT_FORM = 1 (BOM added - all platforms)
	 *
	 * @throws Exception
	 */
	@Test
	public void testFilesysUTF8bom_Windows() throws Exception {

		before("1");

		// Add Source file to depot
		client = server.getCurrentClient();
		IChangelist change = new Changelist();
		change.setDescription("Add foo.txt");
		change = client.createChangelist(change);

		// ... copy from resource to workspace
		File from = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
		File testFile = new File(client.getRoot() + File.separator + "utf8-bom.txt");
		Files.copy( from.toPath(), testFile.toPath() );

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
		assertEquals("//depot/r171/utf8-bom.txt", msg.get(0).getDepotPathString());

		// Verify content
		byte[] encoded = Files.readAllBytes(testFile.toPath());
		String content = new String(encoded, StandardCharsets.UTF_8);

		assertEquals("\uFEFFa\r\nb\r\nc", content);
	}

	/**
	 * Non-Unicode enabled server:
	 *
	 * For files with type 'uft8'
	 *
	 *   filesys.utf8bom = 0 (no BOM - all platforms)
	 *
	 * @throws Exception
	 */
	@Test
	public void testFilesysUTF8nobom_Windows() throws Exception {

		before("0");

		// Add Source file to depot
		client = server.getCurrentClient();
		IChangelist change = new Changelist();
		change.setDescription("Add foo.txt");
		change = client.createChangelist(change);

		// ... copy from resource to workspace
		File from = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
		File testFile = new File(client.getRoot() + File.separator + "utf8-nobom.txt");
		Files.copy( from.toPath(), testFile.toPath() );

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
		assertEquals("//depot/r171/utf8-nobom.txt", msg.get(0).getDepotPathString());

		// Verify content
		byte[] encoded = Files.readAllBytes(testFile.toPath());
		String content = new String(encoded, StandardCharsets.UTF_8);

		assertEquals("a\r\nb\r\nc", content);
	}
}
