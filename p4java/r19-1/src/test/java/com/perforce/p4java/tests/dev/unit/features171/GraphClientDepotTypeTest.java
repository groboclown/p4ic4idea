package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.client.ViewDepotType;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 */
@TestId("Dev171_GraphClientDepotTypeTest")
public class GraphClientDepotTypeTest extends P4JavaRshTestCase {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphClientDepotTypeTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
	}

	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			client = server.getClient(getPlatformClientName("GraphCatFile.ws"));
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Tests the case where the server returns a valid depot type
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void testDepotTypeIfServerReturnsAvalidValue() throws P4JavaException {
		IClient client = server.getClient("GraphCatFile.ws");
		assertNotNull(client);
		assertEquals(ViewDepotType.GRAPH, client.getViewDepotType());

		client = server.getClient("p4jtestsuper.ws");
		assertNotNull(client);
		assertEquals(ViewDepotType.HYBRID, client.getViewDepotType());
	}

	/**
	 * Tests the case where server returns no depot type.
	 * In this cas the system reverts to the default depot type of ViewDepotType.LOCAL
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void testDepotTypeIfServerReturnsNull() throws P4JavaException {

		String[] paths = {"//depot/... //local-client/..."};
		IClient testClient = Client.newClient(server, "local-client", "testing depot type", "/var/temp/local-client", paths);
		server.createClient(testClient);

		IClient testClientFromServer = server.getClient("local-client");
		assertNotNull(testClientFromServer);
		assertEquals(ViewDepotType.LOCAL, testClientFromServer.getViewDepotType());
	}

	/**
	 * Tests the case for 'Type' field
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void testDepotTypeField() throws P4JavaException {
		IClient client = server.getClient("GraphCatFile.ws");
		assertNotNull(client);
		assertEquals(null, client.getType());

		client.setType("graph");
		client.update();

		client = server.getClient("GraphCatFile.ws");
		assertEquals("graph", client.getType());
	}

	/**
	 * Test for setting existing client to readonly: fails as not allowed
	 *
	 * @throws P4JavaException
	 * @throws IOException
	 */
	@Test
	public void settingExstingClientToReadOnly() throws P4JavaException, IOException {

		IClient client = server.getClient("GraphCatFile.ws");
		assertNotNull(client);

		exception.expect(com.perforce.p4java.exception.RequestException.class);

		client.setType("readonly");
		client.update();

		client = server.getClient("GraphCatFile.ws");
		assertEquals("readonly", client.getType());
	}

	/**
	 * Test for setting existing client to partitioned: fails as not allowed
	 *
	 * @throws P4JavaException
	 * @throws IOException
	 */
	@Test
	public void settingExistingClientToPartitioned() throws P4JavaException, IOException {

		IClient client = server.getClient("GraphCatFile.ws");
		assertNotNull(client);

		exception.expect(com.perforce.p4java.exception.RequestException.class);

		client.setType("partitioned");
		client.update();

		client = server.getClient("GraphCatFile.ws");
		assertEquals("partitioned", client.getType());
	}

	/**
	 * Test for creating a new readonly client on a server not set to accept readonly clients.
	 *
	 * @throws P4JavaException
	 * @throws IOException
	 */
	@Test
	public void newReadonlyClient() throws P4JavaException, IOException {

		String clientRoot = "/var/tmp/test-readonly-client";
		String clientName = "test-readonly-client";
		String[] paths = {"//graph/... //" + clientName + "/..."};

		exception.expect(com.perforce.p4java.exception.RequestException.class);

		IClient testClient = Client.newClient(server, clientName, "testing readonly client", clientRoot, paths);
		testClient.setType("readonly");
		server.createClient(testClient);
	}

	/**
	 * Test for creating a new partitioned client on a server not set to accept partitioned clients.
	 *
	 * @throws P4JavaException
	 * @throws IOException
	 */
	@Test
	public void newPartitionedClient() throws P4JavaException, IOException {

		String clientRoot = "/var/tmp/test-partitioned-client";
		String clientName = "test-test-partitioned-client";
		String[] paths = {"//graph/... //" + clientName + "/..."};

		exception.expect(com.perforce.p4java.exception.RequestException.class);

		IClient testClient = Client.newClient(server, clientName, "testing test-partitioned client", clientRoot, paths);
		testClient.setType("test-partitioned");
		server.createClient(testClient);
	}

	/**
	 * Test for creating a brand new readonly client on a server
	 * configured to accept read only clients.
	 *
	 * @throws P4JavaException
	 * @throws IOException
	 */
	@Test
	public void newReadonlyClientOnASupportedServer() throws P4JavaException, IOException {

		server.setOrUnsetServerConfigurationValue("client.readonly.dir", "/var/tmp/readonly-client");

		String clientRoot = "/var/tmp/test-readonly-client";
		String clientName = "test-readonly-client";
		String[] paths = {"//graph/p4-plugin/src/test/java/... //" + clientName + "/..."};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing readonly client", clientRoot, paths);
		testClient.setType("readonly");
		server.createClient(testClient);

		client = server.getClient(clientName);
		assertNotNull(client);
		assertEquals("readonly", client.getType());

		server.setCurrentClient(client);

		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//graph/p4-plugin/src/test/java/...");
		List<IFileSpec> resultSpec = client.sync(fileSpec, syncOptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertEquals(16, resultSpec.size());
	}

	/**
	 * Test for creating a brand new readonly client on a server
	 * configured to accept read only clients.
	 *
	 * @throws P4JavaException
	 * @throws IOException
	 */
	@Test
	public void newPartitionedClientOnASupportedServer() throws P4JavaException, IOException {

		server.setOrUnsetServerConfigurationValue("client.readonly.dir", "/var/tmp/readonly-client");

		String clientRoot = "/var/tmp/test-partitioned-client";
		String clientName = "test-partitioned-client";
		String[] paths = {"//graph/p4-plugin/src/test/java/... //" + clientName + "/..."};

		FileUtils.deleteDirectory(new File(clientRoot));

		IClient testClient = Client.newClient(server, clientName, "testing partitioned client", clientRoot, paths);
		testClient.setType("partitioned");
		server.createClient(testClient);

		client = server.getClient(clientName);
		assertNotNull(client);
		assertEquals("partitioned", client.getType());

		server.setCurrentClient(client);

		SyncOptions syncOptions = new SyncOptions();
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//graph/p4-plugin/src/test/java/...");
		List<IFileSpec> resultSpec = client.sync(fileSpec, syncOptions);
		Assert.assertNotNull(resultSpec);
		Assert.assertEquals(16, resultSpec.size());
	}
}
