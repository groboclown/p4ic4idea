/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test describe changelist with many files using IStreamingCallback
 */
@Jobs({ "job065303" })
@TestId("Dev131_FilterCallbackTest")
public class FilterCallbackTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	IChangelist changelist = null;
	List<IFileSpec> files = null;
	String serverMessage = null;
	long completedTime = 0;

	public static class SimpleCallbackHandler implements IFilterCallback {

		int limit = Integer.MAX_VALUE;
		int count = 0;
		
		private Map<String, String> doNotSkipKeysMap = null;
		
		public SimpleCallbackHandler(int limit) {
			this.limit = limit;
			this.doNotSkipKeysMap = new HashMap<String, String>();
			this.doNotSkipKeysMap.put("func", null);
		}
		
		public void reset() {
			System.out.println("count: " + count);
			this.count = 0;
		}
		
		public boolean skip(String key, Object value, final AtomicBoolean skipSubsequent) throws P4JavaException {

			if (++this.count >= this.limit) {
				skipSubsequent.set(true);
			}

			/*
			if (key != null) {
				if (key.startsWith("depotFile") ||
						key.startsWith("action") ||
						key.startsWith("type") ||
						key.startsWith("rev") ||
						key.startsWith("fileSize") ||
						key.startsWith("digest")) {
					return true;
				}
			}

			this.count++;
			*/

			return false;
		}

		public Map<String, String> getDoNotSkipKeysMap() throws P4JavaException {
			
			return this.doNotSkipKeysMap;
		}

	};

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
					//.getOptionsServer(this.serverUrlString, props);
					.getOptionsServer("p4jrpcnts://eng-p4java-vm.perforce.com:20131", props);
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
	 * Test describe changelist with many files using IStreamingCallback
	 */
	@Test
	public void testDescribeChangelistWithManyFiles() {
		int limit = 2;
		
		try {

			SimpleCallbackHandler handler = new SimpleCallbackHandler(limit);

			List<Map<String, Object>> resultsList = server.execInputStringMapCmdList(CmdSpec.DESCRIBE.toString(), new String[] { "176957" }, null, handler);

			assertNotNull(resultsList);
			assertTrue(resultsList.size() > 0);
			assertNotNull(resultsList.get(0));

//			List<IFileSpec> fileList = new ArrayList<IFileSpec>();

			for (Map<String, Object> resultmap : resultsList) {
                assertTrue(resultmap.size() == 2);
            }
            /*
                This test used to look for file revisions in the filtered
                result list and fail if there weren't any.
                There were 2 fundamental issues with this:
                    1) The order of the fields is not guaranteed, so a limit
                       of 8 may or may not include file revision details
                    2) The filter didn't work
                Also the changelist given has no jobs, data probably lost in
                an outage.
                For now the filter has been fixed and we will just check that 2 fields come back

	            if (resultmap != null) {
            		for (int i = 0; resultmap.get("rev" + i) != null; i++) {
			            FileSpec fSpec = new FileSpec(resultmap, (Server)server, i);
			            fSpec.setChangelistId(changelist);
						fileList.add(fSpec);
					}
				}
			}
			
			assertNotNull(fileList);
			assertTrue(fileList.size() > 0);
			
			for (IFileSpec file : fileList) {
				if (file != null) {
				    System.out.println(file.toString());
				}
			}
            */
		} catch (P4JavaException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
