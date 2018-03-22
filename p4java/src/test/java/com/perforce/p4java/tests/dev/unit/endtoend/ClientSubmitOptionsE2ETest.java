package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientSubmitOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * The ClientSubmitOptionsTest class exercises the ClientSubmitOptions class as it affects files.
 */


@TestId("ClientSubmitOptionsE2ETest01")
public class ClientSubmitOptionsE2ETest extends P4JavaTestCase {

    private static IClient client = null;
    private static String clientDir;

    @BeforeClass
    public static void before() throws Exception {
        server = getServer();
        client = getDefaultClient(server);
        clientDir = defaultTestClientName + File.separator + testId;
        server.setCurrentClient(client);
    }

    /**
     * leaveunchanged - Files that have content or type changes are submitted. Unchanged files
     * are moved to the default changelist.
     */
    @Test
    public void testSetLeaveunchanged() throws Exception{
        int numFilesCreated = 3;  // We create 3 files, so we expect three files to be returned.
        int numExpectedOpen = 3;
        debugPrintTestName();

        //get the default options
        ClientSubmitOptions submitOpts = new ClientSubmitOptions();
        assertFalse("Default setting for isLeaveunchanged() should be false.", submitOpts.isLeaveunchanged());

        submitOpts.setLeaveunchanged(true);
        assertTrue("Setting for isLeaveunchanged() should be true.", submitOpts.isLeaveunchanged());

        List<IFileSpec> submittedSpecs = taskAddEditModifySubmitFiles(server, client);
        dumpFileSpecInfo(submittedSpecs, "submittedSpecs - FINAL");
        //assertTrue("Submitted files should be at Rev 2.", )

        dumpFileSpecInfo(FileSpecBuilder.getValidFileSpecs(submittedSpecs), "VALID: Edited and Modified fileSpecs.");

        //need to create filespecs that do not have revs because those filespecs are not acceptable to client.openedFiles()
        String[] filePaths = new String[numFilesCreated];
        int i = 0;
        for (IFileSpec fSpec : FileSpecBuilder.getValidFileSpecs(submittedSpecs)) {
            if (i <= numFilesCreated - 1) {
                String pathStr = fSpec.getDepotPathString();
                filePaths[i] = pathStr;
                i++;
            }
        }

        List<IFileSpec> validFileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
        dumpFileSpecInfo(validFileSpecs, "validFileSpecs created after stripping DepotPathString");
        List<IFileSpec> openedSpecs = client.openedFiles(validFileSpecs, numFilesCreated, submittedSpecs.get(0).getChangelistId());
        dumpFileSpecInfo(openedSpecs, "client.openedFiles()");

        assertEquals("Number of opened files is incorrect.", numExpectedOpen, openedSpecs.size());
        assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(3).getEndRevision());
        assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(4).getEndRevision());
        assertEquals("Unchanged file should be at Rev 2.", 2, submittedSpecs.get(5).getEndRevision());

        submitOpts.setLeaveunchanged(false);
        assertFalse("Setting for isLeaveunchanged() should be false.", submitOpts.isLeaveunchanged());
    }


    /**
     * leaveunchanged+reopen - Files that have content or type changes are submitted. Unchanged
     * files are moved to the default changelist. Submitted files are reopened in Default changelist.
     */
    @Test
    public void testSetLeaveunchangedReopen() throws Exception{
        int numFilesCreated = 3;  // We create 3 files, so we expect three files to be returned.
        int numExpectedOpen = 3;
            debugPrintTestName();

            //get the default options
            ClientSubmitOptions submitOpts = new ClientSubmitOptions();
            assertFalse("Default setting for isLeaveunchangedReopen() should be false.", submitOpts.isLeaveunchangedReopen());

            submitOpts.setLeaveunchangedReopen(true);
            assertTrue("Setting for isLeaveunchangedReopen() should be true.", submitOpts.isLeaveunchangedReopen());

            List<IFileSpec> submittedSpecs = taskAddEditModifySubmitFiles(server, client);
            dumpFileSpecInfo(submittedSpecs, "submittedSpecs - FINAL");

            //need to create filespecs that do not have revs because those filespecs are not acceptable to client.openedFiles()
            String[] filePaths = new String[numFilesCreated];
            int i = 0;
            for (IFileSpec fSpec : FileSpecBuilder.getValidFileSpecs(submittedSpecs)) {
                if (i <= numFilesCreated - 1) {
                    String pathStr = fSpec.getDepotPathString();
                    filePaths[i] = pathStr;
                    i++;
                }
            }

            List<IFileSpec> validFileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            dumpFileSpecInfo(validFileSpecs, "validFileSpecs created after stripping DepotPathString");
            List<IFileSpec> openedSpecs = client.openedFiles(validFileSpecs, numFilesCreated, submittedSpecs.get(0).getChangelistId());
            dumpFileSpecInfo(openedSpecs, "client.openedFiles()");

            assertEquals("Number of opened files is incorrect.", numExpectedOpen, openedSpecs.size());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(3).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(4).getEndRevision());

            submitOpts.setLeaveunchangedReopen(false);
            assertFalse("Setting for isLeaveunchangedReopen() should be false.", submitOpts.isLeaveunchangedReopen());
    }

    /**
     * revertunchanged - Files that have content or type changes are submitted. Unchanged files
     * are reverted.
     */
    @Test
    public void testSetRevertunchanged() throws Exception{
        int numFilesCreated = 3;  // We create 3 files, so we expect three files to be returned.
        int numExpectedOpen = 3;

            debugPrintTestName();

            //get the default options
            ClientSubmitOptions submitOpts = new ClientSubmitOptions();
            assertFalse("Default setting for isRevertunchanged() should be false.", submitOpts.isRevertunchanged());

            submitOpts.setRevertunchanged(true);
            assertTrue("Setting for isRevertunchanged() should be true.", submitOpts.isRevertunchanged());

            List<IFileSpec> submittedSpecs = taskAddEditModifySubmitFiles(server, client);
            dumpFileSpecInfo(submittedSpecs, "submittedSpecs - FINAL");
            //assertTrue("Submitted files should be at Rev 2.", )


            submitOpts.setRevertunchanged(false);
            assertFalse("Setting for isRevertunchanged() should be false.", submitOpts.isRevertunchanged());

            dumpFileSpecInfo(FileSpecBuilder.getValidFileSpecs(submittedSpecs), "VALID: Edited and Modified fileSpecs.");

            //need to create filespecs that do not have revs because those filespecs are not acceptable to client.openedFiles()
            String[] filePaths = new String[numFilesCreated];
            int i = 0;
            for (IFileSpec fSpec : FileSpecBuilder.getValidFileSpecs(submittedSpecs)) {
                if (i <= numFilesCreated - 1) {
                    String pathStr = fSpec.getDepotPathString();
                    filePaths[i] = pathStr;
                    i++;
                }
            }
            List<IFileSpec> validFileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            dumpFileSpecInfo(validFileSpecs, "validFileSpecs created after stripping DepotPathString");
            List<IFileSpec> openedSpecs = client.openedFiles(validFileSpecs, numFilesCreated, submittedSpecs.get(0).getChangelistId());
            dumpFileSpecInfo(openedSpecs, "client.openedFiles()");

            assertEquals("Number of opened files is incorrect.", numExpectedOpen, openedSpecs.size());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(3).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(4).getEndRevision());

    }

    /**
     * revertunchanged+reopen - Files that have content or type changes are submitted. Unchanged
     * files are reverted. Submitted files to be reopened on the default changelist.
     */
    @Test
    public void testSetRevertunchangedReopen() throws Exception{
        int numFilesCreated = 3;  // We create 3 files, so we expect three files to be returned.
        int numExpectedOpen = 3;
            //get the default options
            ClientSubmitOptions submitOpts = new ClientSubmitOptions();
            assertFalse("Default setting for isRevertunchangedReopen() should be false.", submitOpts.isRevertunchangedReopen());

            submitOpts.setRevertunchangedReopen(true);
            assertTrue("Setting for isRevertunchanged() should be true.", submitOpts.isRevertunchangedReopen());

            List<IFileSpec> submittedSpecs = taskAddEditModifySubmitFiles(server, client);
            dumpFileSpecInfo(submittedSpecs, "submittedSpecs - FINAL");
            dumpFileSpecInfo(FileSpecBuilder.getValidFileSpecs(submittedSpecs), "VALID: Edited and Modified fileSpecs.");

            //need to create filespecs that do not have revs because those filespecs are not acceptable to client.openedFiles()
            String[] filePaths = new String[numFilesCreated];
            int i = 0;
            for (IFileSpec fSpec : FileSpecBuilder.getValidFileSpecs(submittedSpecs)) {
                if (i <= numFilesCreated - 1) {
                    String pathStr = fSpec.getDepotPathString();
                    filePaths[i] = pathStr;
                    i++;
                }
            }
            List<IFileSpec> validFileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            dumpFileSpecInfo(validFileSpecs, "validFileSpecs created after stripping DepotPathString");
            List<IFileSpec> openedSpecs = client.openedFiles(validFileSpecs, numFilesCreated, submittedSpecs.get(0).getChangelistId());
            dumpFileSpecInfo(openedSpecs, "client.openedFiles()");

            assertEquals("Number of opened files is incorrect.", numExpectedOpen, openedSpecs.size());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(3).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(4).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(5).getEndRevision());

            submitOpts.setRevertunchangedReopen(false);
            assertFalse("Setting for isRevertunchanged() should be false.", submitOpts.isRevertunchangedReopen());
    }

    /**
     * submitunchanged - All open files are submitted (default).
     */
    @Test
    public void testSetSubmitunchanged() throws Exception{
        int numFilesCreated = 3;  // We create 3 files, so we expect three files to be returned.
        int numExpectedOpen = 3;

            //get the default options
            ClientSubmitOptions submitOpts = new ClientSubmitOptions();
            assertFalse("Default setting for isRevertunchangedReopen() should be false.", submitOpts.isSubmitunchanged());

            submitOpts.setSubmitunchanged(true);
            assertTrue("Setting for isRevertunchanged() should be true.", submitOpts.isSubmitunchanged());

            List<IFileSpec> submittedSpecs = taskAddEditModifySubmitFiles(server, client);
            dumpFileSpecInfo(submittedSpecs, "submittedSpecs - FINAL");
            dumpFileSpecInfo(FileSpecBuilder.getValidFileSpecs(submittedSpecs), "VALID: Edited and Modified fileSpecs.");

            //need to create filespecs that do not have revs because those filespecs are not acceptable to client.openedFiles()
            String[] filePaths = new String[numFilesCreated];
            int i = 0;
            for (IFileSpec fSpec : FileSpecBuilder.getValidFileSpecs(submittedSpecs)) {
                if (i <= numFilesCreated - 1) {
                    String pathStr = fSpec.getDepotPathString();
                    filePaths[i] = pathStr;
                    i++;
                }
            }
            List<IFileSpec> validFileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            dumpFileSpecInfo(validFileSpecs, "validFileSpecs created after stripping DepotPathString");
            List<IFileSpec> openedSpecs = client.openedFiles(validFileSpecs, numFilesCreated, submittedSpecs.get(0).getChangelistId());
            dumpFileSpecInfo(openedSpecs, "client.openedFiles()");

            assertEquals("Number of opened files is incorrect.", numExpectedOpen, openedSpecs.size());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(3).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(4).getEndRevision());

            submitOpts.setSubmitunchanged(false);
            assertFalse("Setting for isSubmitunchanged() should be false.", submitOpts.isSubmitunchanged());
    }

    /**
     * submitunchanged+reopen - All open files are submitted. Submitted
     * files to be reopened on the default changelist.
     */
    @Test
    public void testSubmitunchangedReopen() throws Exception{

        int numFilesCreated = 3;  // We create 3 files, so we expect three files to be returned.
        int numExpectedOpen = 3;
            //get the default options
            ClientSubmitOptions submitOpts = new ClientSubmitOptions();
            assertFalse("Default setting for isRevertunchangedReopen() should be false.", submitOpts.isSubmitunchangedReopen());

            submitOpts.setSubmitunchangedReopen(true);
            assertTrue("Setting for isRevertunchanged() should be true.", submitOpts.isSubmitunchangedReopen());

            List<IFileSpec> submittedSpecs = taskAddEditModifySubmitFiles(server, client);
            dumpFileSpecInfo(submittedSpecs, "submittedSpecs - FINAL");
            dumpFileSpecInfo(FileSpecBuilder.getValidFileSpecs(submittedSpecs), "VALID: Edited and Modified fileSpecs.");

            //need to create filespecs that do not have revs because those filespecs are not acceptable to client.openedFiles()
            String[] filePaths = new String[numFilesCreated];
            int i = 0;
            for (IFileSpec fSpec : FileSpecBuilder.getValidFileSpecs(submittedSpecs)) {
                if (i <= numFilesCreated - 1) {
                    String pathStr = fSpec.getDepotPathString();
                    filePaths[i] = pathStr;
                    i++;
                }
            }

            List<IFileSpec> validFileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            dumpFileSpecInfo(validFileSpecs, "validFileSpecs created after stripping DepotPathString");
            List<IFileSpec> openedSpecs = client.openedFiles(validFileSpecs, numFilesCreated, submittedSpecs.get(0).getChangelistId());
            dumpFileSpecInfo(openedSpecs, "client.openedFiles()");

            assertEquals("Number of opened files is incorrect.", numExpectedOpen, openedSpecs.size());
            assertEquals("Open file should have been reverted.", FileAction.EDIT, openedSpecs.get(0).getAction());
            assertEquals("Reverted File should be at Rev 1.", 2, openedSpecs.get(0).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(3).getEndRevision());
            assertEquals("File should be at Rev 2.", 2, submittedSpecs.get(4).getEndRevision());

            submitOpts.setSubmitunchangedReopen(false);
            assertFalse("Setting for isSubmitunchangedReopen() should be false.", submitOpts.isSubmitunchangedReopen());
    }


    //******************//
    // Helper Functions //
    //******************//

    /**
     * Opens files for Edit and Submits the passed in fileSpecs and returns the list of submitted fileSpecs.
     * No actual modification of the file takes place before submitting.
     */
    public List<IFileSpec> taskEditModifySubmitTestFiles(IServer server, List<IFileSpec> openedSpecs, String[] filePaths,
                                                         int returnType, String fileType, int numToModify) throws Exception{

        return taskEditModifySubmitTestFiles(server, openedSpecs, filePaths, returnType, fileType,
                "This is text added by ClientSubmitOptionsE2ETest", numToModify);
    }


    /**
     * Creates a changelist, reopens passed in fileSpecs using client.reopenFiles(), writes testText
     * to files, closes files, and finally submits files.
     */
    public List<IFileSpec> taskEditModifySubmitTestFiles(IServer server, List<IFileSpec> openedSpecs, String[] filePaths,
                                                         int returnType, String fileType, String testText, int numToModify) throws Exception{

        IClient client = null;
        List<IFileSpec> submittedFiles = null;
        IChangelist changelist = null;

            client = server.getClient(getPlatformClientName(defaultTestClientName));
            server.setCurrentClient(client);
            assertNotNull("Null client returned", client);

            //create a changelist
            IChangelist changelistImpl = getNewChangelist(server, client,
                    "Changelist to submit edited files " + getName());
            changelist = client.createChangelist(changelistImpl);
            //add the test files
            client.refresh();
            //List<IFileSpec> editFSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            //testFiles = client.reopenFiles(editFSpecs, changelist.getId(), fileType);
            List<IFileSpec> reopenedFiles = client.reopenFiles(openedSpecs, changelist.getId(), fileType);
            int numWritten = writeToTestFiles(filePaths, testText, numToModify);
            debugPrint("Number Files Modified" + numWritten);

            assertNotNull("FileSpec testFiles returned Null!!", reopenedFiles);
            assertFalse("FileSpec testFiles should not be empty.", reopenedFiles.isEmpty());
            //FIXME: verify null below is appropriate with p4 commandline
            //verifyFileAction(testFiles, countValidFileSpecs(editFSpecs), null);

            //submit files
            submittedFiles = changelist.submit(true);
            dumpFileSpecInfo(submittedFiles, "submittedFiles after edited");
            assertNotNull("submittedFiles should not be Null!!", submittedFiles);

        return submittedFiles;
    }

    /**
     * Opens a file and appends the testText to that file. Returns the number
     * of FILES acted on. Does not submit the files.
     */
    public int writeToTestFiles(String[] filePaths, String testText) throws IOException {

        return (writeToTestFiles(filePaths, testText, filePaths.length));

    }


    /**
     * Opens a file and appends the testText to that file. Returns the number
     * of FILES acted on. Does not submit the files.
     */
    public int writeToTestFiles(String[] filePaths, String testText, int numToModify) throws IOException {

        int numWritten = 0;
        boolean success = false;
            for (int i = 0; i <= numToModify - 1; i++) {
                success = writeFileBytes(filePaths[i], testText, true);
                if (success) {
                    numWritten++;
                    success = false;
                }
            }

        return numWritten;

    }

    /**
     * This task adds->submits->opens[edit]->modifies first 2 files->submits 3 files. Returns the submitted
     * fileSpecs.
     */
    private List<IFileSpec> taskAddEditModifySubmitFiles(IServer server, IClient client) throws Exception{
        List<IFileSpec> modifiedSpecs = null;

            assertNotNull("client should not be Null.", client);
            String clientRoot = client.getRoot();
            assertNotNull("clientRoot should not be Null.", clientRoot);
            server.setCurrentClient(client);

            //create the files
            String sourceFile = clientRoot + File.separator + textBaseFile;
            String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileSO.txt";
            String newFilePath = clientRoot + File.separator + clientDir;

            File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));
            File file2 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));
            File file3 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));

            //create the filelist
            final String[] filePaths = {
                    file1.getAbsolutePath(),
                    file2.getAbsolutePath(),
                    file3.getAbsolutePath(),
            };

            //add the files, submit them, reopen them
            IChangelist changelistImpl = getNewChangelist(server, client,
                    "Changelist to submit files for " + getName());
            IChangelist changelist = client.createChangelist(changelistImpl);

            List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
            assertNotNull("FileSpecs should not be Null.", fileSpecs);
            List<IFileSpec> addedFileSpecs = client.addFiles(fileSpecs, false, 0, P4JTEST_FILETYPE_TEXT, false);
            dumpFileSpecInfo(addedFileSpecs, "Added FileSpecs");
            //submit files. Check if added files are in the correct changelist.
            List<IFileSpec> reopenedFileSpecs = client.reopenFiles(fileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);
            dumpFileSpecInfo(reopenedFileSpecs, "Added FileSpecs");

            List<IFileSpec> submittedFileSpecs = changelist.submit(true);

            int numSubmitted = FileSpecBuilder.getValidFileSpecs(submittedFileSpecs).size();
            assertEquals("numSubmitted should equal number of files created.", filePaths.length, numSubmitted);

            //List<IFileSpec> modifiedSpecs = taskEditModifySubmitTestFiles(server, submittedFileSpecs, filePaths,
            modifiedSpecs = taskEditModifySubmitTestFiles(server, fileSpecs, filePaths,
                    P4JTEST_RETURNTYPE_VALIDONLY, P4JTEST_FILETYPE_TEXT, "Modified by ClientSubmitOptionsE2ETest", 2);

        return modifiedSpecs;

    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
