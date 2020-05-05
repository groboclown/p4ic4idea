/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import com.perforce.p4java.core.IFix;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Simple login / logout tests based on the new Options server.
 */

@TestId("Dev101_LoginLogoutTest")
public class LoginLogoutTest extends P4JavaRshTestCase {

	@Before
	public void setUp() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, props);
	}
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LoginLogoutTest.class.getSimpleName());

	@Test
	public void testLoginLogout() {
		final String testUserName = userName;
		final String testUserPassword = password;
		
		try {
			// First just test options setter chaining:
			assertEquals(true, new LoginOptions().setAllHosts(true).isAllHosts());
			server.connect();
			server.setUserName(invalidUserName);
			@SuppressWarnings("unused")
			List<IFix> fixes = null;
			try {
				 fixes = server.getFixes(null, new GetFixesOptions());
				 fail("access allowed for invalid user");
			} catch (AccessException aexc) {
			}
			try {
			    server.setUserName(testUserName);
			    server.login(this.invalidUserPassword, null);
				fail("access allowed with bad password");
			} catch (AccessException aex) {
			}
			try {
			    server.login(testUserPassword, new LoginOptions("-z"));
				fail("bad login option accepted");
			} catch (RequestException aexc) {
				assertEquals(1, aexc.getGenericCode());
				assertEquals(3, aexc.getSeverityCode());
			}
			try {
				server.setUserName(testUserName);
				server.login(testUserPassword, null);
			} catch (RequestException aexc) {
				fail("login failed for valid login / password pair");
			}
			try {
				 fixes = server.getFixes(null, new GetFixesOptions());
			} catch (AccessException aexc) {
				fail("'fixes' access disallowed for valid user");
			}
			server.logout(null);
			try {
				 fixes = server.getFixes(null, new GetFixesOptions());
				 fail("'fixes' access allowed for logged-out user");
			} catch (AccessException aexc) {
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
