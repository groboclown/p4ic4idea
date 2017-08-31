/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test force delete of other users' pending changelist: 'p4 change -f changelist#'
 */
@Jobs({ "job063310" })
@TestId("Dev131_ForceDeletePendingChangelistTest")
public class ForceDeletePendingChangelistTest extends P4JavaTestCase {

	IOptionsServer superServer = null;
	IClient superClient = null;

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			superServer = getServerAsSuper();
			superClient = superServer.getClient("p4TestSuperWS20112");
			assertNotNull(superClient);
			superServer.setCurrentClient(superClient);
			
			server = getServer();
			assertNotNull(server);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

    /**
     * Test force delete of other users' pending changelist: 'p4 change -f changelist#'
     */
    @Test
	public void testForceDeletePendingChangelist() {
    	// Pending changelist
    	IChangelist changelist = null;
    	
		try {
			changelist = getNewChangelist(server, client,
					"Dev131_ForceDeletePendingChangelistTest pending changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			String result = null;
			// Delete the changelist without the '-f' flag
			try {
				result = superServer.deletePendingChangelist(changelist.getId());
			} catch (Exception exec) {
				assertNotNull(exec);
				assertNotNull(exec.getMessage());
				assertTrue(exec.getMessage().contains("Change " + changelist.getId() + " belongs to client " + client.getName()));
			}

			// Now, delete the changelist with the '-f' flag
			result = superServer.deletePendingChangelist(changelist.getId(), new ChangelistOptions().setForce(true));
			assertNotNull(result);
			assertTrue(result.contains("Change " + changelist.getId() + " deleted."));

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
