package com.perforce.p4java.tests.dev.unit.bug.r191;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Job094474Test extends P4JavaRshTestCase {

	public static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
	private static IClient client = null;
	private static final String clientName = "Job094474TestClient";
	private static final String depotTestPath = "//depot/src/...";

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r19.1", Job094474Test.class.getSimpleName());

	@BeforeClass
	public static void setUp() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, null);
		assertNotNull(server);
		client = createClient(server, clientName);
		assertNotNull(client);
		File sourceDir = new File(client.getRoot() + "/src");
		sourceDir.mkdirs();
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
	public void testReconcileUnChangedUtf8File() throws Exception {
		String sourceFile = client.getRoot() + File.separator + "src/utfsource.txt";
		File from = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_8-jp_without_bom.txt");

		File testFile = new File(sourceFile);
		Files.copy(from.toPath(), testFile.toPath());

		IChangelist change = createNewChangelist(client, "Created a utf_8 file and added to perforce");
		List<IFileSpec> files = client.addFiles(FileSpecBuilder.makeFileSpecList(depotTestPath),
				new AddFilesOptions().setChangelistId(change.getId()));

		assertNotNull(files);
		assertEquals("utf8", files.get(0).getFileType());
		submitChangelist(change);

		client.sync(FileSpecBuilder.makeFileSpecList(depotTestPath + "#0"), new SyncOptions().setForceUpdate(true));
		client.sync(FileSpecBuilder.makeFileSpecList(depotTestPath), new SyncOptions().setForceUpdate(true));

		// Run reconcile without changing the file.
		List<IFileSpec> reconcileFiles = client.reconcileFiles(
				FileSpecBuilder.makeFileSpecList(depotTestPath),
				new ReconcileFilesOptions().setUseWildcards(true).setOutsideAdd(true).setOutsideEdit(true).setRemoved(true));

		// Verify that the reconcileFiles is not empty and there are no files to reconcile.
		assertNotNull(reconcileFiles);
		assertEquals(FileSpecOpStatus.ERROR, reconcileFiles.get(0).getOpStatus());
		assertEquals("//depot/src/... - no file(s) to reconcile.", reconcileFiles.get(0).getStatusMessage());

	}
}
