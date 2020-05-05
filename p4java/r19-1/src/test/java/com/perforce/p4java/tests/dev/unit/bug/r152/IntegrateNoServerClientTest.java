/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test integrate files with no server client (or invalid).
 */
@Jobs({"job062825"})
@TestId("Dev152_IntegrateNoServerClientTest")
public class IntegrateNoServerClientTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", IntegrateNoServerClientTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		setupServer(p4d.getRSHURL(), null, null, true, null);
	}

	/**
	 * Test integrate files with no server client (or invalid).
	 */
	@Test
	public void testIntegrateNoServerClientTest() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";

		String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/Delete_MessagesBundle_es.properties";

		IChangelist changelist = null;

		try {
			IClient client = server.getClient(defaultTestClientName);
			server.setCurrentClient(client);
			// Create the copy changelist
			changelist = getNewChangelist(server, client,
					"Dev152_IntegrateNoServerClientTest copy changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Make a new copy of a file
			List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
							sourceFile), new FileSpec(targetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(copyFiles);

			// Submit the file in the copy changelist
			changelist.refresh();
			List<IFileSpec> submitCopyList = changelist.submit(null);
			assertNotNull(submitCopyList);

			// Create the integrate changelist
			changelist = getNewChangelist(server, client,
					"Dev152_IntegrateNoServerClientTest integration changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Remove the client from the server, or set some non-existent client
			server.setCurrentClient(null);

			// Integrate the new copy file to the destination
			List<IFileSpec> integrateFiles = client.integrateFiles(
					new FileSpec(targetFile),
					new FileSpec(targetFile2),
					null,
					new IntegrateFilesOptions().setChangelistId(
							changelist.getId()).setForceIntegration(true));
			assertNotNull(integrateFiles);

			// Check for errors
			List<IFileSpec> invalidFiles = FileSpecBuilder
					.getInvalidFileSpecs(integrateFiles);
			if (invalidFiles.size() != 0) {
				fail(invalidFiles.get(0).getOpStatus() + ": "
						+ invalidFiles.get(0).getStatusMessage());
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (client != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							client.revertFiles(
									changelist.getFiles(true),
									new RevertFilesOptions()
											.setChangelistId(changelist.getId()));
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
			if (client != null && server != null) {
				try {
					// Delete the newly integrated copied file
					IChangelist deleteChangelist = getNewChangelist(server,
							client,
							"Dev152_IntegrateNoServerClientTest delete submit test files changelist");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					client.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[]{targetFile,
									targetFile2}), new DeleteFilesOptions()
							.setChangelistId(deleteChangelist.getId()));
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
