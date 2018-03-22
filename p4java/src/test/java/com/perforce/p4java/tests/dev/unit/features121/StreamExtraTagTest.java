/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStream.IExtraTag;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test stream extraTag* fields
 */
@Jobs({ "job052899" })
@TestId("Dev121_StreamExtraTagTest")
public class StreamExtraTagTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IOptionsServer superServer = null;
	IClient client = null;
	String message = null;
	IChangelist changelist = null;

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
			client = server.getClient("p4java_stream_dev");
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
