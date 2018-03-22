/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the Options and DeleteFilesOptions functionality.
 */
@Jobs({"job046086"})
@TestId("Dev112_DeleteFilesOptionsTest")
public class DeleteFilesOptionsTest extends P4JavaTestCase {
    private IClient client = null;

    @BeforeAll
    public static void beforeAll() throws Exception {
        server = getServer();
        assertNotNull(server);
    }

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @BeforeEach
    public void beforeEach() throws Exception{
        client = getDefaultClient(server);
        assertNotNull(client);
        server.setCurrentClient(client);
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @AfterAll
    public static void afterAll() throws Exception {
        afterEach(server);
    }

    @Test
    public void testConstructors() {
        try {
            DeleteFilesOptions opts = new DeleteFilesOptions();
            assertNotNull(opts.processOptions(null));
            assertEquals(0, opts.processOptions(null).size());

            opts = new DeleteFilesOptions("-n", "-cdefault");
            assertNotNull(opts.getOptions());
            String[] optsStrs = opts.getOptions().toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(optsStrs.length, 2);
            assertEquals("-n", optsStrs[0]);
            assertEquals("-cdefault", optsStrs[1]);

            opts = new DeleteFilesOptions(IChangelist.DEFAULT, true, false,
                    true);
            assertTrue(opts.isNoUpdate());
            assertFalse(opts.isDeleteNonSyncedFiles());
            assertTrue(opts.isBypassClientDelete());
            assertEquals(IChangelist.DEFAULT, opts.getChangelistId());
        } catch (OptionsException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testToStrings() {
        try {
            DeleteFilesOptions opts = new DeleteFilesOptions(100, true, false,
                    true);

            assertNotNull(opts.processOptions(null));
            String[] optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(3, optsStrs.length);

            // Order is not guaranteed here, so we have to
            // search for the expected strings at each position

            boolean foundChangelistId = false;
            boolean foundNoUpdate = false;
            boolean foundDeleteNonSyncedFiles = false;
            boolean foundBypassClientDelete = false;

            for (String optStr : optsStrs) {
                if (optStr.equals("-c100"))
                    foundChangelistId = true;
                if (optStr.equals("-n"))
                    foundNoUpdate = true;
                if (optStr.equals("-v"))
                    foundDeleteNonSyncedFiles = true;
                if (optStr.equals("-k"))
                    foundBypassClientDelete = true;
            }

            assertTrue(foundChangelistId);
            assertTrue(foundNoUpdate);
            assertFalse(foundDeleteNonSyncedFiles);
            assertTrue(foundBypassClientDelete);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test setters and chaining
     */
    @Test
    public void testSetters() {
        try {
            DeleteFilesOptions opts = new DeleteFilesOptions();
            opts.setChangelistId(100);
            opts.setNoUpdate(true);
            opts.setDeleteNonSyncedFiles(false);
            opts.setBypassClientDelete(true);

            assertEquals(100, opts.getChangelistId());
            assertEquals(true, opts.isNoUpdate());
            assertEquals(false, opts.isDeleteNonSyncedFiles());
            assertEquals(true, opts.isBypassClientDelete());
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test delete files but not modifying local files (-k)
     */
    @Test
    public void testBypassClientDelete() {
        int randNum = getRandomInt();

        String files = "//depot/112Dev/GetOpenedFilesTest/bin/com/perforce/test4delete/...";
        String sourceFiles = "//depot/112Dev/GetOpenedFilesTest/bin/com/perforce/test4delete/...";
        String targetFiles = "//depot/112Dev/GetOpenedFilesTest/bin/com/perforce/ToBeDeletedGetOpenedFilesTest" + randNum + "/...";

        String deleteFileDir = client.getRoot() + "/112Dev/GetOpenedFilesTest/bin/com/perforce/ToBeDeletedGetOpenedFilesTest" + randNum;

        IChangelist copyChangelist = null;
        IChangelist deleteChangelist = null;

        List<IFileSpec> deleteFiles = null;

        try {
            copyChangelist = getNewChangelist(server, client,
                    "Dev112_DeleteFilesOptionsTest copy changelist");
            assertNotNull(copyChangelist);
            copyChangelist = client.createChangelist(copyChangelist);
            CopyFilesOptions copyOpts = new CopyFilesOptions();
            copyOpts.setChangelistId(copyChangelist.getId());

            List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
                    sourceFiles), new FileSpec(targetFiles), null, copyOpts);
            assertNotNull(copyFiles);
            List<IFileSpec> nonValidFiles = FileSpecBuilder
                    .getInvalidFileSpecs(copyFiles);
            if (nonValidFiles.size() != 0) {
                fail(nonValidFiles.get(0).getOpStatus() + ": "
                        + nonValidFiles.get(0).getStatusMessage());
            }
            copyChangelist.refresh();
            List<IFileSpec> submitCopyList = copyChangelist.submit(null);
            assertNotNull(submitCopyList);

            SyncOptions syncOpts = new SyncOptions();
            syncOpts.setForceUpdate(true);

            List<IFileSpec> syncFiles = client.sync(
                    FileSpecBuilder.makeFileSpecList(files), syncOpts);
            assertNotNull(syncFiles);

            deleteChangelist = getNewChangelist(server, client,
                    "Dev112_DeleteFilesOptionsTest delete changelist");
            assertNotNull(deleteChangelist);
            deleteChangelist = client.createChangelist(deleteChangelist);

            DeleteFilesOptions deleteOpts = new DeleteFilesOptions();
            deleteOpts.setChangelistId(deleteChangelist.getId());
            deleteOpts.setBypassClientDelete(true);

            deleteFiles = client.deleteFiles(copyFiles,
                    deleteOpts);
            assertNotNull(deleteFiles);

            deleteChangelist.refresh();
            List<IFileSpec> submitDeleteList = deleteChangelist.submit(null);
            assertNotNull(submitDeleteList);

            // Check the files deleted on the server still exist on the client
            for (IFileSpec fileSpec : deleteFiles) {
                File file = new File(fileSpec.getClientPathString());
                if (!file.exists()) {
                    fail(file.getPath() + " doesn't exists!");
                }
            }
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (client != null) {
                if (copyChangelist != null) {
                    if (copyChangelist.getStatus() == ChangelistStatus.PENDING) {
                        try {
                            // Revert files in pending changelist
                            client.revertFiles(
                                    copyChangelist.getFiles(true),
                                    new RevertFilesOptions()
                                            .setChangelistId(copyChangelist.getId()));
                        } catch (P4JavaException e) {
                            // Can't do much here...
                        }
                    }
                }
            }
            if (client != null && server != null) {
                try {
                    // Delete submitted test files
                    deleteChangelist = getNewChangelist(server,
                            client, "Dev112_DeleteFilesOptionsTest delete submitted files");
                    deleteChangelist = client
                            .createChangelist(deleteChangelist);
                    client.deleteFiles(FileSpecBuilder
                                    .makeFileSpecList(new String[]{targetFiles}),
                            new DeleteFilesOptions()
                                    .setChangelistId(deleteChangelist.getId()));
                    deleteChangelist.refresh();
                    deleteChangelist.submit(null);
                } catch (P4JavaException e) {
                    // Can't do much here...
                }
            }

            // Delete the test files recursively
            if (deleteFileDir != null) {
                try {
                    deleteFile(new File(deleteFileDir));
                } catch (IOException e) {
                    // Can't do much here...
                }
            }
        }
    }

    // Delete files/directories recursively 
    private void deleteFile(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                deleteFile(c);
        }
        f.delete();
    }
}
