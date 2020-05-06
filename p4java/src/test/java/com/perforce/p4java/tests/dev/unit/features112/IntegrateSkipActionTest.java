/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the integrate files with schedules 'branch resolves' instead of
 * branching new target files automatically.
 */
@Jobs({"job046102"})
@TestId("Dev112_IntegrateSkipActionTest")
public class IntegrateSkipActionTest extends P4JavaRshTestCase {

	private static Logger logger = LoggerFactory.getLogger(IntegrateSkipActionTest.class);

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", IntegrateSkipActionTest.class.getSimpleName());

	private static IClient client;

	// The source file has 6 revisions (#6)
	private static final String sourceFile = "//depot/source/file.txt";

	// The target file has revision #1 as original branch, and integrated a cherry-picked revision #4,#4
	private static final String targetFile = "//depot/target/file.txt";

	@Before
	public void before() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, null);

		String clientName = "resolveFileStreamTest";
		String clientRoot = p4d.getPathToRoot();
		String[] clientViews = {"//depot/... //" + clientName + "/..."};
		client = createClient(server, clientName, "test", clientRoot, clientViews);

		addFile(server, client, "Add1", sourceFile);
		editFile(server, client, "Edit2", sourceFile);
		editFile(server, client, "Edit3", sourceFile);
		editFile(server, client, "Edit4", sourceFile);
		editFile(server, client, "Edit5", sourceFile);
		editFile(server, client, "Edit6", sourceFile);
	}

	@Test
	public void testIntegrateFilesWithSkipIntegrated() throws Exception {

		// Create the integrate changelist
		IChangelist changelist1 = getNewChangelist(server, client, "Dev112_IntegrateSkipActionTest schedule resolve changelist");
		assertNotNull(changelist1);
		changelist1 = client.createChangelist(changelist1);

		// Integrate revision #1 of a file to the destination
		IFileSpec sourceFileSpec1 = new FileSpec(sourceFile);
		sourceFileSpec1.setStartRevision(1);
		sourceFileSpec1.setEndRevision(1);

		IFileSpec targetFileSpec1 = new FileSpec(targetFile);

		IntegrateFilesOptions integOpts1 = new IntegrateFilesOptions();
		integOpts1.setChangelistId(changelist1.getId());
		integOpts1.setForceIntegration(true);

		List<IFileSpec> integrateFiles1 = client.integrateFiles(sourceFileSpec1, targetFileSpec1, null, integOpts1);
		assertNotNull(integrateFiles1);
		validateFileSpecs(integrateFiles1);

		// Submit the file in the integrate changelist
		changelist1.refresh();
		List<IFileSpec> submitIntegrationList1 = changelist1.submit(null);
		assertNotNull(submitIntegrationList1);
		validateFileSpecs(submitIntegrationList1);

		// Create the integrate changelist
		IChangelist changelist2 = getNewChangelist(server, client, "Dev112_IntegrateSkipActionTest schedule resolve changelist");
		assertNotNull(changelist2);
		changelist2 = client.createChangelist(changelist2);

		// Integrate cherry-picked revision #4,#4 of a file to the destination
		IFileSpec sourceFileSpec2 = new FileSpec(sourceFile);
		sourceFileSpec2.setStartRevision(4);
		sourceFileSpec2.setEndRevision(4);

		IFileSpec targetFileSpec2 = new FileSpec(targetFile);
		IntegrateFilesOptions integOpts2 = new IntegrateFilesOptions();
		integOpts2.setChangelistId(changelist2.getId());
		integOpts2.setForceIntegration(true);

		List<IFileSpec> integrateFiles2 = client.integrateFiles(sourceFileSpec2, targetFileSpec2, null, integOpts2);
		assertNotNull(integrateFiles2);
		validateFileSpecs(integrateFiles2);

		// Resolve files
		ResolveFilesAutoOptions resOpts = new ResolveFilesAutoOptions();
		resOpts.setChangelistId(changelist2.getId());
		resOpts.setForceResolve(true);
		resOpts.setAcceptTheirs(true);
		List<IFileSpec> resolveFiles = client.resolveFilesAuto(null, resOpts);
		assertNotNull(resolveFiles);
		validateFileSpecs(resolveFiles);

		// Submit the file in the integrate changelist
		changelist2.refresh();
		List<IFileSpec> submitIntegrationList2 = changelist2.submit(null);
		assertNotNull(submitIntegrationList2);
		validateFileSpecs(submitIntegrationList2);

		// Create the integrate changelist
		IChangelist changelist3 = getNewChangelist(server, client, "Dev112_IntegrateSkipActionTest schedule resolve changelist");
		assertNotNull(changelist3);
		changelist3 = client.createChangelist(changelist3);

		// Run integrate with 'skip cherry-picked revisions already integrated'
		IFileSpec sourceFileSpec3 = new FileSpec(sourceFile);
		IFileSpec targetFileSpec3 = new FileSpec(targetFile);
		IntegrateFilesOptions integOpts3 = new IntegrateFilesOptions();
		integOpts3.setChangelistId(changelist3.getId());
		integOpts3.setSkipIntegratedRevs(true);
		List<IFileSpec> integrateFiles3 = client.integrateFiles(sourceFileSpec3, targetFileSpec3, null, integOpts3);
		assertNotNull(integrateFiles3);
		validateFileSpecs(integrateFiles3);

		// Check for invalid filespecs
		List<IFileSpec> invalidFiles3 = FileSpecBuilder.getInvalidFileSpecs(integrateFiles3);
		if (invalidFiles3.size() != 0) {
			fail(invalidFiles3.get(0).getOpStatus() + ": " + invalidFiles3.get(0).getStatusMessage());
		}

		// Check for the correct number of filespecs with 'valid' op status
		changelist3.refresh();
		List<IFileSpec> openedFiles3 = changelist3.getFiles(true);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(openedFiles3).size());

		// Validate the filespecs action types: integrate
		assertEquals(FileAction.INTEGRATE, openedFiles3.get(0).getAction());

		// Validate the filespecs integrate revisions:
		// The source file has 6 revisions (#6)
		// The target file has #1 as original branch, and #4,#4
		// cherry-picked integrate

		// There should be two filespecs in integrate files
		assertEquals(2, integrateFiles3.size());

		// Check the first integrate info
		assertEquals(FileSpecOpStatus.VALID, integrateFiles3.get(0).getOpStatus());
		assertTrue(integrateFiles3.get(0).getFromFile().contentEquals(sourceFile));

		// The second integrate: startFromRev 1 and endFromRev 3
		assertEquals(1, integrateFiles3.get(0).getStartFromRev());
		assertEquals(3, integrateFiles3.get(0).getEndFromRev());

		// Check the second integrate info
		assertEquals(FileSpecOpStatus.VALID, integrateFiles3.get(0).getOpStatus());
		assertTrue(integrateFiles3.get(1).getFromFile().contentEquals(sourceFile));

		// The first file integrate: startFromRev 4 and endFromRev 6
		// Note: revision #4 is skipped as expected
		assertEquals(4, integrateFiles3.get(1).getStartFromRev());
		assertEquals(6, integrateFiles3.get(1).getEndFromRev());

		// Validate using the 'resolve' command
		// Check the "how" it is resolved
		ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
		resolveFilesAutoOptions.setChangelistId(changelist3.getId());

		List<IFileSpec> resolveFiles2 = client.resolveFilesAuto(null, resolveFilesAutoOptions);
		assertNotNull(resolveFiles2);
		assertTrue(resolveFiles2.size() > 0);
		IFileSpec lastFileSpec = resolveFiles2.get(resolveFiles2.size() - 1);
		assertNotNull(lastFileSpec);

		assertTrue(lastFileSpec.getOpStatus() == FileSpecOpStatus.VALID);
		assertTrue(lastFileSpec.getHowResolved().contentEquals("merge from"));
	}
}

