/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetBranchSpecOptions;
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
 * Test 'p4 branch [ -S stream ] [ -P parent ] -o name'.
 * <p>
 * The -S stream flag will expose the internally generated mapping. The -P flag
 * may be used with -S to treat the stream as if it were a child of a different
 * parent. The -o flag is required with -S.
 */
@Jobs({ "job046692" })
@TestId("Dev112_GetBranchSpecTest")
public class GetBranchSpecTest extends P4JavaRshTestCase {

	IClient client = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";
	int randNum = getRandomInt();
	String streamPath = "//p4java_stream/dev";
	String streamName = "main" + randNum;
	String parentStreamPath = "//p4java_stream/" + streamName;
	
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetBranchSpecTest.class.getSimpleName());

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
    			IBranchSpec branchSpec =  BranchSpec.newBranchSpec(server, "test-branch", "testbranch", new String[]{});
                server.createBranchSpec(branchSpec);
                createStreamsDepot(streamsDepotName, server, streamDepth);

			   // Create a parentstream
			   IStream newParentStream = Stream.newStream(server, parentStreamPath,
					   "mainline", null, null, null, null, null, null, null);
			   String retVal = server.createStream(newParentStream);
			   // parent stream should be created
			   assertNotNull(retVal);
			   assertEquals(retVal, "Stream " + parentStreamPath + " saved.");
			   // Create a stream
			   IStream stream = Stream.newStream(server, streamPath,
					   "development", parentStreamPath, null, null, null, null, null, null);
			   String retVal1 = server.createStream(stream);
			   // The stream should be created
			   assertNotNull(retVal1);
			   assertEquals(retVal1, "Stream " + streamPath + " saved.");
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
	 * Test 'p4 branch [ -S stream ] [ -P parent ] -o name'.
	 * <p>
	 * The -S stream flag will expose the internally generated mapping. The -P
	 * flag may be used with -S to treat the stream as if it were a child of a
	 * different parent. The -o flag is required with -S.
	 */
	@Test
	public void testGetBranchSpecWithStreamView() {
		try {
			// Get an existing branch spec
			IBranchSpec existingBranchSpec = server.getBranchSpec("test-branch");
			assertNotNull(existingBranchSpec);
			assertNotNull(existingBranchSpec.getAccessed());
			assertNotNull(existingBranchSpec.getUpdated());
			
			// Get a non-existent branch without the stream parameter
			IBranchSpec newBranchSpec = server.getBranchSpec("new-branch-"
					+ randNum);
			assertNotNull(newBranchSpec);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the branch spec doesn't exist
			assertNull(newBranchSpec.getAccessed());
			assertNull(newBranchSpec.getUpdated());

			// Get a non-existent branch spec with a stream parameter
			// will ill expose the internally generated mapping.
			IBranchSpec newBranchSpecStreamView = server
					.getBranchSpec("new-branch-stream-view" + randNum,
							new GetBranchSpecOptions()
									.setStream(streamPath));
			assertNotNull(newBranchSpecStreamView);

			// The "Updated" and "Accessed" fields should be null
			// These null fields indicate the branch spec doesn't exist
			assertNull(newBranchSpecStreamView.getAccessed());
			assertNull(newBranchSpecStreamView.getUpdated());

			// The view mapping should contain "//p4java_stream/dev"
			ViewMap<IBranchMapping> viewMap = newBranchSpecStreamView
					.getBranchView();
			assertNotNull(viewMap);
			for (IBranchMapping entry : viewMap.getEntryList()) {
				assertTrue(entry.toString().contains("//p4java_stream/"));
			}

			// Get a non-existent branch spec with a stream parameter
			// will ill expose the internally generated mapping.
			// Note: with the parent stream parameter the stream is treated
			// as if it were a child of this specified parent stream
			IBranchSpec newBranchSpecStreamView2 = server.getBranchSpec(
					"new-branch-stream-view2" + randNum,
					new GetBranchSpecOptions().setStream(streamPath)
							.setParentStream(parentStreamPath));
			assertNotNull(newBranchSpecStreamView2);

			// The "Updated" and "Accessed" fields should be null
			assertNull(newBranchSpecStreamView2.getAccessed());
			assertNull(newBranchSpecStreamView2.getUpdated());

			// The view mapping should contain the parent
			// "//p4java_stream/main2"
			ViewMap<IBranchMapping> viewMap2 = newBranchSpecStreamView2
					.getBranchView();
			assertNotNull(viewMap2);
			for (IBranchMapping entry : viewMap2.getEntryList()) {
				assertTrue(
					entry.toString().contains(parentStreamPath)
					|| entry.toString().contains("//p4java_stream/...")
					|| entry.toString().contains("//p4java_stream/...")
					|| entry.toString().contains("//p4java_stream/..."));
			}

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
