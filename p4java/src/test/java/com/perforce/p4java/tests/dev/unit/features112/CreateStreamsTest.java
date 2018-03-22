/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 stream' command.
 */
@Jobs({ "job050690" })
@TestId("Dev112_CreateStreamsTest")
public class CreateStreamsTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

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
			client = server.getClient("p4TestUserWS");
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
	 * Test 'p4 stream' command.
	 */
	@Test
	public void testCreateStreams() {

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
			stream.setStream("//p4java_stream/simple");
			stream.setOwnerName(server.getUserName());
			stream.setType(Type.DEVELOPMENT);
			stream.setRemappedView(remappedView);
			stream.setIgnoredView(ignoredView);
			stream.setStreamView(view);		
			server.createStream(stream);

			stream = server.getStream("//p4java_stream/simple");
			assertTrue(stream.getDescription().contains("A simple stream"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

}
