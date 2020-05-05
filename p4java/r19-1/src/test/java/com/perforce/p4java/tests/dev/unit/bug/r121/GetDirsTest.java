/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 dirs' command.
 */
@Jobs({ "job050447" })
@TestId("Dev112_GetDirsTest")
public class GetDirsTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetDirsTest.class.getSimpleName());

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getSuperConnection(p4d.getRSHURL());
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
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
			this.endServerSession(server);
		}
	}

	/**
	 * Test 'p4 dirs' command.
	 */
	@Test
	public void testGetDirs() {

		try {
			List<IFileSpec> directories = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(new String[] {
							"//depot/101*", "//depot/112Dev/*" }),
					new GetDirectoriesOptions());
			assertNotNull(directories);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'p4 dirs' command with errors.
	 */
	@Test
	public void testGetDirsWithErrors() {

		try {
			List<IFileSpec> directories = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(new String[] {
							"//depotadfa/adf", "//depot/101*", "//depot/abc",
							"//adfadf/", "adsfasdf", "..." }),
					new GetDirectoriesOptions());
			assertNotNull(directories);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'p4 dirs' stream command, should return null if file not found.
	 */
	@Test
	@Ignore("Server team say this is no longer a valid test")
	public void tesDirsStreamCommand() {

		int randNum = getRandomInt();
		String nonExistingDepotDir = "//depot/non-existing-dir-" + randNum + "/*";

		String [] depotDirs = new String[] {"//depot/112Dev/GetOpenedFilesTest/src/com/perforce/*",
				nonExistingDepotDir };

		InputStream is = null;
		
		try {
			is = server.execStreamCmd("dirs", depotDirs);
			assertNotNull(is);
			int c;
			while ((c = is.read()) != -1) {
				System.out.print((char) c);
			}
			is.close();

			is = server.execStreamCmd("dirs", new String[] { nonExistingDepotDir });
			assertNull(is);

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Can't do anyting else here...
				}
			}
		}
	}
	
	/**
	 * Test "p4 dirs" streaming map command. Verify the values for the error message.
	 */
	@Test
	public void tesDirsStreamingMapCommand() {

		int randNum = getRandomInt();
		String depotDir = "//depot/baz2012xyz" + randNum + "/*";

		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			int key = this.getRandomInt();
			ListCallbackHandler handler = new ListCallbackHandler(this, key,
					resultsList);

			// We're turning off tagged output for the "dirs" command
			HashMap<String, Object> inMap = new HashMap<String, Object>();
			inMap.put(Server.IN_MAP_USE_TAGS_KEY, "no");
			
			server.execStreamingMapCommand("dirs", new String[] { depotDir },
					inMap, handler, key);

			assertTrue(resultsList.size() > 0);
			assertNotNull(resultsList.get(0));

			assertTrue(((String) resultsList.get(0).get("argc"))
					.contains(depotDir));
			assertTrue(((String) resultsList.get(0).get("fmt0"))
					.contains("[%argc% - no|No] such file(s)."));

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

	protected void fails(String msg) {
		fail(msg);
	}

}
