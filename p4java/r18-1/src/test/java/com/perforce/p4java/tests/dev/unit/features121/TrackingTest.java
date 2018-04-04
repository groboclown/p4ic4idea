/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test tracking ("-Ztrack")
 */
@Jobs({ "job041786" })
@TestId("Dev121_TrackingTest")
public class TrackingTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

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
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
	}

	/**
	 * Test tracking ("-Ztrack")
	 */
	@Test
	public void testTracking() {

		try {
			Properties props = new Properties();

			props.put("enableTracking", "true");

			server = ServerFactory
					.getOptionsServer(this.serverUrlString, props);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					completedTime = millisecsTaken;
				}
			});
			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set the server user
			server.setUserName(this.userName);

			// Login using the normal method
			server.login(this.password, new LoginOptions());

			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);

			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.PRINT.toString(), new String[] {"//depot/basic/readonly/grep/P4CmdGetter.java"}, null);
			assertNotNull(result);

			// Verifying the result
			Map<String, Object> lastResult = result[result.length - 1];
			assertNotNull(lastResult);
			
			assertNotNull(lastResult.get("data"));
			assertTrue(((String)lastResult.get("data")).contains("db.user"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
