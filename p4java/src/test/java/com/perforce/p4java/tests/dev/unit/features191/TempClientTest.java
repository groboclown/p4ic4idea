package com.perforce.p4java.tests.dev.unit.features191;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TempClientTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r19.1", TempClientTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@BeforeClass
	public static void setUp() {
		try {
			setupServer(p4d.getNonThreadSafeRSHURL(), userName, password, true, null);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@AfterClass
	public static void tearDown() {
		if (server != null) {
			endServerSession(server);
		}
	}

	@Test
	public void testAddFile() throws Exception {
		String clientName = "tempClientX";
		String clientDescription = "temp client for test";
		String clientRoot = Paths.get(server.getServerInfo().getServerRoot() + File.separator + "testClients" + File.separator + clientName).toAbsolutePath().toString();
		String[] clientViews = {"//depot/... //" + clientName + "/..."};
		IClient tmpClient = Client.newClient(server, clientName, clientDescription, clientRoot, clientViews);

		server.createTempClient(tmpClient);
		tmpClient = server.getClient(clientName);
		server.setCurrentClient(tmpClient);

		List<IClientSummary> clients = server.getClients(null, clientName, 1);
		assertEquals(1, clients.size());

		createTextFileOnServer(tmpClient, "testFile", "test change");

		server.disconnect();
		TimeUnit.SECONDS.sleep(3);
		server.connect();
		clients = server.getClients(null, clientName, 1);
		assertEquals(0, clients.size());
	}
}
