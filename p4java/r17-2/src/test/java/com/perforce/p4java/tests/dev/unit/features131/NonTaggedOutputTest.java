/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test non-tagged output for the 'spec -o' commands.
 * 
 * <pre>
 * 'p4 client -o'
 * 'p4 job -o'
 * 'p4 label -o'
 * 'p4 branch -o'
 * 'p4 change -o'
 * 'p4 user -o'
 * 'p4 depot -o'
 * 'p4 protect -o'
 * </pre>
 */
@Jobs({ "job061531" })
@TestId("Dev131_NonTaggedOutputTest")
public class NonTaggedOutputTest extends P4JavaTestCase {

	private static IClient client = null;
	private static String serverMessage = null;
	private static long completedTime = 0;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// initialization code (before each test).
		try {
			Properties props = new Properties();
			//props.put("quietMode", "true");

			server = ServerFactory
					.getOptionsServer(serverUrlString, props);
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
			server.setUserName(superUserName);

			// Login using the normal method
			server.login(superUserPassword, new LoginOptions());

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
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
	}

	/**
	 * Test non-tagged output for the 'client -o <client name>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedClientOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.CLIENT.toString(), new String[] { "-o", "p4TestUserWS20112"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce Client Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'label -o <label name>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedLabelOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.LABEL.toString(), new String[] { "-o", "LabelSyncTestLabel"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce Label Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'job -o <job id>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedJobOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.JOB.toString(), new String[] { "-o", "job000012"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce Job Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'branch -o <branch name>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedBranchOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.BRANCH.toString(), new String[] { "-o", "test-branch"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce Branch Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'change -o <change id>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedChangeOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.CHANGE.toString(), new String[] { "-o", "8"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce Change Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'user -o <user name>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedUserOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.USER.toString(), new String[] { "-o", "p4jtestuser"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce User Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'depot -o <depot name>' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedDepotOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.DEPOT.toString(), new String[] { "-o", "depot"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("A Perforce Depot Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'protect -o' command using execMapCmd().
	 */
	@Test
	public void testNonTaggedProtectOutput() {

		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			Map<String, Object>[] result = server.execMapCmd(
					CmdSpec.PROTECT.toString(), new String[] { "-o"}, inMap);
			assertNotNull(result);
			assertTrue(result.length > 0);
			assertNotNull(result[0]);
			assertNotNull(result[0].get("data"));
			assertTrue(((String)result[0].get("data")).contains("Perforce Protections Specification"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test non-tagged output for the 'protect -o' command using execStreamCmd().
	 */
	@Test
	public void testNonTaggedProtectStreamCmdOutput() {

		InputStream is = null;
		try {
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put("useTags", "no");
			is = server.execStreamCmd(CmdSpec.PROTECT.toString(), new String[] {"-o"}, inMap);
			assertNotNull(is);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append(LINE_SEPARATOR);
			}
			br.close();
			assertTrue(sb.toString().contains("Perforce Protections Specification"));

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
