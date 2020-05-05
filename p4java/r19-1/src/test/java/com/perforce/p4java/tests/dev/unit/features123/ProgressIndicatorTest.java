/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
 * Test progress indicator (p4 -I <command>). Currently, only certain Perforce
 * commands have progress indicator support. And, it only works with Perforce
 * server 12.2 or above.
 */
@Jobs({ "job057603" })
@TestId("Dev123_ProgressIndicatorTest")
public class ProgressIndicatorTest extends P4JavaTestCase {

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
		try {
			Properties props = new Properties();

			props.put("enableProgress", "true");

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
	 * Test progress indicator (p4 -I <command>) using execMapCmd().
	 */
	@Test
	public void testProgressIndicatorMapCmd() {

		try {
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.SYNC.toString(), new String[] { "-q", "-f",
							"//depot/112Dev/CopyFilesTest/..." }, null);
			assertNotNull(result);
			assertTrue(result.length > 0);
			
			assertNotNull(result[result.length-1]);
			assertTrue(result[result.length-1].containsKey("handle"));
			assertNotNull(result[result.length-1].get("handle"));
			assertEquals((String)result[result.length-1].get("handle"), "progress");

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test progress indicator (p4 -I <command>) using execStreamCmd().
	 * 
	 * test "p4 -I sync -q"
	 */
	@Test
	public void testProgressIndicatorStreamCmd() {

		InputStream is = null;
		try {
			is = server.execStreamCmd(CmdSpec.SYNC.toString(), new String[] {
					"-q", "-f", "//depot/112Dev/CopyFilesTest/..." });
			assertNotNull(is);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			// Capture lines
			List<String> lines = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();

			// Should contain at least one line
			assertTrue(lines.size() > 0);

			assertNotNull(lines.get(lines.size()-1));
			assertTrue(lines.get(lines.size()-1).contains("finishing"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Can't do anything else here...
				}
			}
		}
	}
}
