/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features152;

import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IStreamViewMapping;
import com.perforce.p4java.core.IStreamViewMapping.PathType;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.Stream.StreamViewMapping;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test streams with "import+" path type
 */
@Jobs({ "job080744" })
@TestId("Dev152_StreamImportPlusPathTest")
public class StreamImportPlusPathTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", StreamImportPlusPathTest.class.getSimpleName());

	IOptionsServer superServer = null;

	final String depotName = this.getRandomName(false, "test-stream-import-plus-path-depot");

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		final String streamDepth = "//" + depotName + "/1";
		final String clientName = "test-stream-import-plus-path-client";
		final String clientDescription = "temp stream client for test";
		final String clientRoot = Paths.get("").toAbsolutePath().toString();
		final String[] clientViews = {"//" + depotName + "/... //" + clientName + "/..."};
		try {
			setupServer(p4d.getRSHURL(),userName, password, true, null);
			createStreamsDepot(depotName, server, streamDepth);
			client = createClient(server, clientName, clientDescription, clientRoot, clientViews);
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
		if (server != null) {
			this.endServerSession(server);
		}
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test create and retrieve streams with "import+" path type.
	 */
	@Test
	public void testCreateRetrieveStream() {

		int randNum = getRandomInt();
		String streamName = "streamtest" + randNum;
		String newStreamPath = "//" + depotName + "/" + streamName;

		try {
			IStream stream = new Stream();
			stream.setStream(newStreamPath);
			stream.setType(Type.MAINLINE);
			stream.setParent(null);

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
			assertNotNull(returnedView.getEntryList());
			assertTrue(returnedView.getEntryList().size() == 1);

			assertNotNull(returnedView.getEntry(0));
			assertTrue(returnedView.getEntry(0).getPathType() == PathType.SHARE);
			assertTrue(returnedView.getEntry(0).getViewPath().contentEquals("..."));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				superServer = getServerAsSuper();
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
