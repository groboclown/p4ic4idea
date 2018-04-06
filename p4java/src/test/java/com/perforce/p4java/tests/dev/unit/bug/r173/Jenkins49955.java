package com.perforce.p4java.tests.dev.unit.bug.r173;

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
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

import static com.perforce.p4java.tests.ServerMessageMatcher.containsText;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Jenkins49955 extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";
	private static final String clientName = "UTF8BOM-digest-client";

	private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Jenkins49955.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		setupServer(p4d.getRSHURL(), userName, userName, false, properties);

		// initialization code (before each test).
		try {
			// Creates new client
			String clientRoot = p4d.getPathToRoot() + "/client";
			String[] paths = {"//depot/md5/... //" + clientName + "/..."};
			IClient testClient = Client.newClient(server, clientName, "Digest", clientRoot, paths);
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
	public void testNoEditOnReconcile() throws Exception {
		// Add Source file to depot
		client = server.getCurrentClient();
		IChangelist change = new Changelist();
		change.setDescription("Add UTF8BOM files and reconcile for no changes.");
		change = client.createChangelist(change);

		// ... copy from resource to workspace
		File from = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
		File testFile = new File(client.getRoot() + FILE_SEP + "utf8.txt");
		Files.copy(from.toPath(), testFile.toPath());

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

		// ... reconcile workspace (should find no change)
		List<IFileSpec> clientSpecs = FileSpecBuilder.makeFileSpecList(client.getRoot() + "/...");
		ReconcileFilesOptions recOpts = new ReconcileFilesOptions();
		List<IFileSpec> rec = client.reconcileFiles(clientSpecs, recOpts);
		assertNotNull(rec);
		assertThat(rec.get(0).getStatusMessage(), containsText(" - no file(s) to reconcile."));
	}
}
