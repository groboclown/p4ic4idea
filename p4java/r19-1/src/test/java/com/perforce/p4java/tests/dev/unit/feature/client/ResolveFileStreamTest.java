package com.perforce.p4java.tests.dev.unit.feature.client;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests text and binary resolves using the stream-based IClient resolveFile method.
 * Adapted from Job040601Test.
 */

@TestId("Client_ResolveFileStreamTest")
public class ResolveFileStreamTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ResolveFileStreamTest.class.getSimpleName());

	private static final String TEST_ROOT = "//depot/client/ResolveFileStreamTest";
	private static IClient client;
	private IChangelist changelist = null;

	@BeforeClass
	public static void beforeEach() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, null);
		client = createClient(server, "resolveFileStreamTest");
	}

	@Test
	public void testBinaryStreamResolve() throws Exception {
		String integSource = TEST_ROOT + "/" + "test03.jpg";
		String integTarget = TEST_ROOT + "/" + "test04.jpg";

		// Populate workspace
		forceSyncFiles(client, TEST_ROOT + "/...");

		// Create numbered pending change
		changelist = client.createChangelist(new Changelist(
				IChangelist.UNKNOWN,
				client.getName(),
				getUserName(),
				ChangelistStatus.NEW,
				null,
				"Bugs101_Job040601Test test submit changelist",
				false,
				(Server) server
		));
		assertNotNull(changelist);

		// Open integTarget for edit
		List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(integTarget),
				new EditFilesOptions().setChangelistId(changelist.getId()));
		assertNotNull(editList);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());

		// Submit change
		changelist.refresh();
		List<IFileSpec> submitList2 = changelist.submit(false);
		assertNotNull(submitList2);
		validateFileSpecs(submitList2);


		// Use 'default' change for integrate and resolve steps, as resolveFile does not support a numbered change
		changelist = server.getChangelist(IChangelist.DEFAULT);

		// Integrate integSource -> integTarget
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions().setChangelistId(Changelist.DEFAULT);
		List<IFileSpec> integFiles = client.integrateFiles(new FileSpec(integSource), new FileSpec(integTarget), null, integOpts);
		assertNotNull(integFiles);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(integFiles).size());

		// Resolve target using source as the stream.
		String editSourcePath = this.getSystemPath(client, integSource);
		assertNotNull(editSourcePath);
		File editFile = new File(editSourcePath);
		assertTrue(editFile.canRead());
		try (FileInputStream editStream = new FileInputStream(editFile)) {
			IFileSpec resolvedFile = client.resolveFile(new FileSpec(integTarget), editStream);
			assertNotNull(resolvedFile);
			assertEquals("edit from", resolvedFile.getHowResolved());
		}

		// Open integTarget for edit
		List<IFileSpec> tamperList = client.editFiles(FileSpecBuilder.makeFileSpecList(integTarget),
				new EditFilesOptions().setChangelistId(changelist.getId()));
		assertNotNull(tamperList);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(tamperList).size());

		changelist.setDescription("Resolve with stream:");
		List<IFileSpec> subList = changelist.submit(null);
		assertNotNull(subList);
		validateFileSpecs(subList);
	}

	@Test
	public void testTextStreamResolve() throws Exception {
		String test01Name = TEST_ROOT + "/" + "test01.txt";
		String test02Name = TEST_ROOT + "/" + "test02.txt";

		forceSyncFiles(client, TEST_ROOT + "/...");
		changelist = client.createChangelist(new Changelist(
				IChangelist.UNKNOWN,
				client.getName(),
				getUserName(),
				ChangelistStatus.NEW,
				null,
				"Bugs101_Job040601Test test submit changelist",
				false,
				(Server) server
		));
		assertNotNull(changelist);

		// Edit
		List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(test01Name),
				new EditFilesOptions().setChangelistId(changelist.getId()));
		assertNotNull(editList);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
		changelist.refresh();

		// Submit
		List<IFileSpec> submitList = changelist.submit(new SubmitOptions());
		assertNotNull(submitList);

		changelist = server.getChangelist(IChangelist.DEFAULT);

		List<IFileSpec> integFiles = client.integrateFiles(
				new FileSpec(test01Name),
				new FileSpec(test02Name),
				null,
				new IntegrateFilesOptions().setChangelistId(IChangelist.DEFAULT));
		assertNotNull(integFiles);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(integFiles).size());

		String sourcePath = this.getSystemPath(client, test01Name);
		assertNotNull(sourcePath);
		File sourceFile = new File(sourcePath);
		assertTrue(sourceFile.canRead());
		try (FileInputStream sourceStream = new FileInputStream(sourceFile)) {
			IFileSpec resolvedFile = client.resolveFile(new FileSpec(test02Name), sourceStream);
			assertNotNull(resolvedFile);
			assertEquals("edit from", resolvedFile.getHowResolved());
		}
		changelist.setDescription("test");
		List<IFileSpec> subList = changelist.submit(false);
		assertNotNull(subList);
		validateFileSpecs(subList);
	}

}


