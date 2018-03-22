/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the IOptionsServer.getStreams method.
 */
@Jobs({ "job046686" })
@TestId("Dev112_GetStreamsTest")
public class StreamsTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	IOptionsServer superServer = null;

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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test IOptionsServer.getStreams method
	 */
	@Test
	public void testStreams() {
		int randNum = getRandomInt();
		String streamName = "testmain" + randNum;
		String newStreamPath = "//p4java_stream/" + streamName;

		String streamName2 = "testdev" + randNum;
		String newStreamPath2 = "//p4java_stream/" + streamName2;

		try {
			// Create a stream
			IStream newStream = Stream.newStream(server, newStreamPath,
					"mainline", null, null, null, null, null, null, null);

			String retVal = server.createStream(newStream);

			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + newStreamPath + " saved.");

			// Get the newly created stream
			IStream returnedStream = server.getStream(newStreamPath);
			assertNotNull(returnedStream);

			// Validate the content of the stream
			assertEquals(newStreamPath, returnedStream.getStream());
			assertEquals("none", returnedStream.getParent());
			assertEquals("mainline", returnedStream.getType().toString()
					.toLowerCase(Locale.ENGLISH));
			assertEquals("allsubmit unlocked toparent fromparent",
					returnedStream.getOptions().toString());
			assertEquals(streamName, returnedStream.getName());
			assertTrue(returnedStream.getDescription().contains(
					Stream.DEFAULT_DESCRIPTION));
			assertTrue(returnedStream.getStreamView().getSize() == 1);
			assertTrue(returnedStream.getRemappedView().getSize() == 0);
			assertTrue(returnedStream.getIgnoredView().getSize() == 0);

			String options = "locked ownersubmit notoparent nofromparent";
			String[] viewPaths = new String[] {
					"share ...",
					"share core/GetOpenedFilesTest/src/gnu/getopt/...",
					"isolate readonly/sync/p4cmd/*",
					"import core/GetOpenedFilesTest/bin/gnu/... //p4java_stream/main/core/GetOpenedFilesTest/bin/gnu/...",
					"exclude core/GetOpenedFilesTest/src/com/perforce/p4cmd/..." };
			String[] remappedPaths = new String[] {
					"core/GetOpenedFilesTest/... core/GetOpenedFilesTest/src/...",
					"core/GetOpenedFilesTest/src/... core/GetOpenedFilesTest/src/gnu/..." };
			String[] ignoredPaths = new String[] { "/temp", "/temp/...",
					".tmp", ".class" };

			IStream newStream2 = Stream.newStream(server, newStreamPath2,
					"development", newStreamPath, "Development stream",
					"The development stream of " + newStreamPath, options,
					viewPaths, remappedPaths, ignoredPaths);

			retVal = server.createStream(newStream2);

			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + newStreamPath2 + " saved.");

			// Get the newly created stream
			returnedStream = server.getStream(newStreamPath2);
			assertNotNull(returnedStream);

			// Validate the content of the stream
			assertEquals(newStreamPath2, returnedStream.getStream());
			assertEquals(newStreamPath, returnedStream.getParent());
			assertEquals("development", returnedStream.getType().toString()
					.toLowerCase(Locale.ENGLISH));
			assertEquals("ownersubmit locked notoparent nofromparent",
					returnedStream.getOptions().toString());
			assertEquals("Development stream", returnedStream.getName());
			assertTrue(returnedStream.getDescription().contains(
					"The development stream of " + newStreamPath));
			assertTrue(returnedStream.getStreamView().getSize() == 5);
			assertTrue(returnedStream.getRemappedView().getSize() == 2);
			assertTrue(returnedStream.getIgnoredView().getSize() == 4);

			// Use stream update() and refresh() methods
			returnedStream.setDescription("New updated description.");
			returnedStream.update();
			returnedStream.refresh();
			assertTrue(returnedStream.getDescription().contains(
					"New updated description."));

			// Get all the streams
			List<IStreamSummary> streams = server.getStreams(null,
					new GetStreamsOptions());
			assertNotNull(streams);

			// Get only the two new streams
			streams = server.getStreams(
					new ArrayList<String>(Arrays.asList(newStreamPath,
							newStreamPath2)), new GetStreamsOptions());
			assertNotNull(streams);
			assertTrue(streams.size() == 2);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				superServer = getServerAsSuper();
				assertNotNull(superServer);
				String serverMessage = superServer.deleteStream(newStreamPath2,
						new StreamOptions().setForceUpdate(true));
				assertNotNull(serverMessage);
				serverMessage = superServer.deleteStream(newStreamPath,
						new StreamOptions().setForceUpdate(true));
				assertNotNull(serverMessage);

			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}
	}
}
