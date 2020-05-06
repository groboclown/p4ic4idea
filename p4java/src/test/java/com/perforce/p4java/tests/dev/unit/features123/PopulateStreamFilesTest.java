/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.PopulateFilesOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test "p4 populate -Sstream -PparentStream".
 */
@Jobs({ "job058523" })
@TestId("Dev123_PopulateStreamFilesTest")
public class PopulateStreamFilesTest extends P4JavaRshTestCase {

	IOptionsServer superServer;
	private final static String depotName = "p4java_stream";
	IClient client = null;
	IClient streamClient = null;
	private int randNum = getRandomInt();
	private String streamName = "testmain" + randNum;
	private String parentStreamPath = "//p4java_stream/" + streamName;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", PopulateStreamFilesTest.class.getSimpleName());

	/**
	 * @throws Exception 
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() throws Exception {
		// initialization code (before each test).
		final String clientRoot = Paths.get("tmp\\setupClient").toAbsolutePath().toString();
		final String[] clientViews = {parentStreamPath + "/... //setupClient/..."};
		try {
		    Properties properties = new Properties();
	        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
	        createStreamsDepot(depotName, server, null);
		    assertNotNull(server);
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			superServer = getSuperConnection(p4d.getRSHURL());
			assertNotNull(superServer);
			IStream newParentStream = Stream.newStream(server, parentStreamPath,
					"mainline", null, null, null, null, null, null, null);
			String retVal = server.createStream(newParentStream);
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + parentStreamPath + " saved.");
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
	 * Test "p4 populate -Sstream -PparentStream".
	 */
	@Test
	public void testPopulateStreamFiles() {
		String serverMessage = null;
		List<IFileSpec> files = null;

		String streamName2 = "testdev" + randNum;
		String newStreamPath = "//p4java_stream/" + streamName2;

		try {
			String options = "locked ownersubmit notoparent nofromparent";
			String[] viewPaths = new String[] {
					"share ...",
					"share core/GetOpenedFilesTest/src/gnu/getopt/...",
					"isolate readonly/sync/p4cmd/*",
					"import core/GetOpenedFilesTest/bin/gnu/... //p4java_stream/main/core/GetOpenedFilesTest/bin/gnu/...",
					"exclude core/GetOpenedFilesTest/src/com/perforce/p4cmd/..." };
			String[] remappedPaths = new String[] {
					"core/GetOpenedFilesTest/... core/GetOpenedFilesTest/src/...",
					"core/GetOpenedFilesTest/src/... core/GetOpenedFilesTest/src/gnu/..." };
			String[] ignoredPaths = new String[] { "/temp", "/temp/...",
					".tmp", ".class" };

			IStream newStream = Stream.newStream(server, newStreamPath,
					"development", parentStreamPath, "Development stream",
					"The development stream of " + parentStreamPath, options,
					viewPaths, remappedPaths, ignoredPaths);

			serverMessage = server.createStream(newStream);

			// The stream should be created
			assertNotNull(serverMessage);
			assertEquals(serverMessage, "Stream " + newStreamPath + " saved.");

			files = client
					.populateFiles(
							null,
							FileSpecBuilder
									.makeFileSpecList(newStreamPath
											+ "/core/GetOpenedFilesTest/src/gnu/getopt/LongOpt.java"),
							new PopulateFilesOptions().setStream(newStreamPath)
									.setReverseMapping(true)
									.setShowPopulatedFiles(true));
			assertNotNull(files);

			// Create a stream client dedicated to the test stream
			streamClient = new Client();
			streamClient.setName("testStreamClient1" + randNum);
			streamClient.setOwnerName(client.getOwnerName());
			streamClient.setDescription(streamClient.getName()
					+ " description.");
			streamClient.setRoot(client.getRoot());
			ClientView clientView1 = new ClientView();
			String mapping1 = "//depot/101Bugs/... //" + streamClient.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping1 = new ClientViewMapping(0,
					mapping1);
			clientView1.addEntry(clientViewMapping1);

			// Set the stream's path to the client
			streamClient.setStream(newStreamPath);

			streamClient.setClientView(clientView1);
			streamClient.setServer(server);
			server.setCurrentClient(streamClient);

			// Create the stream client
			serverMessage = server.createClient(streamClient);
			assertNotNull(serverMessage);
			assertTrue(serverMessage.contentEquals("Client " + streamClient.getName()
					+ " saved."));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (superServer != null && streamClient != null && client != null) {
					// Set the streamClient to the superServer
					superServer.setCurrentClient(streamClient);

					// Delete stream files
					IChangelist changelist = getNewChangelist(superServer, streamClient,
							"Dev123_PopulateStreamFilesTest delete files.");
					assertNotNull(changelist);
					changelist = streamClient.createChangelist(changelist);
					assertNotNull(changelist);
					files = streamClient.deleteFiles(
							FileSpecBuilder.makeFileSpecList(newStreamPath
									+ "/..."), new DeleteFilesOptions()
									.setChangelistId(changelist.getId())
									.setDeleteNonSyncedFiles(true));
					assertNotNull(files);
					changelist.refresh();
					files = changelist.submit(new SubmitOptions());
					assertNotNull(files);

					// Set the classic client to the superServer
					superServer.setCurrentClient(client);
					
					// Delete stream client and stream
					serverMessage = superServer.deleteClient(streamClient.getName(), true);
					assertNotNull(serverMessage);
					serverMessage = superServer.deleteStream(
							newStreamPath,
							new StreamOptions().setForceUpdate(true));
					assertNotNull(serverMessage);
				}
			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}
	}
}
