package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * - Add a gif file to the depot
 * - Submit a second revision with no changes
 * - Sync to first file
 * - Check out
 * - Sync to setup resolve
 * - Do a resolve -as on the file using p4java
 *
 * @testid Job036923Test
 * @job job036923
 */
@TestId("Job036923Test")
@Jobs({"Job036923Test"})
public class Job036923Test extends P4JavaTestCase {
    private static final String TEST_GIF_DEPOT_PATH = "//depot/92bugs/Job036923Test/testgif02.gif";
    private static final String TEST_JPEG_DEPOT_PATH = "//depot/92bugs/Job036923Test/testjpeg02.jpg";
    private IClient client = null;

    @Test
    public void testResolve() throws Exception {
        IServer server = null;

        IChangelist changelist = null;
        server = getServer("p4java://eng-p4java-vm.perforce.com:20131", null, userName, password);
        assertNotNull(server);
        client = getDefaultClient(server);
        assertNotNull("Unable to get default client '" + this.defaultTestClientName + "'", client);
        server.setCurrentClient(client);
        syncInitialResolveTestFiles(client);
        initialEditTestFile(server, client);
        int headRev = getHeadRev(client);
        assertTrue(headRev != IFileSpec.NO_FILE_REVISION);
        assertTrue(headRev > 1);
        IFileSpec prevSpec = getPrevRev(client, headRev);
        assertNotNull(prevSpec);
        int changeId = prevRevEdit(server, client);
        syncHeadRev(client);
        changelist = server.getChangelist(changeId);
        assertNotNull(changelist);

        // Now try the safe auto resolve:

        List<IFileSpec> resolveList = client.resolveFilesAuto(
                FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH),
                true, false, false, false, false);
        assertNotNull(resolveList);

        for (IFileSpec fSpec : resolveList) {
            assertNotNull(fSpec);
            if (fSpec.getOpStatus() != FileSpecOpStatus.INFO && fSpec.getOpStatus() != FileSpecOpStatus.VALID) {
                fail("auto resolve error: " + fSpec.getStatusMessage());
            }
        }

        // Now just get on and submit it...
        changelist.refresh();
        List<IFileSpec> submitList = changelist.submit(false);
        assertNotNull(submitList);
    }

    private void syncHeadRev(IClient client) throws Exception {
        List<IFileSpec> syncList = client.sync(
                FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH),
                true, false, false, false);
        assertNotNull("Null synclist returned from initial sync", syncList);
        for (IFileSpec fSpec : syncList) {
            assertNotNull(fSpec);
            if (fSpec.getOpStatus() == FileSpecOpStatus.ERROR) {
                fail("head sync failed: " + fSpec.getStatusMessage());
            }
        }
    }

    private IFileSpec getPrevRev(IClient client, int headRev)
            throws Exception {
        List<IFileSpec> syncList = client.sync(
                FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH + "#" + (headRev - 1)),
                false, false, false, false);
        assertNotNull(syncList);
        for (IFileSpec fSpec : syncList) {
            assertNotNull(fSpec);
            if (fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                assertNotNull(fSpec.getDepotPath());
                assertNotNull(fSpec.getDepotPath().getPathString());
                if (!fSpec.getDepotPath().getPathString().equals(TEST_GIF_DEPOT_PATH)) {
                    fail("Unable to retrieve previous file revision");
                } else {
                    return fSpec;
                }
            } else if (fSpec.getOpStatus() == FileSpecOpStatus.ERROR) {
                fail("Sync error: " + fSpec.getStatusMessage() + " File probably already open");
            }
        }
        fail("Unknown error!");
        return null;    // not reachable...
    }

    private void syncInitialResolveTestFiles(IClient client) throws Exception {
        List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH, TEST_JPEG_DEPOT_PATH);
        List<IFileSpec> syncList = client.sync(
                fileSpecs,
                true, false, false, false);
        assertNotNull("Null synclist returned from initial sync", syncList);
        boolean gotGifFile = false;
        boolean gotJpegFile = false;
        for (IFileSpec fSpec : syncList) {
            assertNotNull(fSpec);
            if (fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                assertNotNull(fSpec.getDepotPath());
                assertNotNull(fSpec.getDepotPath().getPathString());
                if (fSpec.getDepotPath().getPathString().equals(TEST_GIF_DEPOT_PATH)) {
                    gotGifFile = true;
                } else if (fSpec.getDepotPath().getPathString().equals(TEST_JPEG_DEPOT_PATH)) {
                    gotJpegFile = true;
                }
            } else if (fSpec.getOpStatus() == FileSpecOpStatus.ERROR) {
                fail("Sync error: " + fSpec.getStatusMessage() + " File probably already open");
            }
        }
        assertTrue("Test GIF file missing", gotGifFile);
        assertTrue("Test GIF file missing", gotJpegFile);
    }

    private void initialEditTestFile(IServer server, IClient client)
            throws Exception {
        Changelist changelistImpl = new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                getUserName(),
                ChangelistStatus.NEW,
                null,
                "Primary test changelist for test" + testId,
                false,
                (Server) server
        );
        IChangelist changelist = client.createChangelist(changelistImpl);
        assertNotNull("changelist creation failed", changelist);
        List<IFileSpec> editList = client.editFiles(
                FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH),
                false, false, changelist.getId(), null);
        assertNotNull(editList);
        for (IFileSpec fSpec : editList) {
            assertNotNull(fSpec);
            assertTrue(fSpec.getOpStatus() == FileSpecOpStatus.VALID);
        }
        changelist.refresh();
        List<IFileSpec> submitList = changelist.submit(false);
        assertNotNull(submitList);
        for (IFileSpec fSpec : submitList) {
            assertNotNull(fSpec);
            assertTrue(fSpec.getOpStatus() != FileSpecOpStatus.ERROR);
        }
    }

    private int prevRevEdit(IServer server, IClient client)
            throws Exception {
        Changelist changelistImpl = new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                getUserName(),
                ChangelistStatus.NEW,
                null,
                "Secondary test changelist for test" + testId,
                false,
                (Server) server
        );
        IChangelist changelist = client.createChangelist(changelistImpl);
        assertNotNull("changelist creation failed", changelist);
        List<IFileSpec> editList = client.editFiles(
                FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH),
                false, false, changelist.getId(), null);
        assertNotNull(editList);
        for (IFileSpec fSpec : editList) {
            assertNotNull(fSpec);
            assertTrue(fSpec.getOpStatus() != FileSpecOpStatus.ERROR);
        }
        return changelist.getId();
    }

    private int getHeadRev(IClient client) throws Exception {
        List<IFileSpec> haveList = client.haveList(
                FileSpecBuilder.makeFileSpecList(TEST_GIF_DEPOT_PATH));
        assertNotNull(haveList);
        assertEquals(1, haveList.size());
        assertTrue(haveList.get(0).getOpStatus() == FileSpecOpStatus.VALID);
        return haveList.get(0).getEndRevision();
    }

    @After
    public void afterEach() throws Exception {
        if (client != null) {
            List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(
                    TEST_GIF_DEPOT_PATH,
                    TEST_JPEG_DEPOT_PATH);
            client.revertFiles(fileSpecs, new RevertFilesOptions());
        }
    }
}
