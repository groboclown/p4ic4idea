/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
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
 * Test automatic charset configuration when talking to an Unicode enabled Perforce server.
 */
@Jobs({ "job072273" })
@TestId("Dev141_AutoCharsetUnicodeServerTest")
public class AutoCharsetUnicodeServerTest extends P4JavaTestCase {

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
	 * Test automatic charset configuration when talking to an Unicode enabled Perforce server.
	 */
	@Test
	public void testAutoCharset() {

		// Unicode enabled Perforce server
		String unicodeServerUrl = "p4java://eng-p4java-vm.perforce.com:30132";
		
		IOptionsServer server = null;
		IClient client = null;

		try {
			Properties properties = new Properties();
			properties.put("relaxCmdNameChecks", "true");
			
			server = ServerFactory.getOptionsServer(unicodeServerUrl, properties);
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

			// Set the charset before connecting
			server.setCharsetName("iso8859-1");
			
			// Connect to the server.
			server.connect();

			// Check the charset after connecting
			assertNotNull(server.getCharsetName());
			assertEquals(server.getCharsetName(), "iso8859-1");
			System.out.println(server.getCharsetName());

			server.setUserName(this.getSuperUserName());
			server.login(this.getSuperUserPassword(), new LoginOptions());
			
			IClient testClient = getDefaultClient(server);
			assertNotNull(testClient);
			
			// Disconnect
			server.disconnect();

			// Set charset to "no charset"
			server.setCharsetName(null);
			
			// The charset should be null
			assertNull(server.getCharsetName());

			// Connect again
			server.connect();
			
			// The charset should be automatically set
			assertNotNull(server.getCharsetName());
			System.out.println(server.getCharsetName());
			
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
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
