/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests the rather insidious RPC protocol-implicated bug at the heart of
 * job036870. Under "proper" circumstances the results of adding and then submitting
 * a non-existent file should be an embedded error message that contains the string
 * "Some file(s) could not be transferred from client" (and probably more than that);
 * with the bug, we only saw an incomprehensible message to do with forsaken files...
 * 
 * @jobs job036870
 * @testid Job036870Test
 */

@TestId("Job036870Test")
@Jobs({"job036870"})
public class Job036870Test extends P4JavaTestCase {

	@Test
	public void testAddSubmit() {
		IServer server = null;
		IClient client = null;
		String fileName = getRandomName(null) + ".txt";
		String filePath = null;
		IChangelist newChangelist = null;
		final String expectedErrMsg = "No such file or directory";
		try {
			String clientName = null;
			server = getServer();
			assertNotNull(server);
			Client clientImpl = makeTempClient(null, server);
			ClientView clientView = new ClientView();
			clientView.addEntry(new ClientViewMapping(0, "//depot/...", "//" + clientImpl.getName() + "/..."));
			clientImpl.setClientView(clientView);
			String rsltStr = server.createClient(clientImpl);
			assertNotNull(rsltStr);
			assertTrue("result string: " + rsltStr, rsltStr.contains("saved"));
			client = server.getClient(clientImpl.getName());
			server.setCurrentClient(client);
			assertNotNull(client.getRoot());
			File rootDir = new File(client.getRoot());
			if (!rootDir.exists()) {
				assertTrue("Unable to create client root directory: " + client.getRoot(),
									rootDir.mkdir());
			}
			clientName = client.getName();
			assertNotNull(clientName);
			filePath = "//" + client.getName() + "/"  + fileName;

			Changelist changelist  = new Changelist(
											IChangelist.UNKNOWN,
											client.getName(),
											getUserName(),
											ChangelistStatus.NEW,
											null,
											"Test changelist for " + testId + " test",
											false,
											(Server) server
					);
			assertNotNull(changelist);
			newChangelist = client.createChangelist(changelist);
			assertNotNull(newChangelist);
			List<IFileSpec> addedFiles = client.addFiles(
								FileSpecBuilder.makeFileSpecList(filePath),
								false, newChangelist.getId(), "text", false);
			assertNotNull(addedFiles);
			newChangelist.refresh();
			List<IFileSpec> submittedFiles = newChangelist.submit(false);
			assertNotNull(submittedFiles);
			boolean errFound = false;
			for (IFileSpec fSpec : submittedFiles) {
				// Logic here is a little convoluted due to 
				// debugging requirements...
				assertNotNull(fSpec);
				if (fSpec.getOpStatus() != FileSpecOpStatus.VALID) {
					assertNotNull(fSpec.getStatusMessage());
					if (fSpec.getStatusMessage().contains(expectedErrMsg)) {
						errFound = true;
					}
				}
			}
			if (!errFound) {
				fail("Did not handle missing client-side file correctly.");
			}
		} catch (Exception exc) {
			fail(exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					try {
						if (newChangelist != null) {
							List<IFileSpec> revertFiles = client.revertFiles(
											FileSpecBuilder.makeFileSpecList(filePath),
											new RevertFilesOptions()
												.setChangelistId(newChangelist.getId()));
							assertNotNull(revertFiles);
							String str = server.deletePendingChangelist(newChangelist.getId());
							assertNotNull(str);
						}
						String delStr = server.deleteClient(client.getName(), false);
						assertNotNull(delStr);
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}
	}
}
