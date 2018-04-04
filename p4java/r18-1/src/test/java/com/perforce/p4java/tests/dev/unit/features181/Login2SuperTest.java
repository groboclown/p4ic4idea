package com.perforce.p4java.tests.dev.unit.features181;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.Login2Options;
import com.perforce.p4java.tests.MFAServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Login2SuperTest extends P4JavaRshTestCase {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@ClassRule
	public static MFAServerRule p4d = new MFAServerRule("r18.1", Login2SuperTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
	}

	@Test
	public void loginAnotherUser() throws P4JavaException {

		IUser joe = server.getUser("joe");
		String msg1 = server.getLogin2Status(joe);
		assertNotNull(msg1);
		assertEquals("User joe on host unknown: required\n", msg1);

		Login2Options opts = new Login2Options();
		String msg2 = server.login2(joe, opts);
		assertNotNull(msg2);
		assertEquals("Second factor authentication approved for user joe.", msg2);

		String msg3 = server.getLogin2Status(joe);
		assertNotNull(msg3);
		assertEquals("User joe on host unknown: validated forced\n", msg3);
	}
}
