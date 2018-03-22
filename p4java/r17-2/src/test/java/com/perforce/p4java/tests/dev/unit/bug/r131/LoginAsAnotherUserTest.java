package com.perforce.p4java.tests.dev.unit.bug.r131;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 login [-h<host>] user'.
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job059845" })
@TestId("Dev131_LoginAsAnotherUserTest")
public class LoginAsAnotherUserTest extends P4JavaTestCase {
	private IOptionsServer server = null;
	private String defaultTicketFile = null;
	private IUser anotherUser = null;
	private Properties serverProps;

	@BeforeEach
	public void setUp() {
		defaultTicketFile = Paths.get(System.getProperty("user.dir"), File.separator, ".p4tickets").toString();
		serverProps = new Properties();
		serverProps.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
	}

	@AfterEach
	public void tearDown() {
		if (nonNull(server)) {
			endServerSession(server);
		}
	}

	/**
	 * Test login another user: 'p4 login [-h<host>] user'.
	 */
	@Test
	public void testLoginAsAnotherUserExecMapCmd() throws Exception {

		String superUserName = "p4jtestsuper";
		String superPassword = "p4jtestsuper";

		String userName = "p4jtestuser2";
		String password = "p4jtestuser2";

		server = ServerFactory.getOptionsServer(this.serverUrlString, serverProps);
		assertThat(server, notNullValue());

		// Register callback
		server.registerCallback(createCommandCallback());

		// Connect to the server.
		server.connect();
		setUtf8CharsetIfServerSupportUnicode(server);

		// Set the user to the server
		server.setUserName(userName);

		// Login the user
		server.login(password, new LoginOptions());

		anotherUser = server.getUser(userName);
		assertThat(anotherUser, notNullValue());

		// Set the super user to the server
		server.setUserName(superUserName);

		// Login the super user
		server.login(superPassword, new LoginOptions());

		// Logout the super user.
		// This will produce an error when trying to login another user
		server.logout();

		Map<String, Object> results[] = null;

		// Login the specified "p4jtestuser2" user will fail, since the
		// super user is not logged in.
		results = server.execMapCmd("login", new String[] { userName }, null);
		assertThat(results, notNullValue());
		assertThat(results[0].toString(), containsString("Perforce password (P4PASSWD) invalid or unset"));

		// Login the super user
		server.login(superPassword, new LoginOptions());

		// Login the specified "p4jtestuser2" user will succeed, since the
		// super user is logged in.
		results = server.execMapCmd("login", new String[] { userName }, null);

		assertThat(results, notNullValue());
		assertThat(results[0].toString(), containsString("logged in"));
	}

	/**
	 * Test login another user: 'p4 login [-h<host>] user'.
	 */
	@Test
	public void testLoginAsAnotherUser() throws Exception {

		String superUserName = "p4jtestsuper";
		String superPassword = "p4jtestsuper";

		String userName = "p4jtestuser2";
		String password = "p4jtestuser2";

		server = ServerFactory.getOptionsServer(this.serverUrlString, serverProps);
		assertThat(server, notNullValue());

		// Register callback
		server.registerCallback(createCommandCallback());

		// Connect to the server.
		server.connect();
		setUtf8CharsetIfServerSupportUnicode(server);

		// Set the user to the server
		server.setUserName(userName);

		// Login the user
		server.login(password, new LoginOptions());

		anotherUser = server.getUser(userName);
		assertThat(anotherUser, notNullValue());

		// Set the super user to the server
		server.setUserName(superUserName);

		// Login the super user
		server.login(superPassword, new LoginOptions());
		// Logout the super user.
		// This will produce an error when trying to login another user
		server.logout();

		StringBuffer authTicketFromMemory = new StringBuffer();

		// Login the specified "p4jtestuser2" user will fail, since the
		// super user is not logged in.
		try {
			server.login(anotherUser, authTicketFromMemory,
					new LoginOptions().setHost(InetAddress.getLocalHost().getHostName()));
			fail("Should fail.");
		} catch (P4JavaException e) {
			assertThat(e.getLocalizedMessage(), notNullValue());
			assertThat(e.getLocalizedMessage(), containsString("Perforce password (P4PASSWD) invalid or unset"));
		}

		// Login the super user
		server.login(superPassword, new LoginOptions());

		// Login the specified "p4jtestuser2" user
		// The ticket should be written to the file
		// Also, the same ticket should be written to the StringBuffer
		server.login(anotherUser, authTicketFromMemory,
				new LoginOptions().setHost(InetAddress.getLocalHost().getHostName()));

		assertThat(authTicketFromMemory, notNullValue());
		assertThat(authTicketFromMemory.length() > 0, is(true));

		// Get the auth ticket after the login of the specified "p4jtestuser2"
		// user
		String authTicketFromFile = AuthTicketsHelper.getTicketValue(anotherUser.getLoginName(),
				server.getServerInfo().getServerAddress(), defaultTicketFile);

		// We should have a ticket for the specified "p4jtestuser2" user
		assertThat(authTicketFromFile, notNullValue());

		// The ticket in the StringBuffer should be the same as the ticket in
		// the file
		assertThat(authTicketFromMemory.toString(), is(authTicketFromFile));
	}
}
