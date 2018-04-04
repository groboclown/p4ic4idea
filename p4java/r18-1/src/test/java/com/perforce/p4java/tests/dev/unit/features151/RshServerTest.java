/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features151;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'rsh' mode server.
 */
@Jobs({ "job034706" })
@TestId("Dev151_RshServerTest")
public class RshServerTest extends P4JavaTestCase {

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
		File files = new File("/tmp/rsh-root");
		files.mkdirs();
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		try {
			if (server != null) {
				this.endServerSession(server);
			}
		} catch (AssertionError e) {
			debugPrint("Could not end session " + e.getMessage());
		}
	}

	/**
	 * Test 'rsh' mode server.
	 */
	@Test
	public void testRshServer() {

		String rshCmdUri = "p4jrsh:///usr/local/bin/p4d -r /tmp/rsh-root -i --java -vserver=5 -vrpc=5 -vnet=5 -L/tmp/rsh-root/log";
		
		try {
			// Connect to a replica server
			server = ServerFactory.getOptionsServer(rshCmdUri, null);
			assertNotNull(server);
			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Get server info
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);

			System.out.println("Server Version: " + serverInfo.getServerVersion());
			System.out.println("Server Root: " + serverInfo.getServerRoot());
			
			final String userName = "p4jtest" + this.getRandomInt();
			final String fullName = "Test user created by junit test " + this.getTestId();
			final String userPassword = null;
			final String expectedStatus = "User " + userName + " saved.";
            // test that read and write operations work
			IUser user = User.newUser(userName, "blah@blah.blah", fullName, userPassword);
			assertNotNull(user);
			String createStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			assertNotNull("null status string from createUser", createStr);
			assertEquals("user not created on server: " + createStr, expectedStatus, createStr);

			System.out.println(createStr);
			
			IUser user2 = server.getUser(userName);
			assertNotNull(user2);
			
		} catch (ConnectionException e) {
			debugPrint("Skipped test, unable to connect to p4jrsh:///usr/local/bin/p4d");
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'rsh' mode server using non-thread-safe P4Java.
	 */
	@Test
	public void testRshNts() {

		String rshCmdUri = "p4jrshnts:///usr/local/bin/p4d -r /tmp/rsh-root -i --java -vserver=5 -vrpc=5 -vnet=5 -L/tmp/rsh-root/log";

		try {
			// Connect to a replica server
			server = ServerFactory.getOptionsServer(rshCmdUri, null);
			assertNotNull(server);

			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Get server info
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);

			System.out.println("Server Version: " + serverInfo.getServerVersion());
			System.out.println("Server Root: " + serverInfo.getServerRoot());
			
			// test that read and write operations work
			final String userName = "p4jtest" + this.getRandomInt();
			final String fullName = "Test user created by junit test " + this.getTestId();
			final String userPassword = null;
			final String expectedStatus = "User " + userName + " saved.";

			IUser user = User.newUser(userName, "blah@blah.blah", fullName, userPassword);
			assertNotNull(user);
			String createStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
			assertNotNull("null status string from createUser", createStr);
			assertEquals("user not created on server: " + createStr, expectedStatus, createStr);

			System.out.println(createStr);
			
			IUser user2 = server.getUser(userName);
			assertNotNull(user2);
			
		} catch (ConnectionException e) {
			debugPrint("Skipped test, unable to connect to p4jrshnts:///usr/local/bin/p4d");
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
