package com.perforce.p4java.tests.qa;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AuthenticationTest extends P4JavaRshTestCase {

	private static final String SECOND_USER_NAME = "secondUser";
	private static final String SECOND_USER_PASSWORD = "thispasswordisoversixteencharacterslong";

	private static Helper helper = null;

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r18.1", AuthenticationTest.class.getSimpleName());

	@BeforeClass
	public static void before() throws Throwable {
		setupServer(p4d.getRSHURL(), superUserName, superUserPassword, false, null);

		// clear protections
		List<IProtectionEntry> protects = new ArrayList<>();
		server.updateProtectionEntries(protects);

		// set super password
		//server.changePassword("", superUserPassword, superUserName);
		//server.login(superUserPassword);

		helper = new Helper();

		IUser user = server.getUser(superUserName);

		IClient client = helper.createClient(server, "client1");
		server.setCurrentClient(client);

		File testFile = new File(client.getRoot(), "foo.txt");
		File test2File = new File(client.getRoot(), "bar.txt");

		helper.addFile(server, user, client, testFile.getAbsolutePath(), "FileSpecTest", "text");
		helper.addFile(server, user, client, test2File.getAbsolutePath(), "FileSpecTest", "text");

		IUserGroup group = new UserGroup();
		List<String> users = new ArrayList<String>();
		// create a group and add some other user
		users.add(SECOND_USER_NAME);
		group.setName("timeoutnow");
		group.setTimeout(2);
		group.setUsers(users);

		UpdateUserGroupOptions opt = new UpdateUserGroupOptions();
		server.createUserGroup(group, opt);

		helper.createUser(server, SECOND_USER_NAME, SECOND_USER_PASSWORD);
		server.login(SECOND_USER_PASSWORD);
		server.logout();
	}

	@Before
	public void Setup() throws Exception {
		server.setUserName(userName);
		server.login(password);
		server.logout();
	}

	/**
	 * verify that we get an access exception if we log out or our ticket expires
	 * streaming commands in particular displayed this issue
	 * we should get an exception if everything is working correctly
	 */
	@Test
	public void exceptionAfterLoggingOut() throws Throwable {
		InputStream diffStream = null;
		try {
			diffStream = server.getServerFileDiffs(new FileSpec("//depot/foo.txt"), new FileSpec("//depot/bar.txt"),
					null, null, false, false, false);
			assertThat(diffStream, notNullValue());
			// we shouldn't get here
			fail("Did not get access exception");
		} catch (AccessException a) {
			assertTrue("Should have been unset", a.getMessage().startsWith("Perforce password (P4PASSWD) invalid or unset."));
		} finally {
			try {
				diffStream.close();
			} catch (Throwable e) {
			}
		}
	}

	/**
	 * make sure a timed out connection works
	 *
	 * @throws Throwable
	 */
	@Test
	public void exceptionAfterTimeout() throws Throwable {
		InputStream diffStream = null;
		try {
			server.setUserName(SECOND_USER_NAME);
			server.login(SECOND_USER_PASSWORD);
			assertThat("Was not secondUser", server.getUserName(), is(SECOND_USER_NAME));
			TimeUnit.SECONDS.sleep(3);
			diffStream = server.getServerFileDiffs(new FileSpec("//depot/foo.txt"), new FileSpec("//depot/bar.txt"),
					null, null, false, false, false);
			assertThat(diffStream, notNullValue());
			// we shouldn't get here
			fail("Did not get access exception");
		} catch (AccessException a) {
			assertTrue("Should have expired.", a.getMessage().startsWith("Your session has expired, please login again."));
		} finally {
			try {
				diffStream.close();
			} catch (Throwable e) {
			}
		}
	}

	/**
	 * verify that we can get a valid ticket for all machines
	 *
	 * @throws Throwable
	 */
	@Test
	public void globalLogin() throws Throwable {
		server.login(password, true);

		List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertThat(changes, notNullValue());
		assertThat("wrong number of changes", changes.size(), is(1));
	}

	/**
	 * verify that we can have a password with a '#'
	 *
	 * @throws Throwable
	 */
	@Test
	public void changePassword() throws Throwable {
		helper.createUser(server, "thirdUser", "foobar");
		server.setUserName("thirdUser");

		server.changePassword("foobar", "foo#bar", null);
		server.login("foo#bar", true);

		List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertThat(changes, notNullValue());
		assertThat("wrong number of changes", changes.size(), is(1));
	}

	/**
	 * create a password with the changePassword method
	 *
	 * @throws Throwable
	 */
	@Test
	public void newPasswordWithChangePassword() throws Throwable {
		helper.createUser(server, "fourthUser", null);
		server.setUserName("fourthUser");

		server.changePassword("", "foo#bar", null);
		server.login("foo#bar", true);

		List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertThat(changes, notNullValue());
		assertThat("wrong number of changes", changes.size(), is(1));
	}

	/**
	 * change to an overly long password
	 */
	@Test
	public void changeToLongPassword() throws Throwable {
		helper.createUser(server, "fifthUser", "short");
		server.setUserName("fifthUser");

		server.changePassword("short", "verylongverylongverylong", null);
		server.login("verylongverylongverylong", true);

		List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertThat(changes, notNullValue());
		assertThat("wrong number of changes", changes.size(), is(1));
	}

	/**
	 * verify job049985: should get access exception from bad login
	 *
	 * @throws Throwable
	 */
	@Test
	public void wrongPassword() throws Throwable {
		try {
			server.login("bad_password");
			fail("should have thrown exception");
		} catch (AccessException ae) {
			assertThat("incorrect error message", ae.getLocalizedMessage(), containsString("Password invalid."));
		}
	}

	/**
	 * verify job047563: verify support of the -p flag
	 *
	 * @throws Throwable
	 */
	@Test
	public void displayTicket() throws Throwable {
		LoginOptions opts = new LoginOptions().setDontWriteTicket(true);
		StringBuffer ticket = new StringBuffer();
		ticket.append("password=");
		server.login(password, ticket, opts);

		assertTrue("option not set", opts.isDontWriteTicket());
		assertTrue("no ticket returned", ticket.length() > 10);
		assertTrue("was not appended", ticket.toString().startsWith("password="));

		// make sure we are still logged in
		List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertThat(changes, notNullValue());
		assertThat("wrong number of changes", changes.size(), is(1));
	}

	/**
	 * verify job046826: verify support for logging in others
	 *
	 * @throws Throwable
	 */
	@Test
	public void loginOther() throws Throwable {
		// generate ticket first
		String sixthUser = "sixthUser";
		helper.createUser(server, sixthUser, SECOND_USER_PASSWORD);
		server.setUserName(sixthUser);
		server.login(SECOND_USER_PASSWORD);
		server.logout();
		// login
		server.setUserName(userName);
		server.login(password);
		List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertNotNull(changes);
		assertThat("wrong number of changes", changes.size(), is(1));

		IUser other = server.getUser(sixthUser);
		server.login(other, null, null);

		// make sure we are still logged in
		server.setUserName(sixthUser);
		String userServerAuthTicket = server.getAuthTicket(sixthUser);
		server.setAuthTicket(userServerAuthTicket);
		changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/basic/..."), null);
		assertThat(changes, notNullValue());
		assertThat("wrong number of changes", changes.size(), is(1));
	}
}
