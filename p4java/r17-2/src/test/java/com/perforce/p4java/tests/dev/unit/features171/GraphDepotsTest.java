package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IRepo;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.ReposOptions;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GraphDepotsTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";
	private static final String clientName = "p4jtestsuper.ws";

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphHaveTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), userName, userName, true, properties);
	}

	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			client = server.getClient(clientName);
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void depotsOutput() {
		// Run have
		try {
			List<IDepot> depots = server.getDepots();
			assertNotNull(depots);

			IDepot graph = null;
			for(IDepot depot : depots) {
				if("graph".equals(depot.getName())) {
					graph = depot;
				}
			}
			assertNotNull(graph);
			assertEquals(IDepot.DepotType.GRAPH, graph.getDepotType());

		} catch (ConnectionException e) {
			fail("Unexpected ConnectionException: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void ServerReposOutput() {
		try {
			List<IRepo> repos = server.getRepos();
			assertNotNull(repos);
			assertTrue(repos.size() > 5);

		} catch (ConnectionException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void ServerReposOptionsOutput() {
		try {
			ReposOptions opts = new ReposOptions();
			opts.setMaxResults(2);
			List<IRepo> repos = server.getRepos(opts);
			assertNotNull(repos);
			assertEquals(2, repos.size());

			opts = new ReposOptions();
			opts.setUser("pallen");
			repos = server.getRepos(opts);
			assertNotNull(repos);
			assertEquals(0, repos.size());

			opts = new ReposOptions();
			opts.setOwner("pallen");
			repos = server.getRepos(opts);
			assertNotNull(repos);
			assertEquals(0, repos.size());

			opts = new ReposOptions();
			opts.setNameFilter("//graph/repo*");
			repos = server.getRepos(opts);
			assertNotNull(repos);
			assertEquals(2, repos.size());

		} catch (P4JavaException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void ClientReposOutput() {
		try {
			List<IRepo> repos = client.getRepos();
			assertNotNull(repos);
			assertEquals(1, repos.size());

			IRepo repo = repos.get(0);
			assertEquals("//graph/p4-plugin.git", repo.getName());
			assertEquals(userName, repo.getOwnerName());
			assertTrue(1400000000000L < repo.getCreatedDate().getTime());
			assertTrue(1400000000000L < repo.getPushedDate().getTime());
		} catch (ConnectionException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		}
	}
}