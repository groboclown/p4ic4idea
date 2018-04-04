/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 branch [ -S stream ] [ -P parent ] -o name'.
 * <p>
 * The -S stream flag will expose the internally generated mapping. The -P flag
 * may be used with -S to treat the stream as if it were a child of a different
 * parent. The -o flag is required with -S.
 */
@Jobs({ "job046692" })
@TestId("Dev112_GetBranchSpecTest")
public class GetBranchSpecTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;

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
			server = getServer(this.getServerUrlString(), props, getUserName(),
					getPassword());
			assertNotNull(server);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
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
					serverMessage = String.valueOf(millisecsTaken);
				}
			});

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
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
	 * Test 'p4 branch [ -S stream ] [ -P parent ] -o name'.
	 * <p>
	 * The -S stream flag will expose the internally generated mapping. The -P
	 * flag may be used with -S to treat the stream as if it were a child of a
	 * different parent. The -o flag is required with -S.
	 */
	@Test
	public void testGetBranchSpecWithStreamView() {
		int randNum = getRandomInt();

		try {
			// Get an existing branch spec
			IBranchSpec existingBranchSpec = server.getBranchSpec("test-branch");
			assertNotNull(existingBranchSpec);
			assertNotNull(existingBranchSpec.getAccessed());
			assertNotNull(existingBranchSpec.getUpdated());
			
			// Get a non-existent branch without the stream parameter
			IBranchSpec newBranchSpec = server.getBranchSpec("new-branch-"
					+ randNum);
			assertNotNull(newBranchSpec);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the branch spec doesn't exist
			assertNull(newBranchSpec.getAccessed());
			assertNull(newBranchSpec.getUpdated());

			// Get a non-existent branch spec with a stream parameter
			// will ill expose the internally generated mapping.
			IBranchSpec newBranchSpecStreamView = server
					.getBranchSpec("new-branch-stream-view" + randNum,
							new GetBranchSpecOptions()
									.setStream("//p4java_stream/dev"));
			assertNotNull(newBranchSpecStreamView);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the branch spec doesn't exist
			assertNull(newBranchSpecStreamView.getAccessed());
			assertNull(newBranchSpecStreamView.getUpdated());

			// The view mapping should contain "//p4java_stream/dev"
			ViewMap<IBranchMapping> viewMap = newBranchSpecStreamView
					.getBranchView();
			assertNotNull(viewMap);
			for (IBranchMapping entry : viewMap.getEntryList()) {
				assertTrue(entry.toString().contains("//p4java_stream/"));
			}

			// Get a non-existent branch spec with a stream parameter
			// will ill expose the internally generated mapping.
			// Note: with the parent stream parameter the stream is treated
			// as if it were a child of this specified parent stream
			IBranchSpec newBranchSpecStreamView2 = server.getBranchSpec(
					"new-branch-stream-view2" + randNum,
					new GetBranchSpecOptions().setStream("//p4java_stream/dev")
							.setParentStream("//p4java_stream/main2"));
			assertNotNull(newBranchSpecStreamView2);

			// The "Updated" and "Accessed" fields should be null
			assertNull(newBranchSpecStreamView2.getAccessed());
			assertNull(newBranchSpecStreamView2.getUpdated());

			// The view mapping should contain the parent
			// "//p4java_stream/main2"
			ViewMap<IBranchMapping> viewMap2 = newBranchSpecStreamView2
					.getBranchView();
			assertNotNull(viewMap2);
			for (IBranchMapping entry : viewMap2.getEntryList()) {
				assertTrue(
					entry.toString().contains("//p4java_stream/main2")
					|| entry.toString().contains("//p4java_stream/...")
					|| entry.toString().contains("//p4java_stream/...")
					|| entry.toString().contains("//p4java_stream/..."));
			}

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
