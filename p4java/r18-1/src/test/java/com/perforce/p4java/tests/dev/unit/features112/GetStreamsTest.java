/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 streams' command.
 */
@Jobs({ "job050292" })
@TestId("Dev112_GetStreamsTest")
public class GetStreamsTest extends P4JavaTestCase {

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
			server = getServer();
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
	 * Test 'p4 streams' command errors.
	 */
	@Test
	public void testGetStreamsError() {

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add("//depot_abc*");

			List<IStreamSummary> streams = server.getStreams(streamPaths, null);
			assertNotNull(streams);
			assertEquals(0, streams.size());

		} catch (P4JavaException e) {
			assertEquals("//depot_abc* - must refer to client '" + client.getName() + "'.\n", e.getMessage());
		}

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add("//depot-ttt***");

			List<IStreamSummary> streams = server.getStreams(streamPaths, null);
			assertNotNull(streams);
			assertEquals(0, streams.size());

		} catch (P4JavaException e) {
			assertEquals("Senseless juxtaposition of wildcards in '//depot-ttt***'.\n", e.getMessage());
		}

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add("depot-abc**adsfasdf");

			List<IStreamSummary> streams = server.getStreams(streamPaths, null);
			assertNotNull(streams);
			assertEquals(0, streams.size());

		} catch (P4JavaException e) {
			assertTrue(e.getMessage().contains("depot-abc**adsfasdf' is not under client's root"));
		}
	}

	/**
	 * Test 'p4 streams //depot*' command.
	 */
	@Test
	public void testGetStreamsNotExist() {

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add("//p4java_stream/testmain6989");
			streamPaths.add("//depot/*");
			streamPaths.add("//p4java_stream/testdev2088");

			List<IStreamSummary> streams = server.getStreams(streamPaths, null);
			assertNotNull(streams);
			assertEquals(2, streams.size());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'p4 streams //p4java_stream/*' command.
	 */
	@Test
	public void testGetStreams() {

		String streamPath = "//p4java_stream/*";

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add(streamPath);

			List<IStreamSummary> streams = server.getStreams(streamPaths, null);
			assertNotNull(streams);
			assertTrue(streams.size() > 1);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'p4 streams //p4java_stream/main' command without the 'Type' field.
	 * Only the 'Stream' and 'Owner' fields.
	 */
	@Test
	public void testGetStreamsNoTypes() {

		String streamPath = "//p4java_stream/main";

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add(streamPath);
			GetStreamsOptions opts = new GetStreamsOptions();
			opts.setFields("Stream,Owner");
			opts.setMaxResults(1);

			List<IStreamSummary> streams = server.getStreams(streamPaths, opts);
			assertNotNull(streams);
			assertTrue(streams.size() == 1);
			assertNull("should not have seen stream type", streams.get(0).getType());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
