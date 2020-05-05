/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test force delete of other users' pending changelist: 'p4 change -f changelist#'
 */
@Jobs({ "job063310" })
@TestId("Dev131_ForceDeletePendingChangelistTest")
public class ForceDeletePendingChangelistTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", ForceDeletePendingChangelistTest.class.getSimpleName());

	private IOptionsServer superServer = null;
	private IClient superClient = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		final String superClientName = "p4TestSuperWS20112";
		final String clientName = "p4TestUserWS20112";

		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = createClient(server,clientName);
			assertNotNull(client);
			server.setCurrentClient(client);

			superServer = getServerAsSuper(p4d.getRSHURL());
			superClient = createClient(superServer, superClientName);
			assertNotNull(superClient);
			superServer.setCurrentClient(superClient);
		} catch (Exception e) {
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
