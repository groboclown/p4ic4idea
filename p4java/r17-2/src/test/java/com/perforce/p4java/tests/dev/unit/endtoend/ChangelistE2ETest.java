package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;


/**
 * This test class exercised the End-to-End actions of P4Java Changelist. It was created as a catch-all for
 * issues that were found or questions that were asked during development of other tests
 * that used this API. Therefore, it is in no way exhaustive.
 */
@TestId("ChangelistE2ETest01")
public class ChangelistE2ETest extends P4JavaTestCase {

    public final int P4JTEST_COMPARE_EXACT = 0;
    private static IClient client = null;
    private static String sourceFile;
    private static String clientDir;

    @BeforeClass
    public static void beforeAll() throws Exception {
        server = getServer();
        client = getDefaultClient(server);
        clientDir = defaultTestClientName + File.separator + testId;
        server.setCurrentClient(client);
        sourceFile = client.getRoot() + File.separator + textBaseFile;
        createTestSourceFile(sourceFile, false);
    }


    @Test(expected = com.perforce.p4java.exception.RequestException.class)
    public void testCreateDefaultChangelistRequestException() throws Exception {
        try {
            debugPrintTestName("testCreateDefaultChangelistErr");

            @SuppressWarnings("unused")
            IChangelist changelist = createTestChangelist(server, client,
                    "This test tries to add files to an alternately created default changelist", true);
            fail("Should have thrown RequestException for createTestChangelist with id=IChangelist.DEFAULT.");

        } finally {
            debugPrint("## Test End ##");
        }
    }

    /**
     * Add a file to the default changelist using changelist impl. File is not submitted.
     */
    @Test
    public void testReopenAddFilesInDefaultChangelist() {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testReopenAddFilesInDefaultChangelist");
            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileNew.txt";

            String newBaseFile = prepareTestFile(sourceFile, newFile, true);
            final String[] filePaths = {
                    new File(clientRoot + File.separator + clientDir, newBaseFile).getAbsolutePath(),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            changelist.setId(Changelist.DEFAULT);
            int changelistId = changelist.getId();
            assertEquals("changelist.getId() should return zero.", Changelist.DEFAULT, changelistId);

            List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            List<IFileSpec> reopenedSpecs = client.reopenFiles(fileSpecs, changelistId, P4JTEST_FILETYPE_TEXT);
            verifyChangelistId(reopenedSpecs, Changelist.DEFAULT, P4JTEST_COMPARE_EXACT);

            testFiles = client.addFiles(reopenedSpecs, false, changelistId, P4JTEST_FILETYPE_TEXT, true);
            dumpFileSpecInfo(testFiles, "TestFiles After AddFiles");
            verifyChangelistId(testFiles, Changelist.DEFAULT, P4JTEST_COMPARE_EXACT);

            //submit files
            submittedFiles = changelist.submit(false);
            assertNotNull("submittedFiles should not be Null.", submittedFiles);

        } catch (Exception exc) {
            debugPrint("Exception Caught: " + exc, exc.getLocalizedMessage());
            fail("Unexpected Exception: " + exc + " msg: " + exc.getLocalizedMessage());
        }
    }


    @Test(expected = com.perforce.p4java.exception.RequestException.class)
    public void testUpdateOnDefaultChangelistErr() throws Exception {

        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            String clientRoot = client.getRoot();

            String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileNew.txt";

            String newBaseFile = prepareTestFile(sourceFile, newFile, false);
            final String[] filePaths = {
                    new File(clientRoot + File.separator + clientDir, newBaseFile).getAbsolutePath(),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            changelist.setId(Changelist.DEFAULT);
            int changelistId = changelist.getId();
            assertEquals("changelist.getId() should return zero.", Changelist.DEFAULT, changelistId);
            testFiles = addTestFiles(client, filePaths, 0, true, P4JTEST_FILETYPE_TEXT);
            assertNotNull("testFiles should not be Null.", testFiles);
            //submit files
            changelist.update();
            fail("Should have thrown RequestException for attempting to call update on default changelist");

        } finally {
            debugPrint("## Test End ##");
        }
    }


    @Test
    public void testAddOneFileToDefaultChangelist() throws Exception {
        List<IFileSpec> testFiles = null;

        try {

            debugPrintTestName("testDefaultChangelistForAddFiles");

            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileNew.txt";

            String newBaseFile = prepareTestFile(sourceFile, newFile, false);
            final String[] filePaths = {
                    new File(clientRoot + File.separator + clientDir, newBaseFile).getAbsolutePath(),
            };

            testFiles = addTestFiles(client, filePaths, IChangelist.DEFAULT, true, P4JTEST_FILETYPE_TEXT);
            assertNotNull("testFiles should not be null", testFiles);
            verifyFileAction(testFiles, filePaths.length, FileAction.ADD);
            //FIXME: verifyChangelistId(testFiles, IChangelist.DEFAULT, P4JTEST_COMPARE_EXACT);

        } finally {
            debugPrint("## Test End ##");
        }
    }

    /**
     * Creates a numbered changelist and adds one file to it
     */
    @Test
    public void testSubmitEmptyNumberedChangelist() throws Exception {

        List<IFileSpec> submittedFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testSubmitEmptyNumberedChangelist");

            String clientRoot = client.getRoot();

            //create a changelist
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            assertNotNull("changelist should not be Null.", changelist);

            //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);
            submittedFiles = changelist.submit(false);
            dumpFileSpecInfo(submittedFiles, "submittedFiles");
            verifyFileSpecInfo(submittedFiles, FileSpecOpStatus.ERROR, "No files to submit");

        } finally {
            debugPrint("## Test End ##");
        }
    }


    /**
     *
     */
    @Test
    public void testAddOneFileToNumberedChangelistBadPath() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;
            debugPrintTestName("testAddOneFileToNumberedChangelistBadPath");

            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(clientRoot + prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            int changelistId = changelist.getId();

            testFiles = addTestFiles(client, filePaths, changelistId, true, P4JTEST_FILETYPE_TEXT);

            dumpFileSpecInfo(testFiles, "testFiles");
            assertNotNull("testFiles should not be null", testFiles);
            changelist.update();

            submittedFiles = changelist.submit(false);
            dumpFileSpecInfo(submittedFiles, "submittedFiles");
            verifyFileSpecInfo(submittedFiles, FileSpecOpStatus.ERROR, "");
    }


    /**
     * Creates a numbered changelist and adds one file to it
     */
    @Test
    public void testAddOneFileToNumberedChangelist() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testAddOneFileToNumberedChangelist");
            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            int changelistId = changelist.getId();

            testFiles = addTestFiles(client, filePaths, changelistId, true, P4JTEST_FILETYPE_TEXT);

            dumpFileSpecInfo(testFiles, "testFiles");
            assertNotNull("testFiles should not be null", testFiles);
            verifyFileAction(testFiles, filePaths.length, FileAction.ADD);
            //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);
            //submit files
            submittedFiles = changelist.submit(false);
            dumpFileSpecInfo(submittedFiles, "submittedFiles");
            verifyFileAction(submittedFiles, filePaths.length, FileAction.ADD);

        } finally {
            debugPrint("## Test End ##");
        }
    }


    /**
     * Creates a numbered changelist and adds three files to it. Submits.
     */
    @Test
    public void testAddMultFilesToNumberedChangelist() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testAddMultFilesToNumberedChangelist");
            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
                    createDepotPathSyntax(clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            int changelistId = changelist.getId();

            testFiles = addTestFiles(client, filePaths, changelistId, true, P4JTEST_FILETYPE_TEXT);

            dumpFileSpecInfo(testFiles, "testFiles");
            assertNotNull("testFiles should not be null", testFiles);
            verifyFileAction(testFiles, filePaths.length, FileAction.ADD);
            //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);

            //submit files
            submittedFiles = changelist.submit(false);
            dumpFileSpecInfo(submittedFiles, "submittedFiles");
            verifyFileAction(submittedFiles, filePaths.length, FileAction.ADD);

        } finally {
            debugPrint("## Test End ##");
        }
    }


    //3 tests with Changelists in various states

    /**
     * Creates a numbered changelist, adds files to it at different stages,
     * checks that the status is pending and then submits.
     */
    @Test
    public void testChangelistStatusPending() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testChangelistStatusPending");

            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            String newBaseFile = prepareTestFile(sourceFile, newFile, false);
            final String[] filePaths = {
                    new File(newBaseFile).getAbsolutePath(),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            int changelistId = changelist.getId();

            testFiles = addTestFiles(client, filePaths, changelistId, true, P4JTEST_FILETYPE_TEXT);

            dumpFileSpecInfo(testFiles, "testFiles");
            assertNotNull("testFiles should not be null", testFiles);
            verifyFileAction(testFiles, filePaths.length, FileAction.ADD);
            //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);

            verifyChangelistStatus(changelist, ChangelistStatus.PENDING);
            newBaseFile = prepareTestFile(sourceFile, newFile, false);
            final String[] filePaths2 = {
                    new File(newBaseFile).getAbsolutePath(),
            };
            testFiles = addTestFiles(client, filePaths2, changelistId, true, P4JTEST_FILETYPE_TEXT);
            verifyChangelistStatus(changelist, ChangelistStatus.PENDING);

            //submit files
            submittedFiles = changelist.submit(false);
            dumpFileSpecInfo(submittedFiles, "submittedFiles");
            verifyFileAction(submittedFiles, filePaths.length + filePaths2.length, FileAction.ADD);

        } finally {
            debugPrint("## Test End ##");
        }
    }

    /**
     * Creates several numbered changelistImpls, checks status is NEW before
     * client.createChangelist, adds files to each separately.
     */
    @Test
    public void testChangelistStatusNew() throws Exception {

        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testChangelistStatusNew");
            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
                    createDepotPathSyntax(clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    createClientPathSyntax(defaultTestClientName, clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
            };

            //create a changelist
            int[] changelistIds = new int[filePaths.length];
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            for (int i = 0; i <= filePaths.length - 1; i++) {
                debugPrint("Pass: " + i);
                Changelist changelistImpl = createNewChangelistImpl(server, client,
                        "Changelist to verify changelist status " + getName(), useDefaultChangelist);
                verifyChangelistStatus(changelistImpl, ChangelistStatus.NEW);
                changelist = client.createChangelist(changelistImpl);
                changelistIds[i] = changelist.getId();
                testFiles = addTestFiles(client, filePaths[i], changelistIds[i], true, P4JTEST_FILETYPE_TEXT);
                dumpFileSpecInfo(testFiles, "testFiles");
                assertNotNull("testFiles should not be null", testFiles);
                verifyFileAction(testFiles, 1, FileAction.ADD);
                //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);
            }

        } finally {
            debugPrint("## Test End ##");
        }
    }

    /**
     * Creates several numbered changelists, checks status is PENDING
     * adds files to each separately.
     */
    @Test
    public void testChangelistStatusPendingAfterCreate() throws Exception {

        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testChangelistStatusPendingAfterCreate");

            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
                    createDepotPathSyntax(clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    createClientPathSyntax(defaultTestClientName, clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
            };

            //create a changelist
            int[] changelistIds = new int[filePaths.length];
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            for (int i = 0; i <= filePaths.length - 1; i++) {
                debugPrint("Pass: " + i);
                changelist = createTestChangelist(server, client,
                        "Changelist to submit edited files " + getName(), useDefaultChangelist);
                verifyChangelistStatus(changelist, ChangelistStatus.PENDING);
                changelistIds[i] = changelist.getId();
                testFiles = addTestFiles(client, filePaths[i], changelistIds[i], true, P4JTEST_FILETYPE_TEXT);
                dumpFileSpecInfo(testFiles, "testFiles");
                assertNotNull("testFiles should not be null", testFiles);
                verifyFileAction(testFiles, 1, FileAction.ADD);
                //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);
            }

        } finally {
            debugPrint("## Test End ##");
        }
    }


    /**
     * Creates several numbered changelists, checks status is PENDING
     * adds files to each separately.
     */
    @Test
    public void testChangelistStatusSubmitted() throws Exception {

        List<IFileSpec> testFiles = null;
        //IChangelist changelist = null;

        try {

            debugPrintTestName("testChangelistStatusPendingAfterCreate");

            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
                    createDepotPathSyntax(testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
            };

            //create a changelist
            int[] changelistIds = new int[filePaths.length];
            IChangelist[] chgLists = new Changelist[filePaths.length];
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            for (int i = 0; i <= filePaths.length - 1; i++) {
                debugPrint("Pass: " + i);
                chgLists[i] = createTestChangelist(server, client,
                        "Changelist to submit edited files " + getName(), useDefaultChangelist);
                changelistIds[i] = chgLists[i].getId();
                testFiles = addTestFiles(client, filePaths[i], changelistIds[i], true, P4JTEST_FILETYPE_TEXT);
                dumpFileSpecInfo(testFiles, "testFiles");
                assertNotNull("testFiles should not be null", testFiles);
                verifyFileAction(testFiles, 1, FileAction.ADD);
                //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);
            }

            for (int i = 0; i <= filePaths.length - 1; i++) {
                debugPrint("Pass: " + i);
                chgLists[i].submit(false);

                verifyChangelistStatus(chgLists[i], ChangelistStatus.SUBMITTED);
            }


        } finally {
            debugPrint("## Test End ##");
        }
    }


    /**
     * Creates a numbered changelist with fileSpecs, then uses changelist.getFiles()
     * to get the files. Right now, doesn't check files to see if they are correct.
     */
    @Test
    public void testChangelistGetFiles() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        try {

            debugPrintTestName("testAddMultFilesToNumberedChangelist");
            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);

            String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileNew.txt";

            final String[] filePaths = {
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
                    createDepotPathSyntax(testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
                    new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
            };

            //create a changelist
            boolean useDefaultChangelist = false; //false sets id=IChangelist.UNKNOWN
            changelist = createTestChangelist(server, client,
                    "Changelist to submit edited files " + getName(), useDefaultChangelist);
            int changelistId = changelist.getId();

            testFiles = addTestFiles(client, filePaths, changelistId, true, P4JTEST_FILETYPE_TEXT);

            dumpFileSpecInfo(testFiles, "testFiles");
            assertNotNull("testFiles should not be null", testFiles);
            verifyFileAction(testFiles, filePaths.length, FileAction.ADD);
            //FIXME: verifyChangelistId(testFiles, changelistId, P4JTEST_COMPARE_EXACT);

            List<IFileSpec> serverFileSpecs = changelist.getFiles(true);
            changelist.refresh();
            verifyTestFilesInFileSpecs(filePaths, serverFileSpecs);
            submittedFiles = changelist.submit(false);
            dumpFileSpecInfo(submittedFiles, "submittedFiles");
            verifyFileAction(submittedFiles, filePaths.length, FileAction.ADD);

        } finally {
            debugPrint("## Test End ##");
        }
    }


    //***********************//
    //** Helper Functions **//
    //**********************//

    /**
     * Right now this just dumps the fileSpec Info and the filePaths[] array. No verification is done.
     * Need to figure out how efficiently to do this.
     */
    private void verifyTestFilesInFileSpecs(String[] testFilePaths, List<IFileSpec> serverFileSpecs) {

        int i = 0;
        String[] tmpPathParts;
        String delimiter = "/";

        //FIXME: Need to actually make this work.
        debugPrint("** verifyTestFilesInFileSpecs **");
        for (IFileSpec fSpec : serverFileSpecs) {
            String fSpecDepotPath = fSpec.getDepotPathString();
            debugPrint("FileSpec" + i + ": ", "testFilePaths" + i + ": " + testFilePaths[i], "fSpec.depotPath: " + fSpecDepotPath);
            tmpPathParts = testFilePaths[i].split(delimiter);
            debugPrint("tmpPathParts: " + tmpPathParts[tmpPathParts.length - 1]);
            i++;
        }

    }

    /**
     * Populates a changelistImpl allowing direct choice of id=IChangelist.DEFAULT or IChangelist.UNKNOWN.
     * Using IChangelist.DEFAULT causes a RequestException to be thrown.
     */
    public Changelist createNewChangelistImpl(IServer server, IClient client, String chgDescr, int changelistIdType) {

        boolean useDefaultId = false;
        if (changelistIdType == IChangelist.DEFAULT) {
            useDefaultId = true;
        }
        return (createNewChangelistImpl(server, client, chgDescr, useDefaultId));
    }


    /**
     * Populates a changelistImpl allowing choice of using IChangelist.DEFAULT or not. Setting useDefaultId=true causes
     * a RequestException to be thrown.
     */
    public Changelist createNewChangelistImpl(IServer server, IClient client, String chgDescr, boolean useDefaultId) {

        int cId = IChangelist.DEFAULT;
        if (useDefaultId == false) {
            cId = IChangelist.UNKNOWN;
        }
        Changelist changeListImpl = null;
        try {
            changeListImpl = new Changelist(
                    cId,
                    client.getName(),
                    userName,
                    ChangelistStatus.NEW,
                    new Date(),
                    chgDescr,
                    false,
                    (Server) server
            );
        } catch (Exception exc) {
            System.err.println("Unexpected Exception when setting changelist: " + exc.getLocalizedMessage());
            fail("Unexpected Exception when setting changelist: " + exc.getLocalizedMessage());
        }

        debugPrint("Created Changelist (-1=UNKNOWN 0=DEFAULT): " + cId);
        return changeListImpl;
    }

    /**
     * Creates a changelist based on populating a changelistImpl. useDefaultId should always be false since it
     * sets id=IChangelist.UNKNOWN . useDefaultId=true will cause a RequestException to be thrown.
     */
    public IChangelist createTestChangelist(IServer server, IClient client, String chgDescr, boolean useDefaultId) throws
            ConnectionException, RequestException, AccessException {

        Changelist changeListImpl = createNewChangelistImpl(server, client, chgDescr, useDefaultId);
        IChangelist changelist = client.createChangelist(changeListImpl);

        debugPrint("Created Changelist ID: " + changelist.getId());

        return changelist;
    }

    public boolean verifyChangelistId(List<IFileSpec> fSpecs, int expId, int compareType) {

        int id = -1;
        boolean matched = false;
        if (fSpecs.size() > 0) {
            for (IFileSpec fileSpec : fSpecs) {
                if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                    id = fileSpec.getChangelistId();
                    debugPrint("FileSpec.getChangelistId() " + id);
                    if (compareType == P4JTEST_COMPARE_EXACT) {
                        assertEquals("Changelist Ids should match exactly.", expId, id);
                    } else if (compareType == 1) {
                        assertTrue("Changelist Id should be greater ", expId > id);
                    } else {
                        assertTrue("Changelist Id should be less than ", expId < id);

                    }
                }
            }
        }


        return matched;
    }

    public void verifyChangelistStatus(IChangelist changelist, ChangelistStatus expChgStatus) {

        assertNotNull("Changelist should not be Null.", changelist);

        ChangelistStatus chgStatus = changelist.getStatus();
        debugPrint("expChgStatus: " + expChgStatus, "changelist.getStatus()" + chgStatus);
        assertEquals("Changelist Status not what expected.", expChgStatus, chgStatus);

    }

    /**
     * Verifies that the expected FileAction is associated with the expected number of fileSpecs.
     */
    private void verifyFileAction(List<IFileSpec> newFSList, int expNumWithAction, FileAction fAction) {

        int validFSpecCount = 0;

        debugPrint("** verifyFileAction **");
        if (newFSList.size() > 0) {
            for (IFileSpec fileSpec : newFSList) {
                if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                    assertEquals("Expected FileSpec Action: " + fAction, fAction, fileSpec.getAction());
                    validFSpecCount++;
                }
                if (fileSpec != null) {
                    debugPrint("fileSpec: " + fileSpec, "fileSpec.getAction() " + fileSpec.getAction(),
                            "fileSpec.getOpStatus(): " + fileSpec.getOpStatus());
                    debugPrint("fileSpec: " + fileSpec, "StatusMsg: " + fileSpec.getStatusMessage());
                }
            }
        }
        debugPrint("VerifyFileAction - " + fAction + " : ", "expNumWithAction: " + expNumWithAction, "validFSpecCount: " + validFSpecCount);
        assertEquals("Action - " + fAction + " not found on expected number of files.", expNumWithAction, validFSpecCount);

    }


    /**
     * Adds and Submits the passed in fileSpecs and returns the list of submitted fileSpecs. If
     * Does not reopen files after the submit.
     */
    public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType, boolean validOnly) throws Exception{

        return taskAddSubmitTestFiles(server, fNameList, fileType, validOnly, false);
    }

    /**
     * Adds and Submits the passed in fileSpecs and returns the list of submitted fileSpecs. If
     * reopenAfterSubmit is true, then returns the fileSpecs in 'opened' state.
     */
    public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType,
                                                  boolean validOnly, boolean reopenAfterSubmit) throws Exception{

        IClient client = null;
        List<IFileSpec> submittedFiles = null;

            client = server.getClient(defaultTestClientName);
            server.setCurrentClient(client);
            assertNotNull("Null client returned.", client);

            //create a changelist
            IChangelist changelist = createTestChangelist(server, client,
                    "Changelist to submit files for " + getName(), false);

            //add the test files
            List<IFileSpec> testFiles = addTestFiles(client, fNameList,
                    changelist.getId(), validOnly, fileType);
            assertNotNull("testFiles should not be Null.", testFiles);

            //submit files
            changelist.update();
            submittedFiles = changelist.submit(reopenAfterSubmit);
            assertNotNull("submittedFiles should not be Null.", submittedFiles);


        return submittedFiles;

    }

    /**
     * Given a String[] of testFiles, makes a valid fileSpec list and then marks the files for 'add
     * using the client.addFiles() method. Uses the passed in filetype, the Default Changelist and returns
     * either VALID, INVALID or ALL fileSpecs.
     */
    public List<IFileSpec> addTestFiles(IClient client, String[] testFiles, boolean validOnly, String fileType)
            throws ConnectionException, AccessException {

        int changelistId = 0;

        return (addTestFiles(client, testFiles, changelistId, validOnly, fileType));
    }


    /**
     * Given a String[] of testFiles, makes a valid fileSpec list and then marks the files for 'add
     * using the client.addFiles() method. Uses the Default Changelist and returns either
     * VALID, INVALID or ALL fileSpecs.
     */
    public List<IFileSpec> addTestFiles(IClient client, String[] testFiles, boolean validOnly)
            throws ConnectionException, AccessException {

        int changelistId = 0;

        return (addTestFiles(client, testFiles, changelistId, validOnly, P4JTEST_FILETYPE_TEXT));
    }


    /**
     * Given a String testFiles, makes a valid fileSpec list and then marks the files for 'add
     * using the client.addFiles() method. Uses the Default Changelist and returns either
     * VALID, INVALID or ALL fileSpecs.
     */
    private List<IFileSpec> addTestFiles(IClient client, String testFile,
                                         int changelistID, boolean validOnly, String fileType)
            throws ConnectionException, AccessException {

        final String[] testFiles = {
                testFile
        };

        return (addTestFiles(client, testFiles, changelistID, validOnly, fileType));
    }


    /**
     * Given a String[] of testFiles, makes a valid fileSpec list and then marks the files for 'add
     * using the client.addFiles() method. Uses the passed in changelistId and returns either
     * VALID, INVALID or ALL fileSpecs.
     */
    public List<IFileSpec> addTestFiles(IClient client, String[] testFiles,
                                        int changelistID, boolean validOnly, String fileType)
            throws ConnectionException, AccessException {

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(testFiles);
        assertNotNull("FileSpecBuilder unexpectedly returned Null SpecList.", fList);
        assertFalse("File List should not be empty.", fList.isEmpty());
        assertEquals("Number of FileList entries should equal original number of files.",
                fList.size(), testFiles.length);

        List<IFileSpec> newAddedSpecList = client.addFiles(fList, false, changelistID, fileType, true);

        if (validOnly) {
            return FileSpecBuilder.getValidFileSpecs(newAddedSpecList);
        } else {
            return newAddedSpecList;
        }

    }

    /**
     * Verifies the expected FileSpecOpStatus and expected StatusMessage match on this fileSpec.
     * StatusMsg is verified to be contained in the returned message, not as an exact match.
     * Useful to verify error info.
     */
    private void verifyFileSpecInfo(List<IFileSpec> fSpecs, FileSpecOpStatus expOpStatus, String expMsg) {

        boolean messageFound = false;
        boolean opStatusFound = false;
        FileSpecOpStatus opStatus = null;
        String msg = null;

        debugPrint("\n**** verifyFileSpecInfo ****");
        if (fSpecs != null) {
            for (IFileSpec fileSpec : fSpecs) {
                if (fileSpec != null) {
                    debugPrint("Verifying Info on fileSpec: " + fileSpec);
                    opStatus = fileSpec.getOpStatus();
                    debugPrint("Action: " + fileSpec.getAction());
                    if (opStatus == expOpStatus) {
                        opStatusFound = true;
                        if (messageFound == false) {
                            msg = fileSpec.getStatusMessage();
                            if (msg != null) {
                                messageFound = msg.contains(expMsg);
                            } else if (msg == null && expMsg == null) {
                                messageFound = true;
                            }
                        }
                    }
                    debugPrint("OpStatus: " + opStatus, "StatusMsg: " + msg);
                }
            }
        }
        assertTrue("The FileSpec's OpStatus did not match the expected value.", opStatusFound);
        assertTrue("The FileSpec's Msg did not contain the expected string.", messageFound);

    }


    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }

}