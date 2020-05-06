/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamViewMapping;
import com.perforce.p4java.core.IStreamViewMapping.PathType;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.Stream.StreamViewMapping;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the IOptionsServer.getStreams method.
 */
@Jobs({ "job050501" })
@TestId("Dev112_CreateStreamsTest")
public class CreateStreamsTest extends P4JavaRshTestCase {

	IClient client = null;
	IOptionsServer superServer = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", CreateStreamsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
			createStreamsDepot(streamsDepotName, server, streamDepth);
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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test create streams
	 */
	@Test
	public void testCreateStreams() {

		int randNum = getRandomInt();
		String streamName = "testmain" + randNum;
		String newStreamPath = "//p4java_stream/" + streamName;

		try {
			 // Create a stream
            IStream stream = Stream.newStream(server, newStreamPath,
                    "mainline", null, null, null, null, null, null, null);

			ViewMap<IStreamViewMapping> view = new ViewMap<IStreamViewMapping>();
			StreamViewMapping entry = new StreamViewMapping();
			entry.setPathType(PathType.SHARE);
			entry.setViewPath("...");
			entry.setOrder(0);
			view.addEntry(entry);
			stream.setStreamView(view);
			String retVal = server.createStream(stream);

			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + newStreamPath + " saved.");

			// Get the newly created stream
			IStream returnedStream = server.getStream(newStreamPath);
			assertNotNull(returnedStream);

			ViewMap<IStreamViewMapping> returnedView = returnedStream.getStreamView();
			assertNotNull(returnedView);
			assertNotNull(returnedView.getEntry(0));
			assertTrue(returnedView.getEntry(0).getPathType() == PathType.SHARE);
			assertTrue(returnedView.getEntry(0).getViewPath().contentEquals("..."));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				superServer = getSuperConnection(p4d.getRSHURL());
				assertNotNull(superServer);
				String serverMessage = superServer.deleteStream(newStreamPath,
						new StreamOptions().setForceUpdate(true));
				assertNotNull(serverMessage);
			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}

	}
}
