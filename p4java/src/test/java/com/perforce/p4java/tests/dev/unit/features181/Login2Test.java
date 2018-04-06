package com.perforce.p4java.tests.dev.unit.features181;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.MFAServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;
import java.util.Properties;

import static com.perforce.p4java.tests.ServerMessageMatcher.isText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class Login2Test extends P4JavaRshTestCase {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@ClassRule
	public static MFAServerRule p4d = new MFAServerRule("r18.1", Login2Test.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		server = ServerFactory.getOptionsServer(p4d.getRSHURL(), properties);
		Assert.assertNotNull(server);
		server.connect();

		server.setUserName("joe");
	}

	@Test
	public void defaultLogin() throws P4JavaException {
		LoginOptions outOpts = new LoginOptions();
		outOpts.setTwoFactor(true);
		server.logout(outOpts);

		server.login("Password", new LoginOptions());

		IServerMessage msg1 = server.getLogin2Status();
		assertNotNull(msg1);
		assertThat(msg1, isText("User joe on host unknown: required\n"));

		// p4 login2 -S list-methods
		Map<String, String> list = server.login2ListMethods();
		assertNotNull(list);
		assertTrue(list.containsKey("GAuth"));
		assertEquals("Google Authenticator generated OTP", list.get("GAuth"));

		// p4 login2 -S init-auth -m GAuth
		String msg2 = server.login2InitAuth("GAuth");
		assertNotNull(msg2);
		assertEquals("Need value from GAuth!", msg2);

		// p4 login2 -S check-auth
		String msg3 = server.login2CheckAuth("123456", false);
		assertNotNull(msg3);
		assertEquals("GAuth says yes!\nSecond factor authentication approved.", msg3);

		IServerMessage msg4 = server.getLogin2Status();
		assertNotNull(msg4);
		assertThat(msg4, isText("User joe on host unknown: validated\n"));
	}

	@Test
	public void persistLogin() throws P4JavaException {
		LoginOptions outOpts = new LoginOptions();
		outOpts.setTwoFactor(true);
		server.logout(outOpts);

		server.login("Password", new LoginOptions());

		IServerMessage msg1 = server.getLogin2Status();
		assertNotNull(msg1);
		assertThat(msg1, isText("User joe on host unknown: required\n"));

		// p4 login2 -S list-methods
		Map<String, String> list = server.login2ListMethods();
		assertNotNull(list);
		assertTrue(list.containsKey("GAuth"));
		assertEquals("Google Authenticator generated OTP", list.get("GAuth"));

		// p4 login2 -S init-auth -m GAuth
		String msg2 = server.login2InitAuth("GAuth");
		assertNotNull(msg2);
		assertEquals("Need value from GAuth!", msg2);

		// p4 login2 -S check-auth
		String msg3 = server.login2CheckAuth("123456", true);
		assertNotNull(msg3);
		assertEquals("GAuth says yes!\nSecond factor authentication approved.", msg3);

		IServerMessage msg4 = server.getLogin2Status();
		assertNotNull(msg4);
		assertThat(msg4, isText("User joe on host unknown: validated persistent\n"));
	}
}