/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 dirs' command.
 */
@Jobs({ "job050447" })
@TestId("Dev112_GetDirsTest")
public class GetDirsTest extends P4JavaTestCase {

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
			server = getServerAsSuper();
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
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

	public static class SimpleCallbackHandler implements IStreamingCallback {
		int expectedKey = 0;
		GetDirsTest testCase = null;

		public SimpleCallbackHandler(GetDirsTest testCase, int key) {
			if (testCase == null) {
				throw new NullPointerException(
						"null testCase passed to CallbackHandler constructor");
			}
			this.expectedKey = key;
			this.testCase = testCase;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			if (resultMap == null) {
				this.testCase.fails("null result map in handleResult");
			}
			return true;
		}
	};

	public static class ListCallbackHandler implements IStreamingCallback {

		int expectedKey = 0;
		GetDirsTest testCase = null;
		List<Map<String, Object>> resultsList = null;

		public ListCallbackHandler(GetDirsTest testCase, int key,
				List<Map<String, Object>> resultsList) {
			this.expectedKey = key;
			this.testCase = testCase;
			this.resultsList = resultsList;
		}

		public boolean startResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean endResults(int key) throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			return true;
		}

		public boolean handleResult(Map<String, Object> resultMap, int key)
				throws P4JavaException {
			if (key != this.expectedKey) {
				this.testCase.fails("key mismatch; expected: "
						+ this.expectedKey + "; observed: " + key);
			}
			if (resultMap == null) {
				this.testCase
						.fails("null resultMap passed to handleResult callback");
			}
			this.resultsList.add(resultMap);
			return true;
		}

		public List<Map<String, Object>> getResultsList() {
			return this.resultsList;
		}
	};
}
