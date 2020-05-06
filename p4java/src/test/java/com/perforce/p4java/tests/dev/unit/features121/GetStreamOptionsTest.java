/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamIgnoredMapping;
import com.perforce.p4java.core.IStreamRemappedMapping;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IStreamViewMapping;
import com.perforce.p4java.core.IStreamViewMapping.PathType;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.Stream.StreamIgnoredMapping;
import com.perforce.p4java.impl.generic.core.Stream.StreamRemappedMapping;
import com.perforce.p4java.impl.generic.core.Stream.StreamViewMapping;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 stream' command.
 */
@Jobs({ "job051988" })
@TestId("Dev121_GetStreamOptionsTest")
public class GetStreamOptionsTest extends P4JavaRshTestCase {

	IClient client = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetStreamOptionsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    Properties properties = new Properties();
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);
			IStream stream = Stream.newStream(server, "//p4java_stream/main", Type.MAINLINE.toString(),null, "main dev stream", null, null, null, null, null);
			server.createStream(stream);
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
	}

	/**
	 * Test 'p4 stream -o -v' command.
	 */
	@Test
	public void testGetStreamOptions() {

		int randNum = getRandomInt();
		String streamPath = "//p4java_stream/simple" + randNum;

		try {

			ViewMap<IStreamRemappedMapping> remappedView = new ViewMap<IStreamRemappedMapping>();
			StreamRemappedMapping entry = new StreamRemappedMapping();
			entry.setLeftRemapPath("y/*");
			entry.setRightRemapPath("y/z/*");
			entry.setOrder(0);
			remappedView.addEntry(entry);

			ViewMap<IStreamIgnoredMapping> ignoredView = new ViewMap<IStreamIgnoredMapping>();
			StreamIgnoredMapping iEntry = new StreamIgnoredMapping();
			iEntry.setIgnorePath(".p4config");
			iEntry.setOrder(0);
			ignoredView.addEntry(iEntry);

			ViewMap<IStreamViewMapping> view = new ViewMap<IStreamViewMapping>();
			StreamViewMapping sEntry = new StreamViewMapping();
			sEntry.setPathType(PathType.SHARE);
			sEntry.setViewPath("...");
			sEntry.setOrder(0);
			view.addEntry(sEntry);

			IStream stream = new Stream();
			stream.setDescription("A simple stream");
			stream.setName("Simple dev stream");
			stream.setParent("//p4java_stream/main");
			stream.setStream(streamPath);
			stream.setOwnerName(server.getUserName());
			stream.setType(Type.DEVELOPMENT);
			stream.setRemappedView(remappedView);
			stream.setIgnoredView(ignoredView);
			stream.setStreamView(view);
			server.createStream(stream);
			
			// Using "stream -o"
			// The client view should be empty
			stream = server.getStream(streamPath);
			assertNotNull(stream);
			assertNotNull(stream.getClientView());
			assertEquals(0, stream.getClientView().getSize());

			// Using "stream -o -v"
			// Should have the automatic generated client view
			stream = server.getStream(streamPath,
					new GetStreamOptions().setExposeClientView(true));
			assertNotNull(stream);
			assertNotNull(stream.getClientView());
			assertTrue(stream.getClientView().getSize() > 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				try {
					// Delete the stream
					server.deleteStream(streamPath,
							new StreamOptions().setForceUpdate(true));
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}

}
