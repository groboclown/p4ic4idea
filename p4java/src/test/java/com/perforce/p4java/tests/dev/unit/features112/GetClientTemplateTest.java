/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 client -S stream [[-c change] -o] [name]'.
 * <p>
 * The '-S stream' flag can be used with '-o -c change' to inspect an old stream
 * client view. It yields the client spec that would have been created for the
 * stream at the moment the change was recorded.
 */
@Jobs({ "job046693" })
@TestId("Dev112_GetClientTemplateTest")
public class GetClientTemplateTest extends P4JavaRshTestCase {

	IClient client = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";
	String streamPath = "//p4java_stream/main";

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetClientTemplateTest.class.getSimpleName());
    
	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).

		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);

			// Create a stream
			IStream stream = Stream.newStream(server, streamPath,
					"mainline", null, null, null, null, null, null, null);
			String retVal = server.createStream(stream);
			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + streamPath + " saved.");
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
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
	 * Test 'p4 client -S stream [[-c change] -o] [name]'.
	 * <p>
	 * The '-S stream' flag can be used with '-o -c change' to inspect an old
	 * stream client view. It yields the client spec that would have been
	 * created for the stream at the moment the change was recorded.
	 */
	@Test
	public void testGetClientWithStreamView() {
		int randNum = getRandomInt();

		try {
			// Get a non-existent client template without the stream parameter
			IClient newClient = server.getClientTemplate("new-client-"
					+ randNum);
			assertNotNull(newClient);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the client doesn't exist
			assertNull(newClient.getAccessed());
			assertNull(newClient.getUpdated());

			// Get a non-existent client with a stream and a changelistId. The
			// '-S stream' flag can be used with '-o -c change' to inspect an
			// old stream client view. It yields the client spec that would have
			// been created for the stream at the moment the change was recorded.
			IClient newClientStreamView = server.getClientTemplate(				
					"new-client-stream-view" + randNum,
					new GetClientTemplateOptions()
					.setStream(streamPath)
					.setChangelistId(-1)
					.setAllowExistent(false));
			assertNotNull(newClientStreamView);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the client doesn't exist
			assertNull(newClientStreamView.getAccessed());
			assertNull(newClientStreamView.getUpdated());

			// The view mapping should contain "//p4java_stream/dev"
			ViewMap<IClientViewMapping> viewMap = newClientStreamView
					.getClientView();
			assertNotNull(viewMap);
			for (IClientViewMapping entry : viewMap.getEntryList()) {
				assertTrue(entry.toString().contains("//p4java_stream/"));
			}

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
