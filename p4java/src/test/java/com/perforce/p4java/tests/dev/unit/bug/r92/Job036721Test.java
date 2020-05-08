/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import com.perforce.p4java.tests.dev.UnitTestDevServerManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Note: test is expected to fail at the moment.
 *
 * @testid Job036721Test
 * @job job036721
 */

@TestId("Job036721Test")
@Jobs({"job036721"})
@Ignore("windows fails with this test, because the `unicode` file system can't be created by the os.")
public class Job036721Test extends P4JavaTestCase {
	// p4ic4idea: use local server
	@BeforeClass
	public static void oneTimeSetUp() {
		UnitTestDevServerManager.INSTANCE.startTestClass("unicode");
	}
	@AfterClass
	public static void oneTimeTearDown() {
		UnitTestDevServerManager.INSTANCE.endTestClass("unicode");
	}

	@Test
	public void testSync() throws Exception {
		final String clientName = getRandomClientName(null);
		final String testCharsetName = "shiftjis";
		final String testMapping00 = "//depot/viv/test/\u00e3?\u00af\u00e3?\u0082\u00e3?\u00b5test/... "
				+ "//" + clientName + "/viv/test/...";

		IServer server = null;
		IClient client = null;
		try {
			server = getServer();
			assertNotNull("Null server returned for Unicode server", server);
			if (!server.setCharsetName(testCharsetName)) {
				fail("Unable to set charset to '" + testCharsetName + "' for Unicode server");
			}
			Client testClient = makeTempClient(clientName, server);
			assertNotNull(testClient);
			ClientView clientView = new ClientView();
			ClientViewMapping clientViewMapping = new ClientViewMapping(0, testMapping00);
			clientView.addEntry(clientViewMapping);
			testClient.setClientView(clientView);
			clientView.setClient(testClient);
			client = testClient;
			String createResult = server.createClient(testClient);
			server.setCurrentClient(testClient);
			assertNotNull("Null test client create string return", createResult);
			assertTrue("Test client '" + clientName + "' not created: " + createResult,
					createResult.contains("saved"));
			List<IFileSpec> syncList = testClient.sync(null, true, false, false, false);
			assertNotNull("Null sync list returned from sync op", syncList);
			for (IFileSpec fspec : syncList) {
				assertNotNull("Null sync filespec in filespec list", fspec);
				assertFalse(fspec.getStatusString(), fspec.getOpStatus() == FileSpecOpStatus.ERROR);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					@SuppressWarnings("unused") // string used for debugging
							String delRsltStr = server.deleteClient(clientName, false);
				}
				server.disconnect();
			}
		}
	}
}
