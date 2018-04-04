/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for 'IsUnloaded' '1' tagged output for  'streams -U', 'clients -U' and
 * 'labels -U' commands.
 */
@Jobs({"job066150"})
@TestId("Dev131_UnloadedStatusTest")
public class UnloadedStatusTest extends P4JavaTestCase {

    private static IClient client = null;
    private static IOptionsServer superserver = null;
    private static IClient superclient = null;

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @BeforeClass
    public static void beforeAll() throws Exception{
        // initialization code (before each test).
        server = getServer();
        assertNotNull(server);
        client = getDefaultClient(server);
        assertNotNull(client);
        server.setCurrentClient(client);

        superserver = getServerAsSuper();
        assertNotNull(superserver);
        superclient = getDefaultClient(superserver);
        assertNotNull(superclient);
        superserver.setCurrentClient(superclient);
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @AfterClass
    public static void tearDown() {
        afterEach(server);
        afterEach(superserver);
    }

    /**
     * Test unloaded status of client and label
     */
    @Test
    public void testUnloadedStatusClientLabel() {

        IClient tempClient = null;
        ILabel tempLabel = null;

        try {
            // Create temp client
            String tempClientName = "testclient-" + getRandomName(testId);
            tempClient = new Client(
                    tempClientName,
                    null,    // accessed
                    null,    // updated
                    testId + " temporary test client",
                    null,
                    getUserName(),
                    getTempDirName() + "/" + testId,
                    ClientLineEnd.LOCAL,
                    null,    // client options
                    null,    // submit options
                    null,    // alt roots
                    server,
                    null
            );
            assertNotNull("Null client", tempClient);
            String resultStr = server.createClient(tempClient);
            assertNotNull(resultStr);
            tempClient = server.getClient(tempClient.getName());
            assertNotNull("couldn't retrieve new client", tempClient);

            // Create temp label
            String tempLabelName = "testlabel-" + getRandomName(testId);
            tempLabel = new Label(
                    tempLabelName,
                    getUserName(),
                    null,    // lastAccess
                    null,    // lastUpdate
                    "Temporary label created for test " + testId,
                    null,    // revisionSpec
                    false,    // locked
                    new ViewMap<ILabelMapping>()
            );
            assertNotNull("Null label", tempLabel);
            resultStr = server.createLabel(tempLabel);
            assertNotNull(resultStr);
            tempLabel = server.getLabel(tempLabel.getName());
            assertNotNull("couldn't retrieve new label", tempLabel);

            // unload client and label
            resultStr = server.unload(new UnloadOptions().setClient(tempClient.getName()).setLabel(tempLabel.getName()));
            assertNotNull(resultStr);

            // Check temp client isUnloaded()
            boolean found = false;
            List<IClientSummary> unloadedClients = server.getClients(new GetClientsOptions().setUnloaded(true));
            assertNotNull(unloadedClients);
            for (IClientSummary cs : unloadedClients) {
                if (cs.getName().equalsIgnoreCase(tempClientName)) {
                    if (cs.isUnloaded()) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);

            // Check temp label isUnloaded()
            found = false;
            List<ILabelSummary> unloadedLabels = server.getLabels(null, new GetLabelsOptions().setUnloaded(true));
            assertNotNull(unloadedLabels);
            for (ILabelSummary ls : unloadedLabels) {
                if (ls.isUnloaded()) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);

            // reload client and label
            resultStr = server.reload(new ReloadOptions().setClient(tempClient.getName()).setLabel(tempLabel.getName()));
            assertNotNull(resultStr);

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } catch (IOException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } finally {
            if (server != null) {
                if (tempClient != null) {
                    try {
                        String resultStr = server.deleteClient(tempClient.getName(), false);
                        assertNotNull(resultStr);
                    } catch (P4JavaException e) {
                        // Can't do much here...
                    }
                }
                if (tempLabel != null) {
                    try {
                        String resultStr = server.deleteLabel(tempLabel.getName(), false);
                        assertNotNull(resultStr);
                    } catch (P4JavaException e) {
                        // Can't do much here...
                    }
                }
            }
        }
    }

    /**
     * Test unloaded status of task stream
     */
    @Test
    public void testUnloadStatusTaskStream() {
        int randNum = getRandomInt();
        String streamName = "testmain" + randNum;
        String newStreamPath = "//p4java_stream/" + streamName;

        String streamName2 = "testtask" + randNum;
        String newStreamPath2 = "//p4java_stream/" + streamName2;

        try {
            // Create a stream
            IStream newStream = Stream.newStream(server, newStreamPath,
                    "mainline", null, null, null, null, null, null, null);

            String retVal = server.createStream(newStream);

            // The stream should be created
            assertNotNull(retVal);
            assertEquals(retVal, "Stream " + newStreamPath + " saved.");

            // Get the newly created stream
            IStream returnedStream = server.getStream(newStreamPath);
            assertNotNull(returnedStream);

            // Validate the content of the stream
            assertEquals(newStreamPath, returnedStream.getStream());
            assertEquals("none", returnedStream.getParent());
            assertEquals("mainline", returnedStream.getType().toString()
                    .toLowerCase(Locale.ENGLISH));
            assertEquals("allsubmit unlocked toparent fromparent",
                    returnedStream.getOptions().toString());
            assertEquals(streamName, returnedStream.getName());
            assertTrue(returnedStream.getDescription().contains(
                    Stream.DEFAULT_DESCRIPTION));
            assertTrue(returnedStream.getStreamView().getSize() == 1);
            assertTrue(returnedStream.getRemappedView().getSize() == 0);
            assertTrue(returnedStream.getIgnoredView().getSize() == 0);

            String options = "locked ownersubmit notoparent nofromparent";
            String[] viewPaths = new String[]{
                    "share ...",
                    "share core/GetOpenedFilesTest/src/gnu/getopt/...",
                    "isolate readonly/sync/p4cmd/*",
                    "import core/GetOpenedFilesTest/bin/gnu/... //p4java_stream/main/core/GetOpenedFilesTest/bin/gnu/...",
                    "exclude core/GetOpenedFilesTest/src/com/perforce/p4cmd/..."};
            String[] remappedPaths = new String[]{
                    "core/GetOpenedFilesTest/... core/GetOpenedFilesTest/src/...",
                    "core/GetOpenedFilesTest/src/... core/GetOpenedFilesTest/src/gnu/..."};
            String[] ignoredPaths = new String[]{"/temp", "/temp/...",
                    ".tmp", ".class"};

            IStream newStream2 = Stream.newStream(server, newStreamPath2,
                    "task", newStreamPath, "Task stream",
                    "The task stream of " + newStreamPath, options,
                    viewPaths, remappedPaths, ignoredPaths);

            retVal = server.createStream(newStream2);

            // The stream should be created
            assertNotNull(retVal);
            assertEquals(retVal, "Stream " + newStreamPath2 + " saved.");

            // Get the newly created stream
            returnedStream = server.getStream(newStreamPath2);
            assertNotNull(returnedStream);

            // Validate the content of the stream
            assertEquals(newStreamPath2, returnedStream.getStream());
            assertEquals(newStreamPath, returnedStream.getParent());
            assertEquals("task", returnedStream.getType().toString()
                    .toLowerCase(Locale.ENGLISH));
            assertEquals("ownersubmit locked notoparent nofromparent",
                    returnedStream.getOptions().toString());
            assertEquals("Task stream", returnedStream.getName());
            assertTrue(returnedStream.getDescription().contains(
                    "The task stream of " + newStreamPath));
            assertTrue(returnedStream.getStreamView().getSize() == 5);
            assertTrue(returnedStream.getRemappedView().getSize() == 2);
            assertTrue(returnedStream.getIgnoredView().getSize() == 4);

            // Use stream update() and refresh() methods
            returnedStream.setDescription("New updated description.");
            returnedStream.update();
            returnedStream.refresh();
            assertTrue(returnedStream.getDescription().contains(
                    "New updated description."));

            // Get all the streams
            List<IStreamSummary> streams = server.getStreams(null,
                    new GetStreamsOptions());
            assertNotNull(streams);

            // Get only the two new streams
            streams = server.getStreams(
                    new ArrayList<String>(Arrays.asList(newStreamPath,
                            newStreamPath2)), new GetStreamsOptions());
            assertNotNull(streams);
            assertTrue(streams.size() == 2);

            // Unload task stream
            retVal = server.unload(new UnloadOptions().setStream(newStreamPath2).setLocked(true));
            assertNotNull(retVal);
            assertTrue(retVal.contains("Stream " + newStreamPath2 + " unloaded."));

            // Check temp task stream isUnloaded()
            boolean found = false;
            List<IStreamSummary> unloadedStreams = server.getStreams(null, new GetStreamsOptions().setUnloaded(true));
            assertNotNull(unloadedStreams);
            for (IStreamSummary ss : unloadedStreams) {
                if (ss.isUnloaded()) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);

            // Reload task stream
            retVal = server.reload(new ReloadOptions().setStream(newStreamPath2));
            assertTrue(retVal.contains("Stream " + newStreamPath2 + " reloaded."));

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } finally {
            if (superserver != null) {
                try {
                    String serverMessage = superserver.deleteStream(newStreamPath2,
                            new StreamOptions().setForceUpdate(true));
                    assertNotNull(serverMessage);
                    serverMessage = superserver.deleteStream(newStreamPath,
                            new StreamOptions().setForceUpdate(true));
                    assertNotNull(serverMessage);

                } catch (Exception ignore) {
                    // Nothing much we can do here...
                }
            }
        }
    }

}
