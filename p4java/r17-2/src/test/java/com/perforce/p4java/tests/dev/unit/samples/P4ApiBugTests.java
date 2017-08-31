/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.samples;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 password" functionality.
 */
@Jobs({ "job000000" })
@TestId("P4ApiBugTests")
public class P4ApiBugTests extends P4JavaTestCase {

	private static String pass;
	private static String user;
	private File ticketFile;
	private final String P4ServerPort_2010_2_322263 = "eng-p4java-vm.perforce.com:20102";
	private final String P4ServerPort_2009_2_273932 = "eng-p4java-vm.perforce.com:20092";
	private static Properties serverProps;
	private static String p4Tickets;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		p4Tickets = Paths.get(System.getProperty("user.dir"),
					File.separator, ".p4tickets").toString();
		serverProps = new Properties();
		serverProps.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, p4Tickets);
		user = "p4jtestuser2";
		pass = "p4jtestuser2";
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		ticketFile = new File(p4Tickets);
		if (ticketFile.exists()) {
			ticketFile.delete();
		}
		assertFalse(ticketFile.exists());
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
	}

	@Test
	@Ignore("P4ServerPort_2009_2_273932 does not exist")
	public void testLogin_withTicketFile_2009_2_273932() throws Exception {
		login(P4ServerPort_2009_2_273932, true);
	}

	@Test
	public void testLogin_withTicketFile_2010_2_322263() throws Exception {
		login(P4ServerPort_2010_2_322263, true);
	}

	@Test
	@Ignore("P4ServerPort_2009_2_273932 does not exist")
	public void testLogin_withoutTicketFile_2009_2_273932() throws Exception {
		login(P4ServerPort_2009_2_273932, false);
	}

	@Test
	public void testLogin_withoutTicketFile_2010_2_322263() throws Exception {
		login(P4ServerPort_2010_2_322263, false);
	}

	@Test
	@Ignore("P4ServerPort_2009_2_273932 does not exist")
	public void testGetLoginStatus_withTicketFile_2009_2_273932()
			throws Exception {
		testGetLoginStatus(P4ServerPort_2009_2_273932, true);
	}

	@Test
	public void testGetLoginStatus_withTicketFile_2010_2_322263()
			throws Exception {
		testGetLoginStatus(P4ServerPort_2010_2_322263, true);
	}

	@Test
	@Ignore("P4ServerPort_2009_2_273932 does not exist")
	public void testGetLoginStatus_withoutTicketFile_2009_2_273932()
			throws Exception {
		testGetLoginStatus(P4ServerPort_2009_2_273932, false);
	}

	@Test
	public void testGetLoginStatus_withoutTicketFile_2010_2_322263()
			throws Exception {
		testGetLoginStatus(P4ServerPort_2010_2_322263, false);
	}

	private IServer login(String serverPort, boolean useTicketFile)
			throws P4JavaException, URISyntaxException {
		IServer server = ServerFactory
				.getServer("p4java://" + serverPort, serverProps);
		assertFalse(ticketFile.exists());
		if (useTicketFile) {
			loginWithTicketFile(server);
		} else {
			loginWithoutTicketFile(server);
		}
		return server;
	}

	private void testGetLoginStatus(String serverPort, boolean useTicketFile)
			throws Exception {
		IServer server = login(serverPort, useTicketFile);
		String loginStatus = server.getLoginStatus();
		assertTrue("Login Status does not contain 'expires': '" + loginStatus
				+ "'", loginStatus.contains("expires"));
	}

	private void loginWithTicketFile(IServer server) throws P4JavaException {
		server.setUserName(user);
		server.connect();
		server.login(pass);
		assertTrue("Ticket file does not exist after login with ticket file!",
				ticketFile.exists());
	}

	private String loginWithoutTicketFile(IServer server)
			throws P4JavaException {
		String ticket = null;
		server.setUserName(user);
		server.connect();
		String[] argp = new String[] { "-p" };
		Map<String, Object> passMap = new HashMap<String, Object>();
		passMap.put("password", pass);

		Map<String, Object>[] result = server
				.execMapCmd("login", argp, passMap);
		boolean ticketExists = ticketFile.exists();
		assertFalse(String.format("ResultLength: %s, TicketFileExists: %s",
				result.length, ticketExists), result == null
				|| result.length == 0 || ticketExists);
		Map<String, Object> firstResult = result[0];

		if ((ticket = (String) firstResult.get("ticket")) == null) {
			throw new P4JavaException(String.format("login failed: %s",
					firstResult.toString()));
		}
		server.setAuthTicket(ticket);
		return ticket;
	}
}
