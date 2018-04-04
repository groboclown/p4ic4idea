/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IFix;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple login / logout tests based on the new Options server.
 */

@TestId("Dev101_LoginLogoutTest")
public class LoginLogoutTest extends P4JavaTestCase {

	public LoginLogoutTest() {
	}

	@Test
	public void testLoginLogout() {
		final String testUserName = userName;
		final String testUserPassword = password;
		
		try {
			// First just test options setter chaining:
			assertEquals(true, new LoginOptions().setAllHosts(true).isAllHosts());
			IOptionsServer optsServer = ServerFactory.getOptionsServer(
											this.serverUrlString, null);
			assertNotNull(optsServer);
			optsServer.connect();
			optsServer.setUserName(invalidUserName);
			@SuppressWarnings("unused")
			List<IFix> fixes = null;
			try {
				 fixes = optsServer.getFixes(null, new GetFixesOptions());
				 fail("access allowed for invalid user");
			} catch (AccessException aexc) {
			}
			try {
				optsServer.setUserName(testUserName);
				optsServer.login(this.invalidUserPassword, null);
				fail("access allowed with bad password");
			} catch (AccessException aex) {
			}
			try {
				optsServer.login(testUserPassword, new LoginOptions("-z"));
				fail("bad login option accepted");
			} catch (RequestException aexc) {
				assertEquals(1, aexc.getGenericCode());
				assertEquals(3, aexc.getSeverityCode());
			}
			try {
				optsServer.setUserName(testUserName);
				optsServer.login(testUserPassword, null);
			} catch (RequestException aexc) {
				fail("login failed for valid login / password pair");
			}
			try {
				 fixes = optsServer.getFixes(null, new GetFixesOptions());
			} catch (AccessException aexc) {
				fail("'fixes' access disallowed for valid user");
			}
			optsServer.logout(null);
			try {
				 fixes = optsServer.getFixes(null, new GetFixesOptions());
				 fail("'fixes' access allowed for logged-out user");
			} catch (AccessException aexc) {
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
