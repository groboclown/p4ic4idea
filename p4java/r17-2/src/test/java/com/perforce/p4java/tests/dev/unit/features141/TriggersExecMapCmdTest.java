/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

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
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test triggers using the raw execMapCmd() method.
 */
@Jobs({ "job071702" })
@TestId("Dev141_TriggersExecMapCmdTest")
public class TriggersExecMapCmdTest extends P4JavaTestCase {

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
		try {
			Properties properties = new Properties();
			properties.put("relaxCmdNameChecks", "true");
			
			server = ServerFactory.getOptionsServer(this.serverUrlString, properties);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
				}

				public void receivedServerInfoLine(int key, String infoLine) {
				}

				public void receivedServerErrorLine(int key, String errorLine) {
				}

				public void issuingServerCommand(int key, String command) {
				}

				public void completedServerCommand(int key, long millisecsTaken) {
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
			server.setUserName(this.getSuperUserName());

			// Login using the normal method
			server.login(this.getSuperUserPassword(), new LoginOptions());

			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}
	
	/**
	 * Test triggers command using input string cmd.
	 */
	@Test
	public void testTriggersInputStringCmd() {
		
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("Triggers:").append("\n");
			sb.append("\t").append("example1 change-submit //depot/... \"echo %changelist%\"").append("\n");
			sb.append("\t").append("example2 change-submit //depot/... \"echo %changelist%\"").append("\n");
			sb.append("\t").append("example3 change-submit //depot/... \"echo %changelist%\"");
			
			Map<String, Object>[] resultMaps = server.execInputStringMapCmd("triggers", new String[] {"-i"}, sb.toString());
			assertNotNull(resultMaps);
			assertTrue(resultMaps.length > 0);
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
