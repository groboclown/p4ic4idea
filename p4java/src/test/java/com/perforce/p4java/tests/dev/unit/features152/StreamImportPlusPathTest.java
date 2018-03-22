/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features152;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
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
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test streams with "import+" path type
 */
@Jobs({ "job080744" })
@TestId("Dev152_StreamImportPlusPathTest")
public class StreamImportPlusPathTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	IOptionsServer server2 = null;
	IClient client2 = null;

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

		if (server2 != null) {
			this.endServerSession(server2);
		}
	}

	/**
	 * Test create and retrieve streams with "import+" path type.
	 */
	@Test
	public void testCreateRetrieveStream() {

		int randNum = getRandomInt();
		String streamName = "testmain" + randNum;
		String newStreamPath = "//p4java_stream/" + streamName;

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
