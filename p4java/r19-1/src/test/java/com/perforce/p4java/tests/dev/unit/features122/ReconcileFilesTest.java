/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features122;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.PlatformType;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test 'p4 reconcile'
 */
@Jobs({"job054780"})
@TestId("Dev122_ReconcileFilesTest")
public class ReconcileFilesTest extends P4JavaRshTestCase {
    
    private IClient client = null;
    private String testZipFile;
    private String deleteFile;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ReconcileFilesTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() throws Exception {
        // initialization code (before each test).
        Properties properties = new Properties();
        properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
        assertNotNull(server);
       
        client = server.getClient("p4TestUserWS20112");
        if (getHostPlatformType() == PlatformType.WINDOWS) {
            client = server.getClient("p4TestUserWS20112Windows");
        }
        assertNotNull(client);
        server.setCurrentClient(client);

        testZipFile = client.getRoot() + "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";
        deleteFile = client.getRoot() + "/112Dev/GetOpenedFilesTest/src/gnu/getopt2/LongOpt.java";
        // revert before sync
        client.revertFiles(FileSpecBuilder.makeFileSpecList("//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt2/..."), new RevertFilesOptions());

        List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(new String[]{testZipFile, deleteFile}), new SyncOptions().setForceUpdate(true));
        assertNotNull(files);
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
     * Test 'p4 reconcile', reconcile files
     */
    @Test
    public void testReconcileFiles() throws Exception {

        IChangelist changelist = null;
        List<IFileSpec> files = null;

        int randNum = getRandomInt();
        String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/branch" + randNum;

        try {
            unpack(new File(testZipFile), new File(dir));

            // Delete a local file
            File delFile = new File(deleteFile);
            assertTrue(delFile.delete());

            changelist = getNewChangelist(server, client,
                    "Dev141_IgnoreFilesTest add files");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);

            // Reconcile files
            files = client.reconcileFiles(
                    FileSpecBuilder.makeFileSpecList(new String[]{dir + "/...", new File(dir).getParent() + "/src/gnu/getopt2/..."}),
                    new ReconcileFilesOptions().setChangelistId(
                            changelist.getId()));

            assertNotNull(files);

            int ignoreCount = 0;

            System.out.println("*************************************");
            for (IFileSpec f : files) {
                if (f.getOpStatus() == FileSpecOpStatus.INFO) {
                    assertNotNull(f.getStatusMessage());
                    System.out.println("INFO: " + f.getStatusMessage());
                    if (f.getStatusMessage().contains(
                            "ignored file can't be added")) {
                        ignoreCount++;
                    }
                } else if (f.getOpStatus() == FileSpecOpStatus.ERROR) {
                    assertNotNull(f.getStatusMessage());
                    System.out.println("ERROR: " + f.getStatusMessage());
                } else if (f.getOpStatus() == FileSpecOpStatus.VALID) {
                    assertNotNull(f.getDepotPath());
                    System.out.println("DEPOT: " + f.getDepotPath());
                    assertNotNull(f.getClientPath());
                    System.out.println("CLIENT: " + f.getClientPath());
                    assertNotNull(f.getAction());
                    System.out.println("ACTION: " + f.getAction());
                }
            }
            System.out.println("*************************************");

            // Should be greater than zero
            assertTrue(ignoreCount > 0);

            changelist.refresh();
            files = changelist.getFiles(true);
            assertNotNull(files);
            assertTrue(files.size() > 0);
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
            // Recursively delete the local test files
            if (dir != null) {
                deleteDir(new File(dir));
            }
        }
    }

    /**
     * Test 'p4 reconcile -I', reconcile with no ignore checking
     */
    @Test
    public void testReconcileFilesWithNoIgnore() throws Exception {

        IChangelist changelist = null;
        List<IFileSpec> files = null;

        int randNum = getRandomInt();
        String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/branch" + randNum;

        try {
            unpack(new File(testZipFile), new File(dir));

            // Delete a local file
            File delFile = new File(deleteFile);
            assertTrue(delFile.delete());

            changelist = getNewChangelist(server, client,
                    "Dev121_IgnoreFilesTest add files");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);

            // Reconcile files
            files = client.reconcileFiles(
                    FileSpecBuilder.makeFileSpecList(new String[]{dir + "/...", new File(dir).getParent() + "/src/gnu/getopt2/..."}),
                    new ReconcileFilesOptions().setChangelistId(
                            changelist.getId()).setNoIgnoreChecking(true));

            assertNotNull(files);

            int ignoreCount = 0;

            System.out.println("*************************************");
            for (IFileSpec f : files) {
                if (f.getOpStatus() == FileSpecOpStatus.INFO) {
                    assertNotNull(f.getStatusMessage());
                    System.out.println("INFO: " + f.getStatusMessage());
                    if (f.getStatusMessage().contains(
                            "ignored file can't be added")) {
                        ignoreCount++;
                    }
                } else if (f.getOpStatus() == FileSpecOpStatus.ERROR) {
                    assertNotNull(f.getStatusMessage());
                    System.out.println("ERROR: " + f.getStatusMessage());
                } else if (f.getOpStatus() == FileSpecOpStatus.VALID) {
                    assertNotNull(f.getDepotPath());
                    System.out.println("DEPOT: " + f.getDepotPath());
                    assertNotNull(f.getClientPath());
                    System.out.println("CLIENT: " + f.getClientPath());
                    assertNotNull(f.getAction());
                    System.out.println("ACTION: " + f.getAction());
                }
            }
            System.out.println("*************************************");

            // Should be zero
            assertTrue(ignoreCount == 0);

            changelist.refresh();
            files = changelist.getFiles(true);
            assertNotNull(files);
            assertTrue(files.size() > 0);
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
            // Recursively delete the local test files
            if (dir != null) {
                deleteDir(new File(dir));
            }
        }
    }
}
