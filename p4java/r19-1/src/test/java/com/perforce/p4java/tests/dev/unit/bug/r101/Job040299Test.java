/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test for job040299. Not terribly interesting, but here it is...
 */
@TestId("Job040299Test")
public class Job040299Test extends P4JavaRshTestCase {

	public Job040299Test() {
	}

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", Job040299Test.class.getSimpleName());

	IClient client = null;
	
	/**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        try {
            setupServer(p4d.getRSHURL(), userName, password, true, props);
            createClient(server, "p4jtest-job040299");
            server.setCurrentClient(client);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } 
    }
	@Test
	public void testJob040299ClientOpenedFilesOptions() {
		IClient client = null;
		final String clientName = "p4jtest-job040299";

		try {
			client = server.getClient(clientName);
			 assertNotNull(client);
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
