package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.apache.commons.lang3.StringUtils;

//import org.junit.BeforeClass;

/**
 * Tests Edit->Submit scenarios from end-to-end.
 */
@TestId("ClientEditSubmitE2ETest01")
public class ClientEditSubmitE2ETest extends P4JavaTestCase {

    private static IClient client = null;
    private static String clientDir;
    private static String sourceFile;

    @BeforeClass
    public static void beforeAll() throws Exception {
        server = getServer();
        client = getDefaultClient(server);
        clientDir = defaultTestClientName + File.separator + testId;
        server.setCurrentClient(client);
        sourceFile = client.getRoot() + File.separator + textBaseFile;
        createTestSourceFile(sourceFile, false);
    }


    /**
     * This tests that you can use the fileSpec returned from addFiles+submit in
     * the editFiles method. Add->Submit->Edit[submittedFSpecs].
     */
    @Test
    public void testAddImmediatelyEditE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testAddImmediatelyEditE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        String newBaseFile = prepareTestFile(sourceFile, newFile, true);
        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + newBaseFile)
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        editedFiles = taskEditSubmitTestFiles(server, filePaths, P4JTEST_RETURNTYPE_VALIDONLY, P4JTEST_FILETYPE_TEXT);
        validSpecCnt = countValidFileSpecs(editedFiles);
        assertTrue("editedFiles should be greater than zero. ", validSpecCnt > 0);

        verifyFilesAdded(submittedFiles, filePaths.length);
        verifyTestFilesSubmitted(submittedFiles, filePaths.length);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);

    }


    /**
     * Verifies Add->Submit->Reopen->Edit=Error[AlreadyOpen]
     */
    @Test
    public void testAddSubmitReopenEditAlreadyOpenErrE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testAddSubmitReopenEditAlreadyOpenErrE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
        assertNotNull("FileSpecBuilder unexpectedly returned Null SpecList.", fList);
        assertFalse("File List should not be empty.", fList.isEmpty());

        client = server.getClient(getPlatformClientName(defaultTestClientName));
        editedFiles = editTestFiles(client, server, 0, fList, P4JTEST_RETURNTYPE_ALL);
        dumpFileSpecInfo(editedFiles);

        verifyFileSpecInfo(editedFiles, FileSpecOpStatus.INFO, "currently opened for edit");

    }


    /**
     * Create file, Add->Submit(reOpen)->Edit->Submit()
     */
    @Test
    public void testSubmitEditE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testSubmitEditE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);

        assertNotNull("client should not be Null.", client);

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
        editedFiles = taskEditNewChangelistSubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT);

        validSpecCnt = countValidFileSpecs(editedFiles);
        dumpFileSpecInfo(editedFiles);

        verifyFilesAdded(submittedFiles, filePaths.length);
        verifyTestFilesSubmitted(editedFiles, filePaths.length);
        verifyTestFileRevision(editedFiles, 1, 2);
    }

    /**
     * Create file, Add->Submit(reOpen)->6x(Edit->Submit())
     */
    @Test
    public void testSubmitEdit6RevsE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testSubmitEdit6RevsE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);

        assertNotNull("client should not be Null.", client);

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
        editedFiles = taskEditNewChangelistSubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT);
        verifyTestFileRevision(editedFiles, 1, 2);
        for (int i = 0; i <= 3; i++) {

            editedFiles = taskEditSubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT);
            validSpecCnt = countValidFileSpecs(editedFiles);
            dumpFileSpecInfo(editedFiles);

            verifyFilesAdded(submittedFiles, filePaths.length);
            verifyTestFilesSubmitted(editedFiles, filePaths.length);
            verifyTestFileRevision(editedFiles, 1, 3 + i);
        }
    }


    /**
     * Submitting valid file through fileSpecbuilder to edit.
     * Add->Submit(reOpen)->Edit. Leaves files opened for edit.
     */
    @Test
    public void testSubmitReopenEditE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;

        debugPrintTestName("testSubmitReopenEditE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
        editedFiles = editTestFiles(client, server, 0, fList, P4JTEST_RETURNTYPE_ALL);
        dumpFileSpecInfo(editedFiles);

        verifyFileSpecInfo(editedFiles, FileSpecOpStatus.INFO, "currently opened for edit");
    }


    /**
     * Create file, Add->Submit(noReopen)->Edit(reOpen)=Error(NotOpened).
     * Error verified through p4 commandline.
     */
    @Test
    public void testSubmitEditReopenNotOpenedErrE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testSubmitEditReopenNotOpenedErrE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, false);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);


        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
        editedFiles = taskEditNewChangelistSubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT);
        verifyFileSpecInfo(editedFiles, FileSpecOpStatus.ERROR, "No files to submit.");

        validSpecCnt = countValidFileSpecs(editedFiles);
        dumpFileSpecInfo(editedFiles);

        verifyFilesAdded(submittedFiles, filePaths.length);
    }


    /**
     * Create file, Add->Submit(reOpen)->Edit=Error(can't change from Default Changelist)
     */
    @Test
    public void testSubmitEditNeedReopenErrE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;

        debugPrintTestName("testSubmitEditNeedReopenErrE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        //create file, add and submit it, then reopen in default changelist
        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);


        client = server.getClient(getPlatformClientName(defaultTestClientName));
        server.setCurrentClient(client);
        assertNotNull("Null client returned", client);

        //create a new changelist
        IChangelist changelist = createTestChangelist(server, client,
                "Changelist to test error " + getName());

        //FIXME: Can remove FileSpecBuilder and send in submittedFiles once bug fixed.
        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);

        //send the files for edit and send in the new changelist id, but do not use reopen as you should
        editedFiles = editTestFiles(client, server, changelist.getId(), fList, P4JTEST_RETURNTYPE_ALL);
        dumpFileSpecInfo(editedFiles);
        verifyFileSpecInfo(editedFiles, FileSpecOpStatus.INFO, "can't change from default change - use 'reopen'");
    }


    /**
     * Create file, Add->Submit(reOpen)->Edit->Modify->Submit()
     */
    @Test
    public void testSubmitEditModifyE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;

        debugPrintTestName("testSubmitEditModifyE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String testText = "This text was added from " + getName();
        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
        String prepareFile = prepareTestFile(sourceFile, newFile, true);
        final String[] fileLocalPaths = {
                clientRoot + File.separator + testId + File.separator + prepareFile
        };
        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareFile)
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(fileLocalPaths);
        editedFiles = taskEditModifySubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL,
                P4JTEST_FILETYPE_TEXT, testText);
        validSpecCnt = countValidFileSpecs(editedFiles);
        dumpFileSpecInfo(editedFiles);

        verifyFilesAdded(submittedFiles, filePaths.length);
        verifyTestFilesSubmitted(editedFiles, filePaths.length);
        verifyTestFileRevision(editedFiles, 1, 2);
        boolean filesMatch = localSystemFileCompare(sourceFile, fileLocalPaths[0]);
        assertFalse("Source and Modified file should differ", filesMatch);
    }


    /**
     * Create file, Add->Submit(reOpen)->Edit->Modify->Submit->Revert()=Error(files not opened)
     */
    @Test
    public void testSubmitEditModifyRevertNotOpenedErrE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testSubmitEditModifyRevertNotOpenedErrE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String testText = "This text was added from " + getName();
        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        String prepareFile = prepareTestFile(sourceFile, newFile, true);
        final String[] fileLocalPaths = {
                clientRoot + File.separator + testId + File.separator + prepareFile
        };
        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareFile)
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);

        client = server.getClient(getPlatformClientName(defaultTestClientName));

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(fileLocalPaths);
        editedFiles = taskEditModifySubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL,
                P4JTEST_FILETYPE_TEXT, testText);
        validSpecCnt = countValidFileSpecs(editedFiles);
        dumpFileSpecInfo(editedFiles);

        verifyFilesAdded(submittedFiles, filePaths.length);
        verifyTestFilesSubmitted(editedFiles, filePaths.length);
        verifyTestFileRevision(editedFiles, 1, 2);
        boolean filesMatch = localSystemFileCompare(sourceFile, fileLocalPaths[0]);
        assertFalse("Source file and submitted file should differ.", filesMatch);
        List<IFileSpec> revertedFiles = revertTestFiles(client, fList, -999, FileAction.EDIT, 0);
        verifyFileSpecInfo(revertedFiles, FileSpecOpStatus.ERROR, "file(s) not opened on this client");
    }

    /**
     * Create file, Add->Submit(reOpen)->Edit->Modify->Submit->Edit->Modify->Revert()
     */
    @Test
    public void testSubmitEditModifyRevert() throws Exception {

    }


    /**
     * Create file, Add->Submit(reOpen)->Edit->Submit->Edit->Submit
     */
    @Test
    public void testEditReopenSubmitEditSubmitE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        debugPrintTestName("testEditReopenWithFileRevNotationE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertTrue("validSpecs should be greater than zero. ", validSpecCnt > 0);
        verifyTestFileRevision(submittedFiles, filePaths.length, 1);

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
        editedFiles = taskEditNewChangelistSubmitTestFiles(server, fList, P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT);
        verifyTestFileRevision(editedFiles, 1, 2);

        validSpecCnt = countValidFileSpecs(editedFiles);
        dumpFileSpecInfo(editedFiles);

        verifyTestFileRevision(editedFiles, 1, 2);
        verifyFilesAdded(submittedFiles, filePaths.length);
        verifyTestFilesSubmitted(editedFiles, filePaths.length);
    }


    /**
     * Create file, Add->Submit(reOpen)->ReOpen=Error(Rev specs not allowed)
     * Should return error. Note: Error text and status code have been verified at server.
     */
    @Test
    public void testEditDirectReopenWithFileRevsErrE2E() throws Exception {

        int changelistId = -1;
        int expLength = 0;

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        //FIXME: Please rename this test since we don't use file revs!!! -Tanya
        debugPrintTestName("testEditDirectReopenWithFileRevsE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
        };

        expLength = filePaths.length;

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertEquals("validSpecs should be equal to number of original files ", 1, validSpecCnt);
        verifyTestFileRevision(submittedFiles, expLength, 1);

        server.setCurrentClient(client);

        //create a changelist
        IChangelist changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        //add the test files
        changelistId = changelist.getId();
        testFiles = client.reopenFiles(submittedFiles, changelistId, P4JTEST_FILETYPE_TEXT);

        dumpFileSpecInfo(testFiles);
    }

    /**
     * Create file, Add file, Submit(reOpen), edit the returned files as is...
     * Should return error, but we should recover from it by cleaning the fileSpecs.
     */
    @Test
    public void testEditReopenErrTryAgainE2E() throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> editedFiles = null;
        List<IFileSpec> testFiles = null;
        int changelistId = Changelist.UNKNOWN;
        int expLength = 0;
        debugPrintTestName("testEditReopenErrTryAgainE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
        };
        List<IFileSpec> fSpecList = FileSpecBuilder.makeFileSpecList(filePaths);

        expLength = filePaths.length;

        submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        int validSpecCnt = countValidFileSpecs(submittedFiles);
        assertEquals("validSpecs should be equal to number of original files ", 1, validSpecCnt);
        verifyTestFileRevision(submittedFiles, expLength, 1);
        verifyFilesAdded(submittedFiles, expLength);

        server.setCurrentClient(client);

        //create a changelist
        IChangelist changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        //add the test files
        changelistId = changelist.getId();

        editedFiles = taskEditNewChangelistSubmitTestFiles(server, fSpecList, P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT);
        dumpFileSpecInfo(editedFiles);

        //Here is the error
        testFiles = client.reopenFiles(fSpecList, changelistId, P4JTEST_FILETYPE_TEXT);
        dumpFileSpecInfo(testFiles);
        editedFiles = taskEditSubmitTestFiles(server, fSpecList, P4JTEST_RETURNTYPE_VALIDONLY, P4JTEST_FILETYPE_TEXT);
        validSpecCnt = countValidFileSpecs(editedFiles);
        dumpFileSpecInfo(editedFiles);

        verifyTestFilesSubmitted(editedFiles, filePaths.length);
        verifyTestFileRevision(editedFiles, 1, 3);
    }


    /**
     * Create file, Add file, Submit(reOpen), edit the returned files as is...
     * Should return error, but we should recover from it by cleaning the fileSpecs.
     * Recreates this error: "Usage: reopen [-c changelist#] [-t type] files...
     * Missing/wrong number of arguments."
     */
    @Test
    public void testEditReopenFilesUsingErrorSpecE2E() throws Exception {

        List<IFileSpec> editedFiles = null;
        List<IFileSpec> testFiles = null;
        debugPrintTestName("testEditReopenFilesUsingErrorSpecE2E");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";

        final String[] filePaths = {
                new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath()
        };

        //add the test files
        List<IFileSpec> fSpecList = FileSpecBuilder.makeFileSpecList(filePaths);
        testFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, true, true);
        dumpFileSpecInfo(testFiles);

        editedFiles = taskEditModifySubmitTestFiles(server, fSpecList,
                P4JTEST_RETURNTYPE_ALL, P4JTEST_FILETYPE_TEXT, "Added to verify we can recover from errors");

        //This is just here so the test fails.
        verifyTestFilesSubmitted(testFiles, filePaths.length);
        verifyTestFilesSubmitted(editedFiles, filePaths.length);
    }

    //*************************//
    //   Helper Functions      //
    //*************************//

    /**
     * Verifies the expected FileSpecOpStatus and expected StatusMessage match on this fileSpec.
     * StatusMsg is verified to be contained in the returned message, not as an exact match.
     * Useful to verify error info.
     */
    private void verifyFileSpecInfo(List<IFileSpec> fSpecs, FileSpecOpStatus expOpStatus, String expMsg) {
        int validFSpecCount = 0;
        int errorFSpecCount = 0;
        int infoFSpecCount = 0;
        FileSpecOpStatus opStatus = null;
        String msg = null;

        debugPrint("** verifyFileSpecInfo **");
        if (fSpecs != null) {
            for (IFileSpec fileSpec : fSpecs) {
                if (fileSpec != null) {
                    opStatus = fileSpec.getOpStatus();
                    msg = fileSpec.getStatusMessage();
                    if (expOpStatus == FileSpecOpStatus.VALID) {
                        debugPrint("DumpInfo on fileSpec: " + fileSpec, "OpStatus: " + expOpStatus, "Action: " + fileSpec.getAction());
                        validFSpecCount++;
                    } else if (expOpStatus == FileSpecOpStatus.INFO) {
                        debugPrint("DumpInfo on fileSpec: " + fileSpec, "OpStatus: " + expOpStatus, "StatusMsg: " + expMsg);
                        infoFSpecCount++;
                    } else if (expOpStatus == FileSpecOpStatus.ERROR) {
                        debugPrint("DumpInfo on fileSpec: " + fileSpec, "OpStatus: " + expOpStatus, "StatusMsg: " + expMsg);
                        errorFSpecCount++;
                    }
                }
            }
        }
        debugPrint("Valid FileSpecs: " + validFSpecCount, "Info FileSpecs: " + infoFSpecCount, "Error FileSpecs: " + errorFSpecCount);
        assertEquals("The FileSpec's OpStatus did not match the expected value.", expOpStatus, opStatus);
        assertTrue("The FileSpec's Msg did not contain the expected string.", msg.contains(expMsg));

    }


    /**
     * Returns the number of valid fileSpecs for the passed in List of FileSpecs.
     */
    public int countValidFileSpecs(List<IFileSpec> fileSpecs) {

        int validFSpecCount = 0;

        if (fileSpecs != null) {
            if (fileSpecs.size() > 0) {
                for (IFileSpec fileSpec : fileSpecs) {
                    if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                        validFSpecCount++;
                    }
                }
            }
        }

        debugPrint("Valid Spec Count : " + validFSpecCount);

        return validFSpecCount;

    }

    public List<IFileSpec> taskEditNewChangelistSubmitTestFiles(IServer server, String[] filePaths, int returnType, String fileType) throws Exception {

        List<IFileSpec> editFSpecs = FileSpecBuilder.makeFileSpecList(filePaths);

        return taskEditNewChangelistSubmitTestFiles(server, editFSpecs, returnType, fileType);
    }

    /**
     * ReOpens files for Edit (client.reopenFiles()) in numbered changelist and Submits the passed in fileSpecs and returns the list of VALID submitted fileSpecs.
     * No actual action on the file is taken after opening for Edit before submitting.
     */
    public List<IFileSpec> taskEditNewChangelistSubmitTestFiles(IServer server, List<IFileSpec> editFSpecs, int returnType, String fileType) throws Exception {

        IClient client = null;
        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;

        client = server.getClient(getPlatformClientName(defaultTestClientName));
        server.setCurrentClient(client);
        assertNotNull("Null client returned", client);

        //create a changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit eidted files " + getName());
        //add the test files
        dumpFileSpecInfo(editFSpecs);
        testFiles = client.reopenFiles(editFSpecs, changelist.getId(), fileType);

        assertNotNull("FileSpec testFiles returned Null!!", testFiles);
        assertFalse("FileSpec testFiles should not be empty.", testFiles.isEmpty());
        dumpFileSpecInfo(testFiles);
        //submit files

        submittedFiles = changelist.submit(false);
        assertNotNull("submittedFiles should not be Null!!", submittedFiles);

        return submittedFiles;
    }

    /**
     * Opens files for Edit and Submits the passed in fileSpecs and returns the list of submitted fileSpecs.
     * No actual modification of the file takes place before submitting.
     */
    public List<IFileSpec> taskEditSubmitTestFiles(IServer server, String[] filePaths, int returnType, String fileType) throws Exception {

        List<IFileSpec> editFSpecs = FileSpecBuilder.makeFileSpecList(filePaths);

        return taskEditSubmitTestFiles(server, editFSpecs, false, returnType, fileType);
    }


    /**
     * Opens files for Edit and Submits the passed in fileSpecs and returns the list of submitted fileSpecs.
     * No actual modification of the file takes place before submitting.
     */
    public List<IFileSpec> taskEditSubmitTestFiles(IServer server, List<IFileSpec> editFSpecs, int returnType, String fileType) throws Exception {

        return taskEditSubmitTestFiles(server, editFSpecs, false, returnType, fileType);
    }


    /**
     * Opens files for Edit and Submits the passed in fileSpecs and returns the list of submitted fileSpecs.
     * No actual modification of the file takes place before submitting.
     */
    public List<IFileSpec> taskEditSubmitTestFiles(IServer server, List<IFileSpec> editFSpecs, boolean useDefaultChangelist,
                                                   int returnType, String fileType) throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;
        //create a changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit eidted files " + getName());

        //add the test files
        if (useDefaultChangelist) {
            testFiles = editTestFiles(client, server, IChangelist.DEFAULT,
                    editFSpecs, returnType);
            changelist.setId(IChangelist.DEFAULT);
            debugPrint("Getting Default Changelist Info", "ID: " + changelist.getId(), "Files: " + changelist.getFiles(true));
        } else {
            dumpFileSpecInfo(testFiles, "Reopened FileSpecs");
            testFiles = editTestFiles(client, server, changelist.getId(),
                    editFSpecs, returnType);
        }
        assertNotNull("testFiles returned Null!!", testFiles);
        dumpFileSpecInfo(testFiles, "Edited TestFiles");
        verifyFileAction(testFiles, countValidFileSpecs(editFSpecs), FileAction.EDIT);

        submittedFiles = changelist.submit(false);
        assertNotNull("submittedFiles should not be Null!!", submittedFiles);


        return submittedFiles;
    }


    /**
     * Creates a changelist, reopens passed in fileSpecs using client.reopenFiles(), writes testText
     * to files, closes files, and finally submits files.
     */
    public List<IFileSpec> taskEditModifySubmitTestFiles(IServer server, List<IFileSpec> editFSpecs,
                                                         int returnType, String fileType, String testText) throws Exception {

        List<IFileSpec> submittedFiles = null;
        List<IFileSpec> testFiles = null;
        IChangelist changelist = null;
        //create a changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        //add the test files
        testFiles = client.reopenFiles(editFSpecs, changelist.getId(), fileType);
        writeToTestFiles(FileSpecBuilder.getValidFileSpecs(editFSpecs), testText);
        assertNotNull("FileSpec testFiles returned Null!!", testFiles);
        assertFalse("FileSpec testFiles should not be empty.", testFiles.isEmpty());

        //submit files
        submittedFiles = changelist.submit(false);
        assertNotNull("submittedFiles should not be Null!!", submittedFiles);


        return submittedFiles;
    }

    /**
     * Opens a file and appends the testText to that file. Returns the number
     * of FILES acted on. Does not submit the files.
     */
    public int writeToTestFiles(List<IFileSpec> fileSpecs, String testText) throws IOException {

        BufferedWriter bw = null;
        int numWritten = 0;

        try {
            for (IFileSpec fSpec : fileSpecs) {
                debugPrint("getClientPathString: " + fSpec.getClientPathString(), "getLocalPathString: " + fSpec.getLocalPath(),
                        "getOriginalPathString: " + fSpec.getOriginalPathString());
                bw = new BufferedWriter(new FileWriter(fSpec.getOriginalPathString(), true));

                debugPrint("** writeToTestFiles **", "adding line: " + testText, " to file: " + fSpec);
                bw.write(testText);
                bw.newLine();
                bw.close();
                numWritten++;
            }
        } finally {
            if (bw != null) {
                bw.close();
            }
        }

        return numWritten;

    }

    /**
     * Sync the test fileSpecs using client.sync(). Only valid fileSpecs will be sync'ed using
     * forced update.
     * client.sync(fSpecs, forceUpdate=true, noUpdate=false, clientBypass = false, serverBypass=false).
     */
    public List<IFileSpec> syncTestFiles(List<IFileSpec> submittedFiles, IClient client) throws Exception {

        List<IFileSpec> syncFiles = null;
        syncFiles = client.sync(
                FileSpecBuilder.getValidFileSpecs(submittedFiles),
                true, false, false, false);
        assertNotNull("syncFiles should not be Null!!", syncFiles);

        return syncFiles;
    }

    /**
     * Adds and Submits the passed in fileSpecs and returns the list of submitted fileSpecs. If
     * Does not reopen files after the submit.
     */
    public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType, boolean validOnly) throws Exception {

        return taskAddSubmitTestFiles(server, fNameList, fileType, validOnly, false);
    }

    /**
     * Adds and Submits the passed in fileSpecs and returns the list of submitted fileSpecs. If
     * reopenAfterSubmit is true, then returns the fileSpecs in 'opened' state.
     */
    public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType,
                                                  boolean validOnly, boolean reopenAfterSubmit) throws Exception {

        IClient client = null;
        List<IFileSpec> submittedFiles = null;

        client = server.getClient(getPlatformClientName(defaultTestClientName));
        server.setCurrentClient(client);
        assertNotNull("Null client returned.", client);

        //create a changelist
        IChangelist changelist = createTestChangelist(server, client,
                "Changelist to submit files for " + getName());

        //add the test files
        List<IFileSpec> testFiles = addTestFiles(client, fNameList,
                changelist.getId(), validOnly, fileType);
        assertNotNull("testFiles should not be Null.", testFiles);

        //submit files
        submittedFiles = changelist.submit(reopenAfterSubmit);
        assertNotNull("submittedFiles should not be Null.", submittedFiles);


        return submittedFiles;

    }


    /**
     * Opens the passed in fileSpecs for edit and returns valid opened fileSpecs only. ChangelistID=0.
     * No modification to the file occurs.
     */
    public List<IFileSpec> editTestFiles(IClient client, IServer server,
                                         List<IFileSpec> fileSpecs) throws RequestException, ConnectionException, AccessException {
        int changelistID = 0;

        return (editTestFiles(client, server, changelistID, fileSpecs, P4JTEST_RETURNTYPE_VALIDONLY));

    }

    /**
     * Open the passed in fileSpecs for edit and returns opened fileSpecs. No modification to the file occurs.
     * Param returnType determines if VALID, INVALID or ALL fileSpecs are returned.
     * local enums: P4JTEST_RETURNTYPE_VALIDONLY, P4JTEST_RETURNTYPE_INVALIDONLY, P4JTEST_RETURNTYPE_ALL
     */
    public List<IFileSpec> editTestFiles(IClient client, IServer server, int changelistID,
                                         List<IFileSpec> fileSpecs, int returnType) throws RequestException, ConnectionException, AccessException {

        List<IFileSpec> allFiles = null;
        //List<IFileSpec> openedFiles = null;

        allFiles = client.editFiles(fileSpecs, false, false, changelistID, fileSpecs.get(0).getFileType());
        assertNotNull("EditFiles returned Null.", allFiles);

        if (returnType == P4JTEST_RETURNTYPE_VALIDONLY) {
            return (FileSpecBuilder.getValidFileSpecs(allFiles));
        } else if (returnType == P4JTEST_RETURNTYPE_INVALIDONLY) {
            return (FileSpecBuilder.getInvalidFileSpecs(allFiles));
        } else {
            return allFiles;
        }

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
     * Create a new ChangelistImpl given the passed in parameters. Uses
     * changelistId=IChangelist.UNKNOWN.
     */
    public Changelist createNewChangelistImpl(IServer server, IClient client, String chgDescr) {

        Changelist changeListImpl = null;
        changeListImpl = new Changelist(
                IChangelist.UNKNOWN,
                client.getName(),
                userName,
                ChangelistStatus.NEW,
                new Date(),
                chgDescr,
                false,
                (Server) server
        );

        return changeListImpl;
    }


    /**
     * Create a new Changelist given the passed in parameters. First creates a new
     * changelistImpl. Uses changelistId=IChangelist.UNKNOWN.
     */
    public IChangelist createTestChangelist(IServer server, IClient client, String chgDescr) throws
            ConnectionException, RequestException, AccessException {

        Changelist changeListImpl = createNewChangelistImpl(server, client, chgDescr);
        IChangelist changelist = client.createChangelist(changeListImpl);

        debugPrint("Created Changelist ID: " + changelist.getId());

        return changelist;
    }


    /**
     * Returns the current OS this test class is running on.
     */
    public String getCurrentOsName() {

        String currOsName = getProperty("os.name");

        return (currOsName);

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
     * Revert the fileSpecs in the specified changelist. If the changelist is -999, then first get the changelist.
     * Verify that the expected number of fileSpecs is reverted and that the expected FileAction is the result.
     */
    private List<IFileSpec> revertTestFiles(IClient cl, List<IFileSpec> newFSList, int chgListID, FileAction expAction, int expNumToRevert) throws AccessException, ConnectionException {

        int numReverted = 0;
        List<IFileSpec> revertedFiles = null;

        //revert the files
        if (chgListID == -999) {
            getTestFileSpecChangelistId(newFSList);
        }
        if (newFSList != null) {
            revertedFiles = cl.revertFiles(newFSList, false, chgListID, false, false);

            //check to make sure they are actually reverted. Also, if a valid fileSpec is passed in
            //will count the valid occurrences
            if (newFSList.size() > 0) {
                for (IFileSpec fileSpec : revertedFiles) {
                    if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                        numReverted++;
                        assertEquals("Expected FileSpec Action " + expAction, fileSpec.getAction(), expAction);
                    }
                    if (fileSpec != null) {
                        debugPrint("fileSpec: " + fileSpec, "fileSpec.getAction(): " + fileSpec.getAction(),
                                "fileSpec.getOpStatus(): " + fileSpec.getOpStatus());
                    }
                }
                debugPrint("expNumToRevert: " + expNumToRevert, "numReverted: " + numReverted);
            }
        }
        assertEquals("Number files reverted does not equal number expected.", expNumToRevert, numReverted);

        return revertedFiles;
    }

    /**
     * Returns the (valid) fileSpec's changelist id.
     */
    private int getTestFileSpecChangelistId(List<IFileSpec> fSpecList) {

        int changelistId = -1;
        if (fSpecList != null) {
            for (IFileSpec fSpec : fSpecList) {
                if (fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                    changelistId = fSpec.getChangelistId();
                }
            }
        }

        debugPrint("Returning ChangelistId: " + changelistId);
        return changelistId;

    }

    /**
     * This function checks the system we are on, and uses the local diff utility to
     * make sure the files are exactly the same. Returns 'true' if the files match, false
     * if they don't. Fails with an error message if there is a problem.
     */
    private boolean localSystemFileCompare(String fName1, String fName2) throws Exception {

        boolean filesMatch = false;
        String cmd = null;
        String line = "";
        String errLine = "";
        String outLine = "";
        String diffStr = "";
        debugPrint("Comparing files: ", fName1, fName2);

        String osName = getCurrentOsName();
        if (osName.toLowerCase().contains("windows")) {
            cmd = "FC " + fName1 + " " + fName2;
            diffStr = "*****";
        } else {
            //what is it on Unix???
            cmd = "diff -q " + fName1 + " " + fName2;
            diffStr = "differ";
        }

        Process p = Runtime.getRuntime().exec(cmd);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        // read the output from the command
        while ((line = stdError.readLine()) != null) {
            errLine += line;
        }
        if (!(errLine.length() == 0)) {
            debugPrint("The following error was returned: " + errLine);
            fail("Unexpected Error: " + errLine);
        }

        line = "";
        while ((line = stdInput.readLine()) != null) {
            outLine += line;
        }
        debugPrint("STDOUT: " + outLine);
        if (!outLine.toLowerCase().contains(diffStr)) {
            filesMatch = true;
        }
        debugPrint("Files Matched?: " + filesMatch);

        return (filesMatch);

    }


    /**
     * Verify that the expected number of files are in the FileAction=FileAction.ADD state.
     */
    private void verifyFilesAdded(List<IFileSpec> newFSList, int expNumAdded) {

        int validFSpecCount = 0;

        debugPrint("** verifyFilesAdded **");
        if (newFSList.size() > 0) {
            for (IFileSpec fileSpec : newFSList) {
                if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                    assertEquals("Expected FileSpec Action ADD.", fileSpec.getAction(), FileAction.ADD);
                    validFSpecCount++;
                }
                if (fileSpec != null) {
                    debugPrint("VerifyFilesAdded fileSpec: " + fileSpec, "fileSpec.getAction() " + fileSpec.getAction(),
                            "fileSpec.getOpStatus(): " + fileSpec.getOpStatus());
                }
            }
        }
        debugPrint("VerifyFilesAdded expNumAdded: " + expNumAdded, "validFSpecCount: " + validFSpecCount);
        assertEquals("Expected number of files not added.", expNumAdded, validFSpecCount);

    }

    /**
     * Verify that the expected number of files has been submitted by verifying the
     * FileSpecOpStatus.INFO has the message 'submitted' message. Also, ensure there
     * are no invalid FileSpecs.
     */
    private void verifyTestFilesSubmitted(List<IFileSpec> newFSList, int expNumSubmitted) {

        int validFSpecCount = 0;
        int invalidFSpecCount = 0;
        FileSpecOpStatus fSpecOpStatus = null;
        String fSpecMsg = "";
        String submitMsg = "Submitted as change";

        debugPrint("** verifyTestFilesSubmitted **");

        if (newFSList.size() > 0) {
            for (IFileSpec fileSpec : newFSList) {
                if (fileSpec != null) {
                    fSpecOpStatus = fileSpec.getOpStatus();
                    debugPrint("Submitted FileStatus: " + fileSpec, "" + fSpecOpStatus);
                    if (fSpecOpStatus == FileSpecOpStatus.VALID) {
                        validFSpecCount++;
                    } else if (fSpecOpStatus == FileSpecOpStatus.INFO) {
                        fSpecMsg = fileSpec.getStatusMessage();
                        if (StringUtils.isNumeric(fSpecMsg)) {
                            // The 2013.2 test P4d has a trigger which adds
                            // Triggers:
                            // example1 change-submit //depot/... "echo %changelist%"
                            // example2 change-submit //depot/... "echo %changelist%"
                            // example3 change-submit //depot/... "echo %changelist%"
                            // So we get 3 info messages - lets ignore them not sure if triggers are needed
                            debugPrint("Ignoring: " + fSpecMsg + " Think it was created with a trigger!");
                        } else {
                            assertTrue("Message should show file was submitted. ", fSpecMsg.toString().contains(submitMsg));
                        }
                    } else {
                        fSpecMsg = fileSpec.getStatusMessage();
                        invalidFSpecCount++;
                        debugPrint("FileSpecOp: " + fSpecOpStatus, "StatusMsg: " + fSpecMsg);
                    }
                }
                //debugPrint("Submitted File Operation Status: " + fileSpec, "" + fSpecOpStatus);
            }
        }
        debugPrint("Submitted expNumAdded: " + expNumSubmitted, "validFSpecCount: " + validFSpecCount);
        assertEquals("Expected number of files were not submitted.", expNumSubmitted, validFSpecCount);
        assertEquals("Expected number of invalid FileSpecs should be zero", 0, invalidFSpecCount);

    }


    /**
     * Verify that the expected number of files have the expected FileRevision.
     */
    private void verifyTestFileRevision(List<IFileSpec> newFSList, int expNumWithRev, int expRev) {

        int validFSpecCount = 0;

        debugPrint("** verifyTestFileRevision **");
        if (newFSList.size() > 0) {
            for (IFileSpec fileSpec : newFSList) {
                if (fileSpec != null) {
                    if (fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                        int endRev = fileSpec.getEndRevision();
                        debugPrint("FileRev: " + fileSpec, "" + expRev, "" + endRev);
                        assertEquals("Expected File Revision not found", expRev, endRev);
                        validFSpecCount++;
                    }
                }
            }
        }
        debugPrint("expNumWithRev: " + expNumWithRev, "validFSpecCount: " + validFSpecCount);
        assertEquals("Expected number of files with revision not found.", expNumWithRev, validFSpecCount);

    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }

}

	
