/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.perforce.p4java.P4JavaUtil;
import com.perforce.p4java.StandardPerforceServers;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.test.ServerRule;
import org.junit.Rule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import org.junit.rules.TemporaryFolder;

/**
 * Test Job040829 -- missing errors when attempting to sync
 * a client that's not the current client.
 */
@TestId("Bugs101_Job040829Test")
public class Job040829Test {
	@Rule
	public ServerRule serverRule = StandardPerforceServers.createP4Java20101();

	@Rule
	public TemporaryFolder clientRoot = new TemporaryFolder();

	public Job040829Test() {
	}

	@Test
	public void testUnattachedClientSync()
			throws Exception {
		final String testClientName = "Bugs101_Job040829TestClient";
		final String exceptionMessage
						= "Attempted to sync a client that is not the server's current client";
		IOptionsServer server = null;
		IClient client = null;

		try {
			server = P4JavaUtil.getServer(serverRule.getRshUrl(), StandardPerforceServers.getStandardUserProperties());
			client = server.getClient(testClientName);
			assertNotNull("could not get client '" + testClientName + "'", client);
			@SuppressWarnings("unused")
			List<IFileSpec> syncFiles = client.sync(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
			fail("did not detect unattached client");
		} catch (RequestException rexc) {
			assertNotNull("null request exception message", rexc.getMessage());
			assertEquals("did not see expected RequestException message",
								exceptionMessage, rexc.getMessage());
		}
	}
}
