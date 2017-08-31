package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple basic sync test. Briefly tests basic sync
 * functionality. Options-based calls are used under the cover in the
 * Client implementation so they are tested as well.
 */

@TestId("Client_SimpleSyncTest")
public class SimpleSyncTest extends P4JavaTestCase {
    private static final String SYNCTEST_ROOT = "//depot/basic/readonly/sync/...";

    private static IClient client;
    @BeforeClass
    public static void beforeAll() throws Exception {
        server = getServer("p4java://eng-p4java-vm.perforce.com:20121", null, null, null);
        assertNotNull(server);
        client = getDefaultClient(server);
        server.setCurrentClient(client);
        assertNotNull("Unable to retrieve default test client '" + defaultTestClientName + "'", client);
    }

    @Test
    public void testSimpleSync() throws Exception {
        List<IFileSpec> targetFiles = FileSpecBuilder.makeFileSpecList(SYNCTEST_ROOT);
        // Nuke any existing local files:
        client.revertFiles(targetFiles, null);


        List<IFileSpec> haveFileSpecs = FileSpecBuilder.makeFileSpecList(SYNCTEST_ROOT + "#0");
        List<IFileSpec> files = client.sync(
                haveFileSpecs,
                true,
                false,
                false,
                false);
        assertNotNull(files);
        assertTrue(files.size() > 0);

        // Non-forced sync:
        files = client.sync(targetFiles, false, false, false, false);
        int filesSynced = files.size();
        assertTrue(diffTree(client, targetFiles, true));

        // Now try normal non-forced sync again:

        files = client.sync(targetFiles, false, false, false, false);
        assertNotNull(files);
        assertEquals(1, files.size()); // should just be the info message
        assertTrue(diffTree(client, targetFiles, true));

        // Try forcing it this time:
        files = client.sync(targetFiles, true, false, false, false);
        assertNotNull(files);
        assertEquals(filesSynced, files.size());
        assertTrue(diffTree(client, targetFiles, true));

        // Nuke the tree again:
        files = client.sync(
                haveFileSpecs,
                true,
                false,
                false,
                false);
        assertNotNull(files);
        assertTrue(files.size() > 0);

        // do the noUpdate case:

        files = client.sync(targetFiles, false, true, false, false);
        assertNotNull(files);
        assertEquals(filesSynced, files.size());
        assertFalse(diffTree(client, targetFiles, true));

        // try it with client bypass:

        files = client.sync(targetFiles, false, false, true, false);
        assertNotNull(files);
        assertEquals(filesSynced, files.size());
        assertFalse(diffTree(client, targetFiles, true));

        // Get it all back again:
        files = client.sync(targetFiles, true, false, false, false);
        assertNotNull(files);
        assertEquals(filesSynced, files.size());
        assertTrue(diffTree(client, targetFiles, true));
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
