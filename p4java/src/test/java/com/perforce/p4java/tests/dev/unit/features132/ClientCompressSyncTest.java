/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features132;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test P4Java sync using JZlib with client compression mode.
 */
@Jobs({ "job066779" })
@TestId("Dev132_ClientCompressSyncTest")
public class ClientCompressSyncTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", ClientCompressSyncTest.class.getSimpleName());

	String serverMessage = null;
	long completedTime = 0;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			final String depotName = this.getRandomName(false, "p4TestUserWSCompress-depot");
			final String clientName = "p4TestUserWSCompress";
			final String clientDescription = "temp stream client for test";
			final String clientRoot = Paths.get("").toAbsolutePath().toString();
			final String[] clientViews = {"//" + depotName + "/... //" + clientName + "/..."};

			Properties props = new Properties();
			props.put("sockPerfPrefs", "3, 2, 1");
			props.put("sockSoTimeout", 0);

			setupServer(p4d.getRSHURL(), userName, password,true, props);
			setupUtf8(server);
			createLocalDepot(depotName, server);

			client = createClient(server, clientName, clientDescription, clientRoot, clientViews);
		} catch (Exception e) {
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
			endServerSession(server);
		}
	}

	/**
	 * Test "p4 sync" using Process.exec("p4 sync")
	 */
	@Test
	@Ignore("Seems pointless to run this in shell immediately before the same in java")
	public void testCmdSync() {

		try {
			String[] command = new String[] {"/bin/sh", "-c", "/opt/perforce/p4/p4 -p"
					+ server.getServerInfo().getServerAddress() + " -u"
					+ userName + " -P" + server.getAuthTicket() + " -c"
					+ client.getName() + " sync -f //depot/..."};
			
		   ProcessBuilder builder = new ProcessBuilder(command);

		    final Process process = builder.start();
		    process.waitFor();
		    System.out.println("Program terminated!");
			    
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (ConnectionException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (InterruptedException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
	}

	/**
	 * Test "p4 sync" using execMapCmd
	 */
	@Test
	public void testExecMapCmdSync() {

		try {
			/** Limited to /depot/basic/... to reduce the memory load. */
			List<Map<String, Object>> result = server.execMapCmdList(
					CmdSpec.SYNC.toString(), new String[] { "-f",
							"//depot/basic/..." }, null);
			assertNotNull(result);
			assertTrue(result.size() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
