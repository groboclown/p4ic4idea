package com.perforce.p4java.tests.dev.unit.features173;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.features172.FilesysUTF8bomTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ChangeViewClientTest extends P4JavaRshTestCase {

	private static final String clientName = "changeView-client";
	private static final String userName = "p4jtestsuper";

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", FilesysUTF8bomTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		setupServer(p4d.getRSHURL(), userName, userName, false, properties);
	}

	@After
	public void cleanup() throws Exception {
		server.deleteClient(clientName, true);
	}

	@Test
	public void testBasicChangeView() throws Exception {
		// Creates new client
		String clientRoot = p4d.getPathToRoot() + "/client";
		String[] paths = {"//depot/r171/... //" + clientName + "/..."};
		IClient testClient = Client.newClient(server, clientName, "filesys.utf8bom=0 test", clientRoot, paths);

		// Simple change view list
		ArrayList<String> changeView = new ArrayList<>();
		changeView.add("//depot/r171/...@now");
		testClient.setChangeView(changeView);

		// Create client
		server.createClient(testClient);

		client = server.getClient(clientName);
		assertNotNull(client);
		assertEquals(1, client.getChangeView().size());
	}

	@Test
	public void testMultiLineChangeView() throws Exception {
		// Creates new client
		String clientRoot = p4d.getPathToRoot() + "/client";
		String[] paths = {"//depot/r171/... //" + clientName + "/..."};
		IClient testClient = Client.newClient(server, clientName, "filesys.utf8bom=0 test", clientRoot, paths);

		// Simple change view list
		ArrayList<String> changeView = new ArrayList<>();
		changeView.add("//depot/A/...@now");
		changeView.add("//depot/B/...@now");
		changeView.add("//depot/C/...@now");
		changeView.add("//depot/D/...@now");
		testClient.setChangeView(changeView);

		// Create client
		server.createClient(testClient);

		client = server.getClient(clientName);
		assertNotNull(client);
		assertEquals(4, client.getChangeView().size());
		changeView = client.getChangeView();
		changeView.remove(2);
		client.setChangeView(changeView);
		client.update();

		// read back and check values
		client = server.getClient(clientName);
		assertNotNull(client);
		assertEquals(3, client.getChangeView().size());
	}

	@Test
	public void testClientBackup() throws Exception {
		// Creates new client
		String clientRoot = p4d.getPathToRoot() + "/client";
		String[] paths = {"//depot/r171/... //" + clientName + "/..."};
		IClient testClient = Client.newClient(server, clientName, "filesys.utf8bom=0 test", clientRoot, paths);
		testClient.setBackup("disable");

		// Create client
		server.createClient(testClient);

		client = server.getClient(clientName);
		assertNotNull(client);
		assertEquals("disable", client.getBackup());

		client.setBackup("enable");
		client.update();

		// read back and check values
		client = server.getClient(clientName);
		assertNotNull(client);
		assertNull(client.getBackup());
	}
}
