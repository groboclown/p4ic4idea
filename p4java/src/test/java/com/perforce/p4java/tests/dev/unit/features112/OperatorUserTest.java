/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test of create, delete and retrieve Perforce operator users. Requires super
 * user login setup.
 */
@Jobs({ "job046114" })
@TestId("Dev112_OperatorUserTest")
public class OperatorUserTest extends P4JavaTestCase {

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
			server = getServerAsSuper();
			assertNotNull(server);
            client = getDefaultClient(server);
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

	public static final String USER_NAME = "optestuser";

    @Test
    public void testCreateDeleteOperatorUser() {

        try {
    		int randNum = getRandomInt();
            String newUserName = testId + randNum;
            String email = newUserName + "@invalid.invalid";
            String fullName = testId + "New P4Java Test Operator User "
                    + randNum;
            IUser newUser = new User(newUserName, email, fullName, null, null,
                    null, null, UserType.OPERATOR, null);
            server.createUser(newUser, true);

            IUser retrievedUser = server.getUser(newUserName);
            assertNotNull("Unable to find new user: " + newUserName,
                    retrievedUser);
            assertEquals("user type mismatch", UserType.OPERATOR,
                    retrievedUser.getType());

            retrievedUser.setEmail("testEmail@localhost.localdomain");
            retrievedUser.update(true);

            retrievedUser = server.getUser(newUserName);
            assertNotNull("Unable to find updated user: " + newUserName,
                    retrievedUser);

            assertEquals(retrievedUser.getEmail(),
                    "testEmail@localhost.localdomain");
            assertEquals("user type mismatch", UserType.OPERATOR,
                    retrievedUser.getType());
            server.deleteUser(newUserName, true);
            retrievedUser = server.getUser(newUserName);
            assertNull("Found deleted user: " + newUserName, retrievedUser);
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    @Test
    public void testRetrieveOperatorUser() {
        final String serviceUserName = "p4jtestoperatoruser";

        try {
            List<IUserSummary> users = server.getUsers(null,
                    new GetUsersOptions().setIncludeServiceUsers(false));
            assertNotNull("null user list returned", users);
            for (IUserSummary user : users) {
                assertNotNull("null user in user list", user);
                assertFalse("found operator user",
                        serviceUserName.equalsIgnoreCase(user.getLoginName()));
            }

            users = server.getUsers(null,
                    new GetUsersOptions().setIncludeServiceUsers(true));
            assertNotNull("null user list returned", users);
            boolean found = false;
            for (IUserSummary user : users) {
                assertNotNull("null user in user list", user);
                if (serviceUserName.equalsIgnoreCase(user.getLoginName())) {
                    found = true;
                }
            }
            assertTrue("operator user not found in list", found);
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
