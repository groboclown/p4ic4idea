/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features101;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Attempts to test the IServer.getLoginStatus method.
 */

@TestId("Features101_GetLoginStatusTest")
public class GetLoginStatusTest extends P4JavaTestCase {

	public GetLoginStatusTest() {
		super();
	}

	/**
	 * Tests string return for validly logged-in user.
	 */
	@Test
	public void testLoginStatusSuccess() {
		
		IServer server = null;

		try {
			server = getServer();			
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
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	/**
	 * Tests string return for invalid user. This user should not even have
	 * access to the server.
	 */
	@Test
	public void testLoginStatusFail() {
		IServer server = null;

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			server.connect();
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
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	/**
	 * Test for a non-logged-in user. May occasionally fail if someone's
	 * actually logged in as this user...
	 */
	
	@Test
	public void testNonLoggedInUser() {
		IServer server = null;

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			server.connect();
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
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
