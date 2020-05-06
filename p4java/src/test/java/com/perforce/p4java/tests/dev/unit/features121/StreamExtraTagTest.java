/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStream.IExtraTag;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test stream extraTag* fields
 */
@Jobs({ "job052899" })
@TestId("Dev121_StreamExtraTagTest")
public class StreamExtraTagTest extends P4JavaRshTestCase {

	IClient client = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", StreamExtraTagTest.class.getSimpleName());	

	/** 
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() throws Exception {
		// initialization code (before each test).
		try {
		    Properties properties = new Properties();
	        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
			assertNotNull(server);
			client = createClient(server, "StreamExtraTagTestClient");
			assertNotNull(client);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);
		} catch (P4JavaException e) {
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
	 * Test stream extraTag* fields
	 */
	@Test
	public void testStreamExtraTag() {

		String streamPath = "//p4java_stream/dev";

		try {
			IStream stream = server.getStream(streamPath);
			assertNotNull(stream);
			List<IExtraTag> extraTags = stream.getExtraTags();
			assertNotNull(extraTags);
			assertTrue(extraTags.size() > 0);
			assertTrue(extraTags.get(0).getName().equalsIgnoreCase("firmerThanParent"));
			assertTrue(extraTags.get(0).getType().equalsIgnoreCase("word"));
			assertTrue(extraTags.get(0).getValue().equalsIgnoreCase("false"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
