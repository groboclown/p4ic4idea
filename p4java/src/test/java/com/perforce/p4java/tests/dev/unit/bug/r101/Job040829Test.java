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
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test Job040829 -- missing errors when attempting to sync
 * a client that's not the current client.
 */
@TestId("Bugs101_Job040829Test")
public class Job040829Test extends Abstract101TestCase {

	public Job040829Test() {
	}

	@Test
	public void testUnattachedClientSync() {
		final String testClientName = "Bugs101_Job040829TestClient";
		final String exceptionMessage
						= "Attempted to sync a client that is not the server's current client";
		IOptionsServer server = null;
		IClient client = null;

		try {
			server = getServer();
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
