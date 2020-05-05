package com.perforce.p4java.tests.dev.unit.bug.r181;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class Job096364Test extends P4JavaRshTestCase {

	IClient client = null;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r18.1", Job096364Test.class.getSimpleName());

	private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), superUserName, superUserPassword, false, null);
			server.setOrUnsetServerConfigurationValue("filesys.utf8bom", "0");

			server.setUserName(userName);
			server.login(password);
			client = createClient(server, "job096364TestClient");
			File clientRoot = new File(client.getRoot());
			File sourceDir = new File(client.getRoot() + "/src");
			File targetDir = new File(client.getRoot() + "/trg");
			clientRoot.mkdir();
			sourceDir.mkdir();
			targetDir.mkdir();
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		attemptCleanupFiles(client);
		if (server != null) {
			this.endServerSession(server);
		}
	}

	@Test
	@Ignore("Fails due to job096364")
	public void testJob096364() throws Exception {
		String sourceFile = client.getRoot() + File.separator + "src/source.txt";
		String targetFile = client.getRoot() + File.separator + "trg/target.txt";
		IChangelist changelist;

		// Submitting source test file to server
		File from = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_8-jp_with_bom.txt");
		File testFile = new File(sourceFile);
		Files.copy(from.toPath(), testFile.toPath());
		IChangelist change = createNewChangelist(client, "test");
		AddFileToChangelist(testFile, change, client);
		submitChangelist(change);

		// Create changelist for 'integrate'
		changelist = getNewChangelist(server, client,
				"Dev112_ResolveBranchingTest 'integrate -Rb' changelist");
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);

		// Run integrate
		List<IFileSpec> integrateFiles = client.integrateFiles(
				new FileSpec(sourceFile),
				new FileSpec(targetFile),
				null,
				new IntegrateFilesOptions().setChangelistId(changelist.getId()));

		// Check for null
		assertNotNull(integrateFiles);

		// Refresh changelist
		changelist.refresh();

		// Submit changelist
		submitChangelist(changelist);

		// Edit source file
		editFile(server, client, "test", sourceFile);

		// Create changelist for 'edit'
		changelist = getNewChangelist(server, client,
				"Dev112_ResolveBranchingTest 'edit' changelist");
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);

		// Submit changelist
		submitChangelist(changelist);

		// Create changelist for 'integrate'
		changelist = getNewChangelist(server, client,
				"Dev112_ResolveBranchingTest 'integrate' changelist");
		assertNotNull(changelist);
		changelist = client.createChangelist(changelist);

		// Run integrate
		integrateFiles = client.integrateFiles(
				new FileSpec(sourceFile),
				new FileSpec(targetFile),
				null,
				new IntegrateFilesOptions().setChangelistId(changelist.getId()));

		// Check for null
		assertNotNull(integrateFiles);

		// Finally, try resolving all the files
		List<IFileSpec> resolveFiles = client.resolveFilesAuto(null,
				new ResolveFilesAutoOptions().setChangelistId(changelist.getId()).setSafeMerge(true));
		assertNotNull(resolveFiles);
		changelist.refresh();

		// Check for absence of bom in the target file, it should'nt exist
//		byte[] encoded = Files.readAllBytes(Paths.get(targetFile));
//		String content = new String(encoded, StandardCharsets.UTF_8);
//		assertFalse(content.contains("\uFEFF"));

		submitChangelist(changelist);
	}

}
