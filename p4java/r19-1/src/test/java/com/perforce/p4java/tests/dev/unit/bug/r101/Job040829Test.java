/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.tests.LocalServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaLocalServerTestCase;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test Job040829 -- missing errors when attempting to sync
 * a client that's not the current client.
 */
@TestId("Bugs101_Job040829Test")
public class Job040829Test extends P4JavaLocalServerTestCase {

	@ClassRule
	public static LocalServerRule p4d = new LocalServerRule("r18.1", Job040829Test.class.getSimpleName(), "localhost:18100");

	@Test
	public void testUnattachedClientSync() throws Exception {
		setupServer(p4d.getP4JavaUri(), superUserName, superUserPassword, false, null);
		final String testClientName = "ws-test";
		final String exceptionMessage = "Attempted to sync a client that is not the server's current client";
		IClient client = null;

		try {
			client = server.getClient(testClientName);
			assertNotNull("could not get client '" + testClientName + "'", client);
			@SuppressWarnings("unused")
			List<IFileSpec> syncFiles = client.sync(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
			fail("did not detect unattached client");
		} catch (RequestException rexc) {
			assertNotNull("null request exception message", rexc.getMessage());
			assertEquals("did not see expected RequestException message",
					exceptionMessage, rexc.getMessage());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
