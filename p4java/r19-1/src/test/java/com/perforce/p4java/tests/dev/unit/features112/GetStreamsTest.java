/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 streams' command.
 */
@Jobs({ "job050292" })
@TestId("Dev112_GetStreamsTest")
public class GetStreamsTest extends P4JavaRshTestCase {

	private static IClient client = null;
	private static String streamPathDev = "//p4java_stream/dev";
	private static String streamPathMain = "//p4java_stream/main";
	private static String streamsDepotName = "p4java_stream";
	private static String streamDepth = "//" + streamsDepotName + "/1";
	
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetStreamsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@BeforeClass
	public static void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);
			// Create test streams
			IStream streamMain = Stream.newStream(server, streamPathMain,
					"mainline", null, null, null, null, null, null, null);
			String retVal1 = server.createStream(streamMain);
			assertNotNull(retVal1);
			assertEquals(retVal1, "Stream " + streamPathMain + " saved.");
			IStream streamDev = Stream.newStream(server, streamPathDev,
					"development", streamPathMain, null, null, null, null, null, null);
			String retVal = server.createStream(streamDev);
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + streamPathDev + " saved.");
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@AfterClass
	public static void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			endServerSession(server);
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
			streamPaths.add(streamPathMain);
			streamPaths.add("//depot/*");
			streamPaths.add(streamPathDev);

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
		String streamsPath = "//p4java_stream/*";

		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add(streamsPath);
			
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
