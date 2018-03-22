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
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IntegrationOutputTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";
	private static final String clientName = "integ-client";
	private static final String srcFile = "//depot/r171/foo.txt";
	private static final String midFile = "//depot/r171/bar.txt";
	private static final String trgFile = "//depot/r171/baz.txt";
	private static final String oldFile = "//depot/r171/old.txt";

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", IntegrationOutputTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), userName, userName, true, properties);

		// initialization code (before each test).
		try {
			// Creates new client
			String clientRoot = p4d.getPathToRoot() + "/client";
			String[] paths = {"//depot/r171/... //" + clientName + "/..."};
			IClient testClient = Client.newClient(server, clientName, "Integ -Obr options test", clientRoot, paths);
			server.createClient(testClient);
			IClient client = server.getClient(clientName);
			assertNotNull(client);
			server.setCurrentClient(client);

			// Clean up directory
			FileUtils.deleteDirectory(new File(clientRoot));
			FileUtils.forceMkdir(new File(clientRoot));

			// Add Source file
			IChangelist change = new Changelist();
			change.setDescription("Add foo.txt");
			change = client.createChangelist(change);

			File testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
			try (FileWriter writer = new FileWriter(testFile)) {
				writer.write("ADD");
				writer.close();
			}

			List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(testFile.getAbsolutePath());
			AddFilesOptions addOpts = new AddFilesOptions();
			addOpts.setChangelistId(change.getId());
			client.addFiles(fileSpecs, addOpts);
			List<IFileSpec> msg = change.submit(false);
			assertNotNull(msg);
			assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
			assertEquals(FileAction.ADD, msg.get(0).getAction());

			// branch file
			branchFile(srcFile, midFile);
			branchFile(srcFile, trgFile);

			// Edit Source and file type
			change = new Changelist();
			change.setDescription("Edit foo.txt");
			change = client.createChangelist(change);

			EditFilesOptions editOpts = new EditFilesOptions();
			editOpts.setChangelistId(change.getId());
			editOpts.setFileType("+w");
			client.editFiles(fileSpecs, editOpts);
			try (FileWriter writer = new FileWriter(testFile)) {
				writer.write("EDIT");
				writer.close();
			}
			msg = change.submit(false);
			assertNotNull(msg);
			assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
			assertEquals(FileAction.EDIT, msg.get(0).getAction());

			// copy file
			mergeFile(srcFile, midFile);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	private static void branchFile(String srcFile, String trgFile) throws P4JavaException {
		IChangelist change = new Changelist();
		change.setDescription("Branch " + srcFile + " to " + trgFile);
		IClient client = server.getCurrentClient();
		change = client.createChangelist(change);

		IFileSpec sourceSpec = new FileSpec(srcFile);
		IFileSpec targetSpec = new FileSpec(trgFile);
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setDisplayBaseDetails(true);
		integOpts.setChangelistId(change.getId());

		List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
		assertNotNull(integedFiles);
		assertEquals(1, integedFiles.size());
		assertEquals(trgFile, integedFiles.get(0).getDepotPathString());

		List<IFileSpec> msg = change.submit(false);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals(FileAction.BRANCH, msg.get(0).getAction());
	}

	private static void mergeFile(String srcFile, String trgFile) throws P4JavaException {
		IChangelist change = new Changelist();
		change.setDescription("Merge " + srcFile + " to " + trgFile);
		IClient client = server.getCurrentClient();
		change = client.createChangelist(change);

		IFileSpec sourceSpec = new FileSpec(srcFile);
		IFileSpec targetSpec = new FileSpec(trgFile);
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setDisplayBaseDetails(true);
		integOpts.setChangelistId(change.getId());

		List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
		assertNotNull(integedFiles);
		assertEquals(1, integedFiles.size());
		assertEquals(trgFile, integedFiles.get(0).getDepotPathString());

		ResolveFilesAutoOptions resolveOpts = new ResolveFilesAutoOptions();
		resolveOpts.setSafeMerge(true);
		List<IFileSpec> resolvedFiles = client.resolveFilesAuto(integedFiles, resolveOpts);
		assertNotNull(resolvedFiles);
		assertTrue(resolvedFiles.size() > 1);
		assertEquals(srcFile, resolvedFiles.get(0).getFromFile());

		List<IFileSpec> msg = change.submit(false);
		assertNotNull(msg);
		assertEquals(FileSpecOpStatus.VALID, msg.get(0).getOpStatus());
		assertEquals(FileAction.INTEGRATE, msg.get(0).getAction());
	}

	// make sure the base rev information is available
	@Test
	public void baseRevOptions() throws Throwable {
		IFileSpec sourceSpec = new FileSpec(midFile);
		IFileSpec targetSpec = new FileSpec(trgFile);
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setShowBaseRevision(true);
		integOpts.setShowScheduledResolve(true);

		IClient client = server.getCurrentClient();
		List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
		assertNotNull(integedFiles);
		assertEquals(1, integedFiles.size());

		assertEquals(FileSpecOpStatus.VALID, integedFiles.get(0).getOpStatus());
		assertEquals("Wrong base", midFile, integedFiles.get(0).getBaseName());
		assertEquals("Wrong rev", 1, integedFiles.get(0).getBaseRev());

		assertNotNull( integedFiles.get(0).getResolveTypes());
		assertEquals( 2, integedFiles.get(0).getResolveTypes().size());
		assertTrue( integedFiles.get(0).getResolveTypes().contains("content"));
		assertTrue( integedFiles.get(0).getResolveTypes().contains("filetype"));
	}

	// make sure the base rev information is available
	@Test
	public void integ2() throws Throwable {
		IFileSpec sourceSpec = new FileSpec(midFile);
		IFileSpec targetSpec = new FileSpec(oldFile);
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setInteg2(true);

		IClient client = server.getCurrentClient();
		List<IFileSpec> integedFiles = client.integrateFiles(sourceSpec, targetSpec, null, integOpts);
		assertNotNull(integedFiles);
		assertEquals(1, integedFiles.size());

		assertEquals(FileSpecOpStatus.VALID, integedFiles.get(0).getOpStatus());
	}
}

