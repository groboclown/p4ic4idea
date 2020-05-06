/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features101;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import com.perforce.p4java.server.IOptionsServer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features121.GetStreamOptionsTest;

/**
 * Attempts to test the IServer.getLoginStatus method.
 */

@TestId("Features101_GetLoginStatusTest")
public class GetLoginStatusTest extends P4JavaRshTestCase {

	public GetLoginStatusTest() {
		super();
	}
	
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetLoginStatusTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            Properties properties = new Properties();
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
            assertNotNull(server);
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
	 * Tests string return for validly logged-in user.
	 */
	@Test
	public void testLoginStatusSuccess() {
		try {
			String statusString = server.getLoginStatus();
			String expectedValue = "User " + this.userName + " ticket expires in";
			assertNotNull("null login status string returned from IServer.getLoginStatus",
						statusString);
			assertTrue("returned status string '" + statusString + "' wrong for logged-in user;"
					+ " expected string containing: '"
					+ expectedValue + "'...",
					statusString.contains(expectedValue));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} 
	}
	
	/**
	 * Tests string return for invalid user. This user should not even have
	 * access to the server.
	 */
	@Test
	public void testLoginStatusFail() {
		try {
			server.setUserName(this.invalidUserName);	
			String statusString = server.getLoginStatus();
			String expectedValue = "Access for user '" + this.invalidUserName + "' has not been enabled";
			assertNotNull("null login status string returned from IServer.getLoginStatus",
						statusString);
			assertTrue("returned status string '" + statusString + "' wrong for logged-in user;"
					+ " expected string containing: '"
					+ expectedValue + "'...",
					statusString.contains(expectedValue));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} 
	}
	
	/**
	 * Test for a non-logged-in user. May occasionally fail if someone's
	 * actually logged in as this user...
	 */
	
	@Test
	public void testNonLoggedInUser() {
		try {  
			server.setUserName(this.noLoginUser);			
			String statusString = server.getLoginStatus();
			String expectedValue = "'login' not necessary, no password set for this user.";
			assertNotNull("null login status string returned from IServer.getLoginStatus",
						statusString);
			assertTrue("returned status string '" + statusString + "' wrong for logged-in user;"
					+ " expected string containing: '"
					+ expectedValue + "'...",
					statusString.contains(expectedValue));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} 
	}
}
