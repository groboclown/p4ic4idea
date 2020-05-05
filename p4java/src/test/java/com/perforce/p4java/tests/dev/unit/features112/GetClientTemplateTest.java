/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.perforce.p4java.tests.MockCommandCallback;
import com.perforce.p4java.tests.dev.UnitTestDevServerManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 client -S stream [[-c change] -o] [name]'.
 * <p>
 * The '-S stream' flag can be used with '-o -c change' to inspect an old stream
 * client view. It yields the client spec that would have been created for the
 * stream at the moment the change was recorded.
 */
@Jobs({ "job046693" })
@TestId("Dev112_GetClientTemplateTest")
public class GetClientTemplateTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
		// p4ic4idea: special setup
		UnitTestDevServerManager.INSTANCE.startTestClass();
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
		// p4ic4idea: special setup
		UnitTestDevServerManager.INSTANCE.endTestClass();
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).

		try {
			server = getServer(props, getUserName(),
					getPassword());
			assertNotNull(server);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
			// Register callback
			server.registerCallback(new MockCommandCallback());

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
	 * Test 'p4 client -S stream [[-c change] -o] [name]'.
	 * <p>
	 * The '-S stream' flag can be used with '-o -c change' to inspect an old
	 * stream client view. It yields the client spec that would have been
	 * created for the stream at the moment the change was recorded.
	 */
	@Test
	public void testGetClientWithStreamView() {
		int randNum = getRandomInt();

		try {
			// Get a non-existent client template without the stream parameter
			IClient newClient = server.getClientTemplate("new-client-"
					+ randNum);
			assertNotNull(newClient);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the client doesn't exist
			assertNull(newClient.getAccessed());
			assertNull(newClient.getUpdated());

			// Get a non-existent client with a stream and a changelistId. The
			// '-S stream' flag can be used with '-o -c change' to inspect an
			// old stream client view. It yields the client spec that would have
			// been created for the stream at the moment the change was recorded.
			IClient newClientStreamView = server.getClientTemplate(				
					"new-client-stream-view" + randNum,
					new GetClientTemplateOptions()
					.setStream("//p4java_stream/dev")
					.setChangelistId(-1)
					.setAllowExistent(false));
			assertNotNull(newClientStreamView);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the client doesn't exist
			assertNull(newClientStreamView.getAccessed());
			assertNull(newClientStreamView.getUpdated());

			// The view mapping should contain "//p4java_stream/dev"
			ViewMap<IClientViewMapping> viewMap = newClientStreamView
					.getClientView();
			assertNotNull(viewMap);
			for (IClientViewMapping entry : viewMap.getEntryList()) {
				assertTrue(entry.toString().contains("//p4java_stream/"));
			}

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
