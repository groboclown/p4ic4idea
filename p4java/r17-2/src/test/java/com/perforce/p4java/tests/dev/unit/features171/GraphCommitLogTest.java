package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.graph.CommitAction;
import com.perforce.p4java.graph.ICommit;
import com.perforce.p4java.option.server.GraphCommitLogOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@TestId("Dev171_GraphCommitLogTest")
public class GraphCommitLogTest extends P4JavaRshTestCase {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphCommitLogTest.class.getSimpleName());

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
	 * Tests the retrieval of commit log from a given depot
	 */
	@Test
	public void allCommitLogsExistingDepot() {
		String depot = "//graph/p4-plugin";

		try {
			GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
			graphCommitLogOptions.setRepo(depot);
			graphCommitLogOptions.setUser("Paul Allen");
			List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);

			assertNotNull(logResult);
			assertTrue(logResult.size() > 0);

			graphCommitLogOptions.setUser("pallen");
			logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);
			assertTrue(logResult.size() > 0);

			graphCommitLogOptions.setUser("pallen@perforce.com");
			logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);
			assertTrue(logResult.size() > 0);

			graphCommitLogOptions.setUser("tpethiyagoda");
			logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);

			assertNotNull(logResult);
			assertEquals(0, logResult.size());

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Tests the retrieval of commit log from given depot that does not exist
	 */
	@Test()
	public void commitLogWithNonExistingDepot() throws Exception {
		String depot = "//graph/p4-plugin-invalid";

		GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
		graphCommitLogOptions.setRepo(depot);
		exception.expect(P4JavaException.class);
		List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
	}

	/**
	 * Tests the retrieval of commit log from given depot and max results
	 */
	@Test
	public void commitLogWithMaxValue() {
		String depot = "//graph/p4-plugin";

		try {
			GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
			graphCommitLogOptions.setRepo(depot);
			graphCommitLogOptions.setMaxResults(5);

			List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);

			assertNotNull(logResult);
			assertEquals(5, logResult.size());

			graphCommitLogOptions.setMaxResults(0);
			logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);

			assertNotNull(logResult);
			assertEquals(649, logResult.size());

			graphCommitLogOptions.setMaxResults(-1);
			logResult = server.getGraphCommitLogList(graphCommitLogOptions);
			assertNotNull(logResult);

			assertNotNull(logResult);
			assertEquals(649, logResult.size());
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Tests the retrieval of commit log from given depot that belongs to a commit SHA value
	 */
	@Test
	public void commitLogForCommitSHA() throws RequestException, P4JavaException {
		String depot = "//graph/p4-plugin";

		String[] commitValueTest1 = {"07c6c96621eafe23ae5ec411a9811ad6d3bf49da"};

		GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitValueTest1);

		List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);

		assertEquals(2, logResult.size());

		String[] commitValueTest2 = {"40b2770a413267e79b8b3d3adf299dda44b8161f", "d82a0624a2f64ee867b66a575f924d6147d0695c"};

		graphCommitLogOptions.setCommitValue(commitValueTest2);
		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);

		assertEquals(6, logResult.size());

		//non existent SHA value
		String[] commitValueTest3 = {"40b2770a413267e79b8b3d3adf299dda44b81"};

		exception.expect(com.perforce.p4java.exception.RequestException.class);
		graphCommitLogOptions.setCommitValue(commitValueTest3);
		try {
			logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		} catch (Exception ex) {
			// ex.printStackTrace();
			//do nothing so that control does not
			//end here and the assertion below is executed.
		}

		exception.expect(com.perforce.p4java.exception.RequestException.class);
		String[] commitValueTest4 = {"40b2770a413267e79b8b3d3adf299dda44b8161fd82a0624a2f64ee867b66a575f924d6147d0695c"};
		graphCommitLogOptions.setCommitValue(commitValueTest4);
		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
	}

	/**
	 * Tests the contents returned by the p4 graph log command
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void commitLogContents() throws P4JavaException {
		String depot = "//graph/p4-plugin";

		GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setMaxResults(5);

		List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);

		assertNotNull(logResult);
		assertEquals(5, logResult.size());

		for (ICommit log : logResult) {
			assertNotNull(log.getCommit());
			assertNotNull(log.getTree());
		}

		//Test the contents of a single commit log
		String expectedCommitSHA = "d6681fcf406d477a6172815974ce53740b24b771";
		String expectedCommitTree = "677c74532bf7932575b4729bbddba1e5fe63874b";

		graphCommitLogOptions.setMaxResults(1);
		graphCommitLogOptions.setCommitValue(expectedCommitSHA);
		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);

		assertNotNull(logResult);
		assertEquals(1, logResult.size());

		ICommit commitLog = logResult.get(0);
		assertNotNull(commitLog);
		assertEquals(expectedCommitSHA, commitLog.getCommit());
		assertEquals(expectedCommitTree, commitLog.getTree());
		assertEquals(CommitAction.MERGE, commitLog.getAction());
		assertTrue(1400000000000L < commitLog.getDate().getTime());
		assertEquals("Paul Allen", commitLog.getAuthor());
		assertEquals("Initial commit", commitLog.getDescription().trim());
	}

	/**
	 * Tests various combinations os -N and -X options
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void commitLogParents() throws P4JavaException {
		String depot = "//graph/p4-plugin";

		GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setMinParents(3);

		//This should not return any results as minParents are greater than available
		//even though the maxResults is set to 5
		List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);

		assertNotNull(logResult);
		assertEquals(0, logResult.size());

		//This should return 5 results as min parents are within available and
		//maxResults is set to 5
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setMinParents(2);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(5, logResult.size());

		//This should return 5 as available parents fall within maxParents(8)
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setMaxParents(8);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(5, logResult.size());

		//This should not return any results
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setMaxParents(1);
		graphCommitLogOptions.setMinParents(2);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(0, logResult.size());

		//This should return results
		graphCommitLogOptions.setMaxResults(2);
		graphCommitLogOptions.setMaxParents(1);
		graphCommitLogOptions.setMinParents(1);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(2, logResult.size());

		//This should not return any results as availability is lower than minParents
		graphCommitLogOptions.setMaxResults(2);
		graphCommitLogOptions.setMinParents(10);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(0, logResult.size());
	}

	/**
	 * Tests -N and -X options with a given commit SHA
	 */
	@Test
	public void commitLogForOptionsNandXWithCommitSHA() throws P4JavaException {
		String depot = "//graph/p4-plugin";
		//This SHA has only one parent
		String commitSHA1Parent = "07c6c96621eafe23ae5ec411a9811ad6d3bf49da";
		String commitSHA2Parents = "e9a370b5ec856951e944086392962592696e67ee";

		//This should not return any as this SHA has only one parent while the -N is set to 3 and we have only 1 parent
		GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA1Parent);
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setMinParents(3);

		List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(0, logResult.size());

		//This should return 1 now as -N is set to 1

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA1Parent);
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setMinParents(1);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(1, logResult.size());

		//Test options -N and -X against a commit SHA that has two parents

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA2Parents);
		graphCommitLogOptions.setMaxResults(15);
		graphCommitLogOptions.setMinParents(2);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(15, logResult.size());

		//Set -N to 1 and should still return results as the commit has 2 parents

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA2Parents);
		graphCommitLogOptions.setMaxResults(15);
		graphCommitLogOptions.setMinParents(1);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(15, logResult.size());

		//This should not return any results as -N 3 is now greater than available parents

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA2Parents);
		graphCommitLogOptions.setMaxResults(15);
		graphCommitLogOptions.setMinParents(3);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(0, logResult.size());

		//Should return results as this test now sets the -X 3 and available parents fall below 3

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA2Parents);
		graphCommitLogOptions.setMaxResults(15);
		graphCommitLogOptions.setMinParents(2);
		graphCommitLogOptions.setMaxParents(3);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(15, logResult.size());

		//Should return results as this test now sets the -X 2 and available parents fall within 2

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setCommitValue(commitSHA2Parents);
		graphCommitLogOptions.setMaxResults(15);
		graphCommitLogOptions.setMinParents(2);
		graphCommitLogOptions.setMaxParents(2);

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(15, logResult.size());
	}

	/**
	 * Tests the options -A and -B
	 */
	@Test
	public void commitLogForOptionsAandB() throws P4JavaException {
		String depot = "//graph/p4-plugin";
		//This SHA has only one parent
		String commitSHA1Parent = "07c6c96621eafe23ae5ec411a9811ad6d3bf49da";
		String commitSHA2Parents = "e9a370b5ec856951e944086392962592696e67ee";

		//This should not return any as this SHA has only one parent while the -N is set to 3 and we have only 1 parent
		GraphCommitLogOptions graphCommitLogOptions = new GraphCommitLogOptions();
		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setStartDate("2014/06/26");

		List<ICommit> logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(5, logResult.size());

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setMaxResults(15);
		graphCommitLogOptions.setStartDate("2014/06/26:01:00:00");
		graphCommitLogOptions.setEndDate("2014/06/26:22:00:00");

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertNotEquals(0, logResult.size());
		assertTrue(logResult.size() < 15);

		graphCommitLogOptions.setRepo(depot);
		graphCommitLogOptions.setMaxResults(5);
		graphCommitLogOptions.setStartDate("2014/06/27:13:00:00");
		graphCommitLogOptions.setEndDate("2014/06/26:14:10:00");

		logResult = server.getGraphCommitLogList(graphCommitLogOptions);
		assertNotNull(logResult);
		assertEquals(0, logResult.size());
	}
}
