/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test symlink support - sync command. This only works with JDK 7 or above on
 * the UNIX environment and newer versions of Windows (Vista or above).<p>
 * <p>
 * Note that hard links are available as of Windows 2000, and symbolic links as
 * of Windows Vista.
 */
@Jobs({"job038013", "job034097"})
@TestId("Dev123_SymbolicLinkSyncTest")
public class SymbolicLinkSyncTest extends P4JavaTestCase {

    private static IOptionsServer superServer = null;
    private static IClient superClient = null;
    private static IClient client = null;

    /**
     * @BeforeClass annotation to a method to be run before all the tests in a class.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        superServer = getServerAsSuper();
        superClient = superServer.getClient("p4TestSuperWS20112");
        assertNotNull(superClient);
        superServer.setCurrentClient(superClient);

        server = getServer();
        assertNotNull(server);
        client = server.getClient(getPlatformClientName("p4TestUserWS20112"));
        assertNotNull(client);
        server.setCurrentClient(client);
    }

    /**
     * @AfterClass annotation to a method to be run after all the tests in a class.
     */
    @AfterClass
    public static void oneTimeTearDown() {
        afterEach(superServer);
        afterEach(server);
    }

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        deletedExistLinkDirBeforeTest();
    }

    private void deletedExistLinkDirBeforeTest() {
        String clientRoot = client.getRoot();
        Path parentTestPath = Paths.get(clientRoot, "symlinks");
        if (Files.exists(parentTestPath)) {
            deleteDir(parentTestPath.toFile());
        }
    }

    /**
     * Test symlink support - sync command. This only works with JDK 7 or above
     * and non-Windows environment.
     */
    @Test
    public void testSyncSymlinks() {

        String[] repoPaths = new String[]{
                "//depot/symlinks/testdira/testdir2/...",
                "//depot/symlinks/testdira/testdirb/testfile.txt",
                "//depot/symlinks/testdira/testdir2"};

        try {

            List<IFileSpec> files = client.sync(
                    FileSpecBuilder.makeFileSpecList(repoPaths),
                    new SyncOptions().setForceUpdate(true));
            assertNotNull(files);

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

    /**
     * Test add symbolic links
     */
    @Test
    public void testAddSymlinks() throws Exception {
        IChangelist changelist = null;
        String clientRoot = client.getRoot();
        String path = null;
        String depotPath = null;
        int rand = getRandomInt();

        try {
            // Check if symlink capable
            if (SymbolicLinkHelper.isSymbolicLinkCapable()) {

                String target = clientRoot + File.separator + "symlinks" + File.separator + "testdir" + File.separator + "testdir2";
                String link = clientRoot + File.separator + "symlinks" + File.separator + "testdir" + File.separator + "testdir2-symlink-" + rand;
                depotPath = "//depot/symlinks/testdir/" + "testfile2-symlink-" + rand;

                // Sync files
                List<IFileSpec> files = client.sync(
                        FileSpecBuilder.makeFileSpecList("//depot/symlinks/testdir/..."),
                        new SyncOptions().setForceUpdate(true));
                assertNotNull(files);

                // Create symbolic link
                path = SymbolicLinkHelper.createSymbolicLink(link, target);

                boolean isSymlink = SymbolicLinkHelper.isSymbolicLink(path
                        .toString());
                assertTrue(isSymlink);

                changelist = getNewChangelist(server,
                        client,
                        "Dev123_SymbolicLinkSyncTest add symbolic link.");
                assertNotNull(changelist);
                changelist = client.createChangelist(changelist);
                assertNotNull(changelist);

                // Add a file specified as "binary" even though it is "text"
                files = client.addFiles(
                        FileSpecBuilder.makeFileSpecList(link),
                        new AddFilesOptions().setChangelistId(changelist.getId()));
                assertNotNull(files);

                changelist.refresh();
                files = changelist.submit(new SubmitOptions());
                assertNotNull(files);

                // Delete the local symbolic link
                File delFile = new File(path);
                boolean delSuccess = delFile.delete();
                assertTrue(delSuccess);

                // Force sync of the submitted symbolic link
                files = client.sync(
                        FileSpecBuilder.makeFileSpecList(path),
                        new SyncOptions().setForceUpdate(true));
                assertNotNull(files);

                // Read the target path of the symbolic link
                // Verify the symbolic link has the correct target path
                String linkTarget = SymbolicLinkHelper.readSymbolicLink(path);
                assertNotNull(linkTarget);
                assertEquals(target, linkTarget);
            }
        } finally {
            if (client != null) {
                if (changelist != null) {
                    if (changelist.getStatus() == ChangelistStatus.PENDING) {
                        try {
                            // Revert files in pending changelist
                            client.revertFiles(
                                    changelist.getFiles(true),
                                    new RevertFilesOptions()
                                            .setChangelistId(changelist.getId()));
                        } catch (P4JavaException e) {
                            // Can't do much here...
                        }
                    }
                }
            }
            if (path != null) {
                if (superClient != null && superServer != null) {
                    try {
                        List<IObliterateResult> obliterateFiles = superServer
                                .obliterateFiles(FileSpecBuilder
                                                .makeFileSpecList(depotPath),
                                        new ObliterateFilesOptions()
                                                .setExecuteObliterate(true));
                        assertNotNull(obliterateFiles);
                    } catch (P4JavaException e) {
                        // Can't do much here...
                    }
                }
                File file = new File(path.toString());
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }
}
