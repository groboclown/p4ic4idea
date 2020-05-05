/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 dirs -S stream' command.
 */
@Jobs({ "job046696" })
@TestId("Dev112_GetStreamDirectoriesTest")
public class GetStreamDirectoriesTest extends P4JavaRshTestCase {

	IClient client = null;
	String streamsDepotName = "p4java_stream";
    String streamDepth = "//" + streamsDepotName + "/1";
	IClient mainStreamClient = null;
	IClient devStreamClient = null;
	int randNum = getRandomInt();
	String devStream = "//p4java_stream/dev" + randNum;
	String mainStream = "//p4java_stream/main";
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetStreamDirectoriesTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);
			// Create a main stream for the test
			IStream mainStreamObject = Stream.newStream(server, mainStream,
					"mainline", null, null, null, null, null, null, null);
			String retVal1 = server.createStream(mainStreamObject);
			// The main stream should be created
			assertNotNull(retVal1);
			assertEquals(retVal1, "Stream " + mainStream + " saved.");
			// Create a dev stream for the test
			IStream devStreamObject = Stream.newStream(server, devStream,
					"development", mainStream, null, null, null, null, null, null);
			String retVal2 = server.createStream(devStreamObject);
			// The stream should be created
			assertNotNull(retVal2);
			assertEquals(retVal2, "Stream " + devStream + " saved.");
			devStreamObject.setParent(mainStream);
			// Create clients for the test
			createStreamsClient(server, "p4java_stream_main", mainStream);
			devStreamClient = createStreamsClient(server, "p4java_stream_dev", devStream);
			assertNotNull(devStreamClient);
			// Creating text files for test
			createTextFileOnServer(devStreamClient, "newtestbranch/readonly/grep/file.txt", "desc");
			createTextFileOnServer(devStreamClient, "newtestbranch/readonly/labelsync/file.txt", "desc");
			createTextFileOnServer(devStreamClient, "newtestbranch/readonly/list/file.txt", "desc");
			createTextFileOnServer(devStreamClient, "newtestbranch/readonly/sync/file.txt", "desc");
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
	 * Test 'p4 dirs -S stream' command.
	 * @throws Exception 
	 */
	@Test
	public void testGetStreamDirs() throws Exception {
		try {
			mainStreamClient = server.getClient("p4java_stream_main");
			assertNotNull(mainStreamClient);

			// Get the test dev stream client
            devStreamClient = server.getClient("p4java_stream_dev");
            assertNotNull(devStreamClient);

			// Set the dev stream client to the server.
			server.setCurrentClient(devStreamClient);
			
			// Use a main stream directory
			String dir = mainStreamClient.getRoot()+ "/newtestbranch/readonly/*";
		
			List<IFileSpec> fileSpecs = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(dir),
					new GetDirectoriesOptions().setStream(devStream));
			assertNotNull(fileSpecs);

			// Should get an error message about the main stream directory not
			// in the current stream view (dev stream)
			assertEquals(1, fileSpecs.size());
			assertNotNull(fileSpecs.get(0));
			assertEquals(FileSpecOpStatus.ERROR, fileSpecs.get(0).getOpStatus());
			assertEquals("Path '" + dir + "/...' is not under client's root '"
					+ devStreamClient.getRoot() + "'.", fileSpecs.get(0)
					.getStatusMessage());

			// Use a regular depot directory
			dir = "//depot/*";

			fileSpecs = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(dir),
					new GetDirectoriesOptions().setStream(devStream));
			assertNotNull(fileSpecs);

			// Should get an error message about //depot/* is not there,
			// since is not in the dev stream view
			assertEquals(1, fileSpecs.size());
			assertNotNull(fileSpecs.get(0));
			assertEquals(FileSpecOpStatus.ERROR, fileSpecs.get(0).getOpStatus());
			assertEquals(dir + " - no such file(s).", fileSpecs.get(0)
					.getStatusMessage());
			
			// Use a dev stream directory
			dir = devStreamClient.getRoot() + "/newtestbranch/readonly/*";
			fileSpecs = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(dir),
					new GetDirectoriesOptions().setStream(devStream));
			assertNotNull(fileSpecs);

			// Should get some directories
			assertTrue(fileSpecs.size() > 0);

			// The expected directories, but these might change in the future...
			String[] expectedDirs = new String[] {
					"newtestbranch/readonly/grep",
					"newtestbranch/readonly/labelsync",
					"newtestbranch/readonly/list",
					"newtestbranch/readonly/sync" };

			// All of the expected directories should be in the returned list.
			// Note, this may fail in the future if the dirs are removed.
			for (String exp : expectedDirs) {
				boolean found = false;
				for (IFileSpec fs : fileSpecs) {
					if (fs.getPreferredPathString().contains(exp)) {
						found = true;
						break;
					}
				}
				assertTrue("Directory '" + exp
						+ "' not found in the returned directories.", found);
			}

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
