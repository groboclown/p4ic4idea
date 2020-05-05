package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GraphHaveTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";
	private static final String clientName = "GraphHaveTest.ws";
	private static final String templateName = "p4jtestsuper.ws";

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphHaveTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), userName, userName, true, properties);
	}

	@Before
	public void setUp() {
		// initialization code (before each test).
		try {


			IClient templateClient = server.getClient(templateName);

			Client implClient = new Client(server);
			implClient.setName(clientName);
			implClient.setOwnerName(userName);
			implClient.setDescription(clientName);
			implClient.setRoot(p4d.getPathToRoot() + "/" + clientName);

			String message = server.createClient(implClient);
			assertNotNull(message);

			SwitchClientViewOptions opts = new SwitchClientViewOptions();
			opts.setForce(true);
			message = server.switchClientView(templateName, clientName, opts);
			assertNotNull(message);

			client = server.getClient(clientName);
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void haveOutput() {
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList("//...");
		SyncOptions syncOpts = new SyncOptions();

		// Force sync client (graph and stream files)
		try {
			syncOpts.setForceUpdate(true);
			List<IFileSpec> spec = client.sync(fileSpec, syncOpts);
			assertNotNull(spec);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}

		// Run have
		try {
			List<IFileSpec> have = client.graphHaveList(fileSpec);
			assertNotNull(have);

			assertNotNull(have.get(0));
			assertEquals("15b25972edfc52d0a0b1e80249bd8efdc914af92", have.get(0).getSha());
			assertEquals("//graph/p4-plugin.git", have.get(0).getRepoName());
			assertEquals("refs/heads/master", have.get(0).getBranch());

			have = client.haveList(fileSpec);
			assertNotNull(have.get(0));
			assertEquals("//p4-perl/main/main/Build/Version.pm", have.get(0).getDepotPathString());

		} catch (ConnectionException e) {
			fail("Unexpected ConnectionException: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		}
	}
}
