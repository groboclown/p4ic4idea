/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test task streams.
 */
@Jobs({ "job062270" })
@TestId("Dev131_TaskStreamsTest")
public class TaskStreamsTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", TaskStreamsTest.class.getSimpleName());

	private static IOptionsServer superServer = null;
	private final static String depotName = "p4java_stream";
	private final static String unloadDepotName = "p4java_unload";
	private final static String streamPath = "//" + unloadDepotName + "/task-unloaded";
	private final static String streamPath2 = "//" + unloadDepotName + "/task-unloaded";


	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, props);
		createStreamsDepot(depotName, server, null);
		createUnloadDepot(unloadDepotName , server);
		IClient client = createClient(server, "p4TestUserWS");
		assertNotNull(client);
		server.setCurrentClient(client);

		IStream stream = Stream.newStream(server, streamPath, "task", null, null, null, null, null, null, null);
		server.createStream(stream);
		stream = Stream.newStream(server, streamPath2, "task", null, null, null, null, null, null, null);
		server.createStream(stream);
		UnloadOptions o = new UnloadOptions(true, false, null, null);
		o.setStream(streamPath);
		server.unload(o);
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		afterEach(server);
		afterEach(superServer);
	}

	/**
	 * Create a task streams
	 */
	@Test
	public void testCreateTaskStreams() {
		int randNum = getRandomInt();
		String streamName = "testmain" + randNum;
		String newStreamPath = "//" + depotName + "/" + streamName;

		String streamName2 = "testtask" + randNum;
		String newStreamPath2 = "//" + depotName + "/" + streamName2;

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
					"task", newStreamPath, "Task stream",
					"The task stream of " + newStreamPath, options,
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
			assertEquals("task", returnedStream.getType().toString()
					.toLowerCase(Locale.ENGLISH));
			assertEquals("ownersubmit locked notoparent nofromparent",
					returnedStream.getOptions().toString());
			assertEquals("Task stream", returnedStream.getName());
			assertTrue(returnedStream.getDescription().contains(
					"The task stream of " + newStreamPath));
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
				superServer = getSuperConnection(p4d.getRSHURL());
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

	/**
	 * Test 'p4 streams -U [streamPath]'.
	 */
	@Test
	public void testGetUnloadedTaskStreams() {
		try {
			List<String> streamPaths = new ArrayList<String>();
			streamPaths.add(streamPath);
			streamPaths.add(streamPath2);
			GetStreamsOptions opts = new GetStreamsOptions();
			opts.setFields("Stream,Owner");
			opts.setUnloaded(true);

			List<IStreamSummary> streams = server.getStreams(streamPaths, opts);

			assertNotNull(streams);
			assertEquals(2, streams.size());
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
