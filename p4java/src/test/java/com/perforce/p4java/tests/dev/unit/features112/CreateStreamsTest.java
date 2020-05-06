/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
 * Test 'p4 stream' command.
 */
@Jobs({ "job050690" })
@TestId("Dev112_CreateStreamsTest")
public class CreateStreamsTest extends P4JavaRshTestCase {

	IClient client = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";
	String parentStreamPath = "//p4java_stream/main";

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", CreateStreamsTest.class.getSimpleName());

   
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);
			// Creating parent stream for the test
			IStream stream = Stream.newStream(server, parentStreamPath,
					"mainline", null, null, null, null, null, null, null);
			String retVal = server.createStream(stream);
			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + parentStreamPath + " saved.");
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
	 * Test 'p4 stream' command.
	 */
	@Test
	public void testCreateStreams() {

	    int randNum = getRandomInt();
        String streamPath = "//p4java_stream/simple" + randNum;

        
	    try {
	     	// Create a stream
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
			stream.setParent(parentStreamPath);
			stream.setStream(streamPath);
			stream.setOwnerName(server.getUserName());
			stream.setType(Type.DEVELOPMENT);
			stream.setRemappedView(remappedView);
			stream.setIgnoredView(ignoredView);
			stream.setStreamView(view);

			String retVal = server.createStream(stream);
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + streamPath + " saved.");
			
			assertTrue(stream.getDescription().contains("A simple stream"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

}
