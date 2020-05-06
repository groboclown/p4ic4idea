/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.impl.generic.core.User;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test get restricted changelist: 'p4 change -o -f changelist#'
 */
@Jobs({ "job062697" })
@TestId("GetRestrictedChangelistTest")
public class GetRestrictedChangelistTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", GetRestrictedChangelistTest.class.getSimpleName());

	IChangelist changelist = null;
	IOptionsServer superServer = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		String user2Name = "p4jtestuser2";
		User user2 = User.newUser(user2Name, "test@test.com", "test", user2Name);

		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			superServer = getSuperConnection(p4d.getRSHURL());
			assertNotNull(superServer);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() throws Exception {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

    /**
     * Test get restricted changelist: 'p4 change -o -f changelist#'
     */
    @Test
	public void testGetRestrictedChangelist() {
    	int changelistID = 30124;
    	// This pending changelist is restricted to view by user p4jtestuser2
		try {
			// Get the changelist without the '-f' flag
			IChangelist changelist = server.getChangelist(changelistID);
			assertNotNull(changelist);
			assertTrue(changelist.getDescription().contains("restricted, no permission to view"));

			// Now, get the changelist with the '-f' flag
			changelist = superServer.getChangelist(changelistID, new ChangelistOptions().setForce(true));
			assertNotNull(changelist);
			assertFalse(changelist.getDescription().contains("restricted, no permission to view"));

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
