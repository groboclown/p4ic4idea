/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for job040299. Not terribly interesting, but here it is...
 */
@TestId("Job040299Test")
public class Job040299Test extends P4JavaTestCase {

	public Job040299Test() {
	}

	@Test
	public void testJob040299ClientOpenedFilesOptions() {
		IOptionsServer server = null;
		IClient client = null;
		final String clientName = "p4jtest-job040299";

		try {
			server = getServer();
			client = server.getClient(clientName);
			assertNotNull(client);
			server.setCurrentClient(client);
			
			// There shouldn't be *any* files open for this client...
			List<IFileSpec> clientOpenFiles = client.openedFiles(
												FileSpecBuilder.makeFileSpecList("//..."),
												new OpenedFilesOptions());
			assertNotNull(clientOpenFiles);
			List<IFileSpec> allOpenFiles = client.openedFiles(FileSpecBuilder.makeFileSpecList("//..."),
												new OpenedFilesOptions().setAllClients(true));
			assertNotNull(allOpenFiles);
			assertEquals(clientOpenFiles.size(), allOpenFiles.size());
			allOpenFiles = client.openedFiles(FileSpecBuilder.makeFileSpecList("//..."),
										new OpenedFilesOptions().setClientName("Xyz"));
			assertNotNull(allOpenFiles);
			assertEquals(clientOpenFiles.size(), allOpenFiles.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
