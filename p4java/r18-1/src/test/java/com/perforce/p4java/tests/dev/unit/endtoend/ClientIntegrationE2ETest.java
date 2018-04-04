package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IntegrationOptions;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.test.FileUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * This Class exercises Integration in an end to end fashion using primarily
 * client.integrateFiles() method. Relies on base files being set up, and other
 * files being created.
 * <p>
 * Constructor values are set as shown below.
 * int changeListId=,
 * boolean showActionsOnly=,
 * IntegrationOptions integOpts=,
 * java.lang.String branchSpec=,
 * IFileSpec fromFile=,
 * IFileSpec toFile=,
 */

@TestId("ClientIntegrationE2ETest01")
public class ClientIntegrationE2ETest extends P4JavaTestCase {

    static final int P4JTEST_MODIFYFILE_MAKEOLDER = 0;
    static final int P4JTEST_MODIFYFILE_MAKENEWER = 1;
    static final int P4JTEST_MODIFYFILE_MAKEWRITABLE = 2;
    static final int P4JTEST_MODIFYFILE_MAKEREADONLY = 3;

    static String mainBranchPath = "Dev" + File.separator + "rteam";
    String mainToDir = testId + File.separator + "CUST";
    //    IServer server = null;
    private static IClient client = null;
    private static String sourceFile;

    @BeforeClass
    public static void before() throws Exception {
        server = getServer();
        client = getDefaultClient(server);
        server.setCurrentClient(client);
        sourceFile = client.getRoot() + File.separator + textBaseFile;
        createTestSourceFile(sourceFile, false);
        String basePath = client.getRoot() + File.separator + mainBranchPath;
        List<Map<String, Object>> result = server.execMapCmdList(
                CmdSpec.SYNC.toString(),
                new String[]{"-f", basePath + File.separator + "HBOP" + File.separator + "...",
                        basePath + File.separator + "HGTV" + File.separator + "...",
                        basePath + File.separator + "SHOW" + File.separator + "...",
                        basePath + File.separator + "TLCP" + File.separator + "...",
                        basePath + File.separator + "HOLD" + File.separator + "old_p4java.jar",
                },
                null);
        assertNotNull(result);
    }

    /**
     * This test makes a simple integration from SRC in depot to NEW target
     * using default IntegrationOptions. Constructor values integOptions=null.
     */
    @Test
    public void testIntegrateNewBranch() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 6;
        debugPrintTestName("testIntegrateNewBranch");
        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);
        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build filespecs
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HGTV" + File.separator + "...";
        String toFileString = basePath + File.separator + mainToDir + File.separator + "ESPN" + changelistId + File.separator + "...";

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //integrate
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, null, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs);
        verifyFileAction(fSpecs, expNumFilesIntegrated, FileAction.BRANCH);

        //submit & verify
        List<IFileSpec> submittedFiles = changelist.submit(false);
        verifyTestFilesSubmitted(submittedFiles, expNumFilesIntegrated);
        //FIXME: Need verification routine to verify that the branch occurred and files exist.

    }

    /**
     * This test makes a simple integration from SRC in depot to target also in DEPOT
     * using default IntegrationOptions. Constructor:
     * integOpts=baseless;
     */
    @Test
    public void testIntegrateOverSubmittedFiles() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 6;
        int expNumFilesSubmitted = 6;
        debugPrintTestName("testIntegrateOverSubmittedFiles");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filespecs
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HBOP" + File.separator + "...";
        String toFileString = basePath + File.separator + mainToDir + changelistId + File.separator + "...";


        //build file dirs
        String[] fNameList = prepareTestDir(fromFileString, toFileString);

        //add and submit file, do not reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, null, true);
        verifyTestFilesSubmitted(sFile, expNumFilesSubmitted);

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //integrate and verify
        boolean baselessMergeVal = true;
        IntegrationOptions intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(baselessMergeVal);
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");
        verifyFileAction(fSpecs, expNumFilesIntegrated, FileAction.INTEGRATE);

        //resolve and submit
        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(fSpecs, true, false, false, false, true);
        dumpFileSpecInfo(resolvedFiles, "fSpecs returned from client.resolveFilesAuto(...)");
        List<IFileSpec> submittedFiles = changelist.submit(false);

        //final verification
        verifyTestFilesSubmitted(submittedFiles, expNumFilesIntegrated);
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from changelist.submit(false)");
        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        //use verifyTestFilesSubmitted
    }


    /**
     * This test integrates 2 branches that have completely different files in different
     * operation states. FromFile has 6 files, ToFile only has 3 files that can be integrated.
     * In the source dir, files are in various states of submission (open for edit, added, submitted,
     * deleted/not submitted, deleted). Verifies files are properly added to the new directory
     * on final submit after resolve.
     */
    @Test
    public void testIntegrateBranchesDiffStates() throws Exception {

        IChangelist changelist = null;
        int expNumIntegrated = 6;
        int expNumResolved = 1;
        int expNumSubmitted = 6;
        debugPrintTestName("testIntegrateBranchesDiffStates");
        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be null", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String baseFileString = basePath + File.separator + "HGTV" + File.separator + "..."; //all files checked in, not open
        String fromFileString = basePath + File.separator + "HBOP" + File.separator + "..."; //files in various states
        String toFileStringBase = basePath + File.separator + mainToDir + changelistId + File.separator;
        String toFileString = toFileStringBase + "...";

        String[] expFinalFileList = new String[]{
                toFileStringBase + "admin" + File.separator + "displayTool.pl", //both
                toFileStringBase + "src" + File.separator + "hbop4.java", //HBOP only
                toFileStringBase + "src" + File.separator + "hbop5.txt", //HBOP only
                toFileStringBase + "src" + File.separator + "homePlan.txt", //HGTV only
                toFileStringBase + "src" + File.separator + "homeWorks.html", //HGTV only
                toFileStringBase + "src" + File.separator + "hProj.java", //HGTV only
                toFileStringBase + "src" + File.separator + "lProj.java", //HGTV only
                toFileStringBase + "src" + File.separator + "proj1.dll", //HGTV only
        };

        //build file dirs. Copy from basefiles(6 files) to toFileString
        String[] fNameList = prepareTestDir(baseFileString, toFileString);

        //integrate and verify
        boolean baselessMergeVal = true;
        List<IFileSpec> integratedFiles = taskAddIntegrateTestFiles(server, fromFileString, toFileString, changelistId,
                baselessMergeVal, fNameList);
        assertEquals("Number of files not integrated as expected.", expNumIntegrated, countValidFileSpecs(integratedFiles));

        //resolve files
        List<IFileSpec> resolvedFiles = resolveTestFilesAuto(client, integratedFiles);
        assertEquals("Number of files not integrated as expected.", expNumResolved, countValidFileSpecs(resolvedFiles));

        //update changelist and submit
        List<IFileSpec> submittedFiles = changelist.submit(false);
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from submit (after resolve)");

        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        //verify files
        verifyTestFilesSubmitted(submittedFiles, expNumSubmitted);
    }


    /**
     * This test integrates 2 branches that have completely different files. In the source
     * dir, files are in various states of submission (open for edit, added, submitted,
     * deleted/not submitted, deleted). Verifies files are properly added to the new directory
     * on final submit after resolve.
     */
    @Test
    public void testIntegrateBranchesAllDiffFiles() throws Exception {

        IChangelist changelist = null;

        int expNumIntegrated = 8;
        int expNumResolved = 1;
        int expNumSubmitted = 8;

        debugPrintTestName("testIntegrateBranchesAllDiffFiles");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String baseFileString = basePath + File.separator + "HGTV" + File.separator + "..."; //use this in prepareTestDir
        String fromFileString = basePath + File.separator + "TLCP" + File.separator + "...";
        String toFileStringBase = basePath + File.separator + mainToDir + changelistId + File.separator;
        String toFileString = toFileStringBase + "...";

        String[] expFinalFileList = new String[]{
                toFileStringBase + "admin" + File.separator + "displayTool.pl", //TLCP & HGTV
                toFileStringBase + "src" + File.separator + "hbop1.html",  //TLCP
                toFileStringBase + "src" + File.separator + "hbop4.java",
                toFileStringBase + "src" + File.separator + "hbop5.txt",
                toFileStringBase + "src" + File.separator + "hbop6.txt",
                toFileStringBase + "src" + File.separator + "hbop7.txt",
                toFileStringBase + "src" + File.separator + "p4merge_help.png",
                toFileStringBase + "src" + File.separator + "bindetmi2.dll",
                toFileStringBase + "src" + File.separator + "homePlan.txt",  //HGTV
                toFileStringBase + "src" + File.separator + "homeWorks.html",
                toFileStringBase + "src" + File.separator + "hProj.java",
                toFileStringBase + "src" + File.separator + "lProj.java",
                toFileStringBase + "src" + File.separator + "proj1.dll"
        };

        //build file dirs
        String[] fNameList = prepareTestDir(baseFileString, toFileString);

        //integrate and verify
        boolean baselessMergeVal = true;
        List<IFileSpec> integratedFiles = taskAddIntegrateTestFiles(server, fromFileString, toFileString, changelistId,
                baselessMergeVal, fNameList);
        assertEquals("Number of files not integrated as expected.", expNumIntegrated, countValidFileSpecs(integratedFiles));

        //resolve files
        List<IFileSpec> resolvedFiles = resolveTestFilesAuto(client, integratedFiles);
        assertEquals("Number of files not integrated as expected.", expNumResolved, countValidFileSpecs(resolvedFiles));

        //update changelist and submit
        List<IFileSpec> submittedFiles = changelist.submit(false);
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from submit (after resolve)");

        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        //verify files
        verifyTestFilesSubmitted(submittedFiles, expNumSubmitted);
        String[] actFinalFileList = getTestFileList(toFileStringBase);
        verifyIntegratedTestFiles(expFinalFileList, actFinalFileList);
    }


    /**
     * This test integrates 2 branches that have the same files, but the target files
     * are not writable. The files are added,
     * submitted with reopen, then reopened in new changelist for editing. Even though target
     * is not writable, resolve should go smoothly.
     */
    @Test
    public void testIntegrateBranchesTargetNotWritable() throws Exception {

        IChangelist changelist = null;
        int expNumIntegrated = 8;
        int expNumResolved = 8;
        int expNumSubmitted = 8;
        debugPrintTestName("testIntegrateBranchesTargetNotWritable");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "TLCP" + File.separator + "...";
        String toFileStringBase = basePath + File.separator + mainToDir + changelistId + File.separator;
        String toFileString = toFileStringBase + "...";

        String[] expFinalFileList = new String[]{
                toFileStringBase + "admin" + File.separator + "displayTool.pl", //TLCP
                toFileStringBase + "src" + File.separator + "hbop1.html",  //TLCP
                toFileStringBase + "src" + File.separator + "hbop4.java",
                toFileStringBase + "src" + File.separator + "hbop5.txt",
                toFileStringBase + "src" + File.separator + "hbop6.txt",
                toFileStringBase + "src" + File.separator + "hbop7.txt",
                toFileStringBase + "src" + File.separator + "p4merge_help.png",
                toFileStringBase + "src" + File.separator + "bindetmi2.dll",
        };

        //build file dirs
        String[] fNameList = prepareTestDir(fromFileString, toFileString);

        //add and submit file, reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, null, true, true);
        verifyTestFilesSubmitted(sFile, expNumSubmitted);
        dumpFileSpecInfo(sFile, "FileSpecs returned from AddSubmitTestFiles. Should be open for edit.");

        //edit files
        List<IFileSpec> fSpecs = FileSpecBuilder.makeFileSpecList(fNameList);
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from makeFileSpecList");

        //integrate and verify
        boolean baselessMergeVal = true;
        client.reopenFiles(fSpecs, changelistId, null);
        List<IFileSpec> integratedFiles = integrateTestFiles(client, fromFileString, toFileString, changelistId,
                baselessMergeVal);
        assertEquals("Number of files not integrated as expected.", expNumIntegrated, countValidFileSpecs(integratedFiles));

        //resolve files
        List<IFileSpec> resolvedFiles = resolveTestFilesAuto(client, integratedFiles);
        assertEquals("Number of files not resolved as expected.", expNumResolved, countValidFileSpecs(resolvedFiles));

        //update changelist and submit
        List<IFileSpec> submittedFiles = changelist.submit(false);
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from submit (after resolve)");

        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        //verify files
        String[] actFinalFileList = getTestFileList(toFileStringBase);
        verifyIntegratedTestFiles(expFinalFileList, actFinalFileList);
        verifyTestFilesSubmitted(submittedFiles, expNumSubmitted);
    }

    /**
     * Files that are to be edited are actually read only when created. They are added,
     * submitted with reopen, then reopened in new changelist for editing. Even though target
     * is not writable, resolve should go smoothly.
     */
    @Test
    public void testIntegrateFileToFileTargetNotWritable() throws Exception {

        IChangelist changelist = null;
        int expNumIntegrated = 1;
        int expNumResolved = 1;        //no changes to the file so resolve should show no conflicts
        int expNumSubmitted = 1;

        debugPrintTestName("testIntegrateFileToFileTargetNotWritable");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "TLCP" + File.separator + "admin" + File.separator + "displayTool.pl";
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "displayTool.pl";

        //build new file
        String toFileString = prepareTestFile(fromFileString, toFileBaseString, false);
        String[] fNameList = {toFileString};

        //add and submit file, reopen.
        List<IFileSpec> fileSpecs = taskAddSubmitTestFiles(server, fNameList, null, true, true);
        verifyTestFilesSubmitted(fileSpecs, expNumSubmitted);
        dumpFileSpecInfo(fileSpecs, "FileSpecs returned from AddSubmitTestFiles. Should be open for edit.");

        //create new fileSpecs since reopen does not like rev#s
        List<IFileSpec> fSpecs = FileSpecBuilder.makeFileSpecList(fNameList);
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from makeFileSpecList");

        //integrate and verify
        boolean baselessMergeVal = true;
        List<IFileSpec> reopenedSpecs = client.reopenFiles(fSpecs, changelistId, null);
        dumpFileSpecInfo(reopenedSpecs, "Reopened FileSpecs");
        List<IFileSpec> integratedFiles = integrateTestFiles(client, fromFileString, toFileString, changelistId,
                baselessMergeVal);
        assertEquals("Number of files not integrated as expected.", expNumIntegrated, countValidFileSpecs(integratedFiles));

        //resolve files
        List<IFileSpec> resolvedFiles = resolveTestFilesAuto(client, integratedFiles);
        assertEquals("Number of files not integrated as expected.", expNumResolved, countValidFileSpecs(resolvedFiles));

        //update changelist and submit
        List<IFileSpec> submittedFiles = changelist.submit(false);
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from submit (after resolve)");

        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        //verify files
        verifyTestFilesSubmitted(submittedFiles, expNumSubmitted);
    }


    /**
     * Files that are to be edited are actually read only when created. They are added,
     * submitted with reopen, then reopened in new changelist for editing. Even though target
     * is not writable, resolve should go smoothly.
     */
    @Test
    public void testIntegrateFileToFileTargetEdited() throws Exception {

        IChangelist changelist = null;
        int expNumIntegrated = 1;
        int expNumResolved = 1;        //no changes to the file so resolve should show no conflicts
        int expNumSubmitted = 1;
        debugPrintTestName("testIntegrateFileToFileTargetEdited");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "TLCP" + File.separator + "admin" + File.separator + "displayTool.pl";
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "displayTool.pl";

        //build new file
        String toFileString = prepareTestFile(sourceFile, toFileBaseString, "Add this to the end of the file, Please!!!!!", false);
        String[] fNameList = {toFileString};

        //create new fileSpecs since reopen does not like rev#s
        List<IFileSpec> fSpecs = FileSpecBuilder.makeFileSpecList(fNameList);
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from makeFileSpecList");

        //add, edit and submit file, reopen.
        List<IFileSpec> addedFiles = taskAddSubmitTestFiles(server, fNameList, null, true, true);
        verifyTestFilesSubmitted(addedFiles, expNumSubmitted);

        List<IFileSpec> editedFiles = taskEditModifySubmitTestFiles(server, fSpecs, P4JTEST_RETURNTYPE_VALIDONLY,
                null, "** Adding text to make sure Integration works :) **");
        verifyTestFilesSubmitted(editedFiles, expNumSubmitted);
        dumpFileSpecInfo(editedFiles, "FileSpecs returned from EditModifySubmitTestFiles. Should be open for edit.");

        //integrate and verify
        boolean baselessMergeVal = true;
        List<IFileSpec> reopenedSpecs = client.reopenFiles(fSpecs, changelistId, null);
        dumpFileSpecInfo(reopenedSpecs, "Reopened FileSpecs");
        List<IFileSpec> integratedFiles = integrateTestFiles(client, fromFileString, toFileString, changelistId,
                baselessMergeVal);
        assertEquals("Number of files not integrated as expected.", expNumIntegrated, countValidFileSpecs(integratedFiles));

        //resolve files
        List<IFileSpec> resolvedFiles = resolveTestFilesAuto(client, integratedFiles);
        assertEquals("Number of files not integrated as expected.", expNumResolved, countValidFileSpecs(resolvedFiles));

        //update changelist and submit
        List<IFileSpec> submittedFiles = changelist.submit(false);
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from submit (after resolve)");

        verifyTestFilesSubmitted(submittedFiles, expNumSubmitted);
    }


    /**
     * This test integrates 2 branches that have the same files, but some are edited in the
     * target branch. The isBidirectionalInteg()=false.
     */
    @Test
    public void testIntegrateBranchesTargetFilesEdited() throws Exception {

        IChangelist changelist = null;
        int expNumIntegrated = 8;
        int expNumResolved = 8;
        int expNumSubmitted = 8;

        debugPrintTestName("testIntegrateBranchesTargetFilesEdited");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "SHOW" + File.separator + "...";
        String toFileStringBase = basePath + File.separator + mainToDir + changelistId + File.separator;
        String toFileString = toFileStringBase + "...";

        String[] expFinalFileList = new String[]{
                toFileStringBase + "admin" + File.separator + "displayTool.pl", //SHOW - checked in but opened for edit
                toFileStringBase + "src" + File.separator + "hbop1.html",  //SHOW
                toFileStringBase + "src" + File.separator + "hbop4.java",
                toFileStringBase + "src" + File.separator + "hbop5.txt",
                toFileStringBase + "src" + File.separator + "hbop6.txt",
                toFileStringBase + "src" + File.separator + "hbop7.txt",
                toFileStringBase + "src" + File.separator + "p4merge_help.png",
                toFileStringBase + "src" + File.separator + "bindetmi2.dll",
        };

        //build file dirs
        String[] fNameList = prepareTestDir(fromFileString, toFileString);

        //future ref touch command touch -d 2001-01-31 bindetmi2.dll
        //add and submit file, do not reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, null, true, true);
        verifyTestFilesSubmitted(sFile, expNumSubmitted);
        dumpFileSpecInfo(sFile, "FileSpecs returned from AddSubmitTestFiles. Should be open for edit.");

        //edit target files
        List<IFileSpec> fSpecs = FileSpecBuilder.makeFileSpecList(fNameList);
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from makeFileSpecList");
        List<IFileSpec> editedFiles = taskEditModifySubmitTestFiles(server, fSpecs, P4JTEST_RETURNTYPE_VALIDONLY,
                null, "** Adding text to make sure Integration works :) **");
        assertEquals("Expected number of valid fileSpecs not returned.", expNumSubmitted, countValidFileSpecs(editedFiles));

        //integrate and verify
        boolean baselessMergeVal = true;
        List<IFileSpec> integratedFiles = integrateTestFiles(client, fromFileString, toFileString, changelistId,
                baselessMergeVal);
        assertEquals("Number of files not integrated as expected.", expNumIntegrated, countValidFileSpecs(integratedFiles));

        //resolve files
        List<IFileSpec> resolvedFiles = resolveTestFilesAuto(client, integratedFiles);
        assertEquals("Number of files not integrated as expected.", expNumResolved, countValidFileSpecs(resolvedFiles));
    }


    /**
     * This test integrates 2 branches that have the same files, but some are to be edited.
     * Files that are to be edited are actually read only when created. They are added,
     * submitted with reopen, then reopened in new changelist for editing. Files are then
     * written to using the buffered writer. BUG: write bit not set when reopened.
     */
    @Ignore
    @Test
    public void testIntegrateBranchesNoSuchSourceErr() throws Exception {

        IChangelist changelist = null;
        int expNumSubmitted = 8;


        debugPrintTestName("testIntegrateBranchesNoSuchSourceErr");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should Not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filepaths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "MSNBC" + File.separator + "...";
        String toFileStringBase = basePath + File.separator + mainToDir + changelistId + File.separator;
        String toFileString = toFileStringBase + "...";

        //build file dirs
        String[] fNameList = prepareTestDir(fromFileString, toFileString);

        //future ref touch command touch -d 2001-01-31 bindetmi2.dll
        //add and submit file, do not reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, null, true, true);
        verifyTestFilesSubmitted(sFile, expNumSubmitted);
        dumpFileSpecInfo(sFile, "FileSpecs returned from AddSubmitTestFiles. Should be open for edit.");

        //edit files
        List<IFileSpec> fSpecs = FileSpecBuilder.makeFileSpecList(fNameList);
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from makeFileSpecList");
        List<IFileSpec> editedFiles = taskEditModifySubmitTestFiles(server, fSpecs, P4JTEST_RETURNTYPE_VALIDONLY,
                null, "** Adding text to make sure Integration works :) **");
        assertEquals("Expected number of valid fileSpecs not returned.", expNumSubmitted, countValidFileSpecs(editedFiles));

        //integrate and verify
        boolean baselessMergeVal = true;
        List<IFileSpec> integratedFiles = integrateTestFiles(client, fromFileString, toFileString, changelistId,
                baselessMergeVal);
        verifyFileSpecInfo(integratedFiles, FileSpecOpStatus.ERROR, "no such file(s).");
    }


    /**
     * This test makes a simple integration from SRC in depot to target also in DEPOT
     * using default IntegrationOptions. Constructor:
     * integOpts=baseless;
     */
    @Test
    @Ignore("Ignored as we do not complain about baseless in integ any more")
    public void testIntegrateBranchNeedBaselessErr() throws Exception {

        IChangelist changelist = null;
        int expNumFilesSubmitted = 6;

        debugPrintTestName("testIntegrateBranchNeedBaselessErr");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build to and from filespecs
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HBOP" + File.separator + "...";
        String toFileString = basePath + File.separator + mainToDir + changelistId + File.separator + "...";

        //build file dirs
        String[] fNameList = prepareTestDir(fromFileString, toFileString);

        //add and submit file, do not reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, null, true);
        verifyTestFilesSubmitted(sFile, expNumFilesSubmitted);

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //integrate and verify - use default integration options because baseless is false
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, null, null,
                fromFile.get(0), toFile.get(0));
        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");
        verifyFileSpecInfo(fSpecs, FileSpecOpStatus.INFO, "without -i flag");
        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        //use verifyTestFilesSubmitted
    }

    /**
     * Integrate 2 files that are binary that are the same.
     */
    @Test
    public void testIntegrateFileToFileBinary() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 1;
        debugPrintTestName("testIntegrateFileToFileBinary");

        //set integration options
        IntegrationOptions intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(true);
        assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //build file paths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "old_p4java.jar";
        String toFileString = prepareTestFile(toFileBaseString, toFileBaseString, false);
        modifyFileAttribs(toFileString, P4JTEST_MODIFYFILE_MAKEOLDER);

        //add and submit file, do not reopen.
        String[] fNameList = {toFileString};
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, P4JTEST_FILETYPE_BINARY, true);
        verifyTestFilesSubmitted(sFile, expNumFilesIntegrated);

        //build filespecs
        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileBaseString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit integrated files " + getName());
        int changelistId = changelist.getId();

        //integrate files
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");
        verifyFileAction(fSpecs, expNumFilesIntegrated, FileAction.INTEGRATE);

        //resolve by accepting theirs and submitting
        client.resolveFilesAuto(fSpecs, true, false, false, false, true);
        List<IFileSpec> submittedFiles = changelist.submit(false);

        //final verification
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from changelist.submit(false)");
        verifyTestFilesSubmitted(submittedFiles, expNumFilesIntegrated);
        //FIXME: Need verification routine to verify that the branch occurred and files exist.
        boolean filesMatch = localSystemFileCompare(toFileBaseString, toFileString);
        assertTrue("Source and Modified file should not differ", filesMatch);
    }

    /**
     * Integrate 2 files that are binary that are the same.
     */
    @Test
    public void testIntegrateBinaryFilesThatDiffer() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 1;
        debugPrintTestName("testIntegrateBinaryFilesThatDiffer");

        //set integration options
        IntegrationOptions intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(true);
        assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //build file paths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "SHOW" + File.separator + "src" + File.separator + "p4merge_help.png";
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "old_p4java.jar";
        String toFileString = prepareTestFile(toFileBaseString, toFileBaseString, false);
        modifyFileAttribs(toFileString, P4JTEST_MODIFYFILE_MAKEOLDER);

        //add and submit file, do not reopen.
        String[] fNameList = {toFileString};
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, P4JTEST_FILETYPE_BINARY, true);
        verifyTestFilesSubmitted(sFile, expNumFilesIntegrated);

        //build filespecs
        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit integrated files " + getName());
        int changelistId = changelist.getId();

        //integrate files
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");
        verifyFileAction(fSpecs, expNumFilesIntegrated, FileAction.INTEGRATE);

        //resolve by accepting theirs and submitting
        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(fSpecs, true, true, false, false, true);
        dumpFileSpecInfo(resolvedFiles, "fSpecs after client.resolveFilesAuto()");
        List<IFileSpec> submittedFiles = changelist.submit(false);

        //final verification
        dumpFileSpecInfo(submittedFiles, "FileSpecs returned from changelist.submit(false)");
    }


    @Test
    @Ignore("Ignored as we do not complain about baseless in integ any more")
    public void testIntegrateFileToFileNeedBaselessErr() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 1;
        debugPrintTestName("testIntegrateFileToFileNeedBaseless");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //build file paths
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HGTV" + File.separator + "src" + File.separator + "lProj.java";
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "hProj.java";
        String toFileString = prepareTestFile(toFileBaseString, toFileBaseString, false);

        //add and submit file, do not reopen.
        String[] fNameList = {toFileString};
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, P4JTEST_FILETYPE_TEXT, true);
        verifyTestFilesSubmitted(sFile, expNumFilesIntegrated);

        //build filespecs
        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit integrated files " + getName());
        int changelistId = changelist.getId();

        //integrate files
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, null, null,
                fromFile.get(0), toFile.get(0));

        verifyFileSpecInfo(fSpecs, FileSpecOpStatus.INFO, "without -i flag");

    }


    /**
     * Files are identical except for a few lines. Since it is file to file,
     * we will need to resolve the final output.
     */
    @Test
    public void testIntegrateTwoFilesNeedResolveErr() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 1;
        debugPrintTestName("testIntegrateTwoFilesNeedResolveErr");

        IntegrationOptions intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(true);
        assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //build files
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HGTV" + File.separator + "src" + File.separator + "lProj.java";
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "hProj.java";
        String toFileString = prepareTestFile(toFileBaseString, toFileBaseString, false);

        //add and submit file, do not reopen.
        String[] fNameList = {toFileString};
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, P4JTEST_FILETYPE_TEXT, true);
        verifyTestFilesSubmitted(sFile, expNumFilesIntegrated);

        //build filespecs
        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //integrate
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");
        verifyFileAction(fSpecs, expNumFilesIntegrated, FileAction.INTEGRATE);

        //update and submit without resolve.
        List<IFileSpec> submittedFiles = changelist.submit(false);

        verifyFileSpecInfo(submittedFiles, FileSpecOpStatus.INFO, "must resolve");
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from changelist.submit(false)");
        //FIXME: Need verification routine to verify that the branch occurred and files exist.

    }


    /**
     * Files are identical except for a few lines. Since it is file to file,
     * we will need to resolve the final output.
     */
    @Test
    public void testIntegrateTwoFilesCantClobberTargetErr() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 1;
        debugPrintTestName("testIntegrateTwoFilesCantClobberTargetErr");

        IntegrationOptions intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(true);
        assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //build filespecs
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HGTV" + File.separator + "src" + File.separator + "lProj.java";
        String toFileBaseString = basePath + File.separator + "HOLD" + File.separator + "hProj.java";
        createTestSourceFile(toFileBaseString, false);
        String toFileString = prepareTestFile(toFileBaseString, toFileBaseString, false);

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //integrate
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "FileSpecs returned from client.integrateFiles()");
        verifyFileAction(fSpecs, expNumFilesIntegrated, FileAction.BRANCH);
        verifyFileSpecInfo(fSpecs, FileSpecOpStatus.ERROR, "Can't clobber writable file");
    }


    @Test
    public void testIntegrateBranchesAlreadyIntegratedErr() throws Exception {

        IChangelist changelist = null;
        debugPrintTestName("testIntegrateBranchesAlreadyIntegratedErr");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //build filespecs
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HGTV";
        String toFileString = basePath + File.separator + "ESPN";

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //integrate
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, null, null,
                fromFile.get(0), toFile.get(0));
        dumpFileSpecInfo(fSpecs);
        verifyFileSpecInfo(fSpecs, FileSpecOpStatus.ERROR, "all revision(s) already integrated.");
    }


    //do not rename test - bug based on this in db.
    @Test
    public void testIntegrateFilesEmptyStringBranchSpec() throws Exception {

        IChangelist changelist = null;
        int expNumFilesIntegrated = 6;
        //do not rename test yet!!!!!
        debugPrintTestName("testIntegrateFilesEmptyStringBranchSpec");

        String clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build filespecs
        String basePath = clientRoot + File.separator + mainBranchPath;
        String fromFileString = basePath + File.separator + "HGTV" + File.separator + "...";
        String toFileString = basePath + File.separator + "ESPN" + changelistId + File.separator + "...";

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});
        dumpFileSpecInfo(fromFile);

        //integrate
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, null, "",
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "FileSpecs after integrate");
        assertEquals(expNumFilesIntegrated, fSpecs.size());
    }


    //*************************//
    //   Helper Functions      //
    //*************************//

    private void verifyIntegratedTestFiles(String[] actFileArray, String[] expFileArray) {
        debugPrint("\n** verifyIntegratedTestFiles **");

        Arrays.sort(expFileArray);
        Arrays.sort(actFileArray);

        boolean arraysEqual = Arrays.equals(actFileArray, expFileArray);
        if (arraysEqual == false) {
            debugPrintArray("Expected File List", expFileArray);
            debugPrintArray("Actual File List", actFileArray);
        } else {
            debugPrint("Expected File Lists match.");
        }
        assertTrue("The final file lists should match.", arraysEqual);
    }


    /**
     * This task makes a simple integration from src to target
     */
    public List<IFileSpec> taskCreateAddIntegrateResolveStandard(String fromFileString, String toFileString, boolean baselessMergeVal,
                                                                 int expNumCreated, int expNumIntegrated) throws Exception {

        IServer server = null;
        IClient client = null;
        IChangelist changelist = null;
        IntegrationOptions intOptions = null;
        List<IFileSpec> submittedFiles = null;

        debugPrint("** taskIntegrateResolve **");

        //set the baseless merge option
        debugPrint("Setting baselessMergeVal to: " + baselessMergeVal);
        intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(baselessMergeVal);
        if (baselessMergeVal) {
            assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());
        } else {
            assertFalse("IntegrationOption value should be false.", intOptions.isBaselessMerge());

        }

        server = getServer();
        client = server.getClient(getPlatformClientName(defaultTestClientName));
        server.setCurrentClient(client);
        assertNotNull("Null client returned", client);

        //create changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        int changelistId = changelist.getId();

        //build file dirs
        String[] fNameList = prepareTestDir(fromFileString, toFileString);

        //add and submit file, do not reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, P4JTEST_FILETYPE_TEXT, true);
        verifyTestFilesSubmitted(sFile, expNumCreated);

        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //integrate
        debugPrint("client.integrate() args - " + " changelist: " + changelistId + " baselessMergeVal: " +
                baselessMergeVal, "from: " + fromFile.get(0), "to: " + toFile.get(0));
        List<IFileSpec> fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));

        //verify fSpecs
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");
//			verifyFileAction(fSpecs, expNumIntegrated, FileAction.INTEGRATE);
        verifyFileAction(fSpecs, expNumIntegrated, FileAction.BRANCH);

        //resolve and submit
        List<IFileSpec> resolvedFiles = client.resolveFilesAuto(fSpecs, true, false, false, false, true);
        dumpFileSpecInfo(resolvedFiles, "fSpecs returned from client.resolveFilesAuto(...)");
        submittedFiles = changelist.submit(false);

        return submittedFiles;
    }


    /**
     * This task Adds files that have already been created, then Integrates them and returns the integrated files list.
     */
    public List<IFileSpec> taskAddIntegrateTestFiles(IServer server, String fromFileString, String toFileString, int changelistId,
                                                     boolean baselessMergeVal, String[] fNameList) throws Exception {

        IClient client = null;
        IntegrationOptions intOptions = null;
        List<IFileSpec> fSpecs = null;
        int expNumCreated = fNameList.length;
        debugPrint("** taskAddIntegrateTestFiles **");

        //set the baseless merge option
        debugPrint("Setting baselessMergeVal to: " + baselessMergeVal);
        intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(baselessMergeVal);
        if (baselessMergeVal) {
            assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());
        } else {
            assertFalse("IntegrationOption value should be false.", intOptions.isBaselessMerge());

        }

        client = server.getClient(getPlatformClientName(defaultTestClientName));
        server.setCurrentClient(client);
        assertNotNull("Null client returned", client);

        //add and submit file, do not reopen.
        List<IFileSpec> sFile = taskAddSubmitTestFiles(server, fNameList, null, true);
        verifyTestFilesSubmitted(sFile, expNumCreated);

        //build the specs
        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //integrate
        debugPrint("client.integrate() args - " + " changelist: " + changelistId + " baselessMergeVal: " +
                baselessMergeVal, "from: " + fromFile.get(0), "to: " + toFile.get(0));
        fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");

        return fSpecs;
    }

    /**
     * This task Adds files that have already been created, then Integrates them and returns the integrated files list.
     */
    public List<IFileSpec> integrateTestFiles(IClient client, String fromFileString, String toFileString, int changelistId,
                                              boolean baselessMergeVal) throws Exception {

        IntegrationOptions intOptions = null;
        List<IFileSpec> fSpecs = null;
        debugPrint("** integrateTestFiles **");

        assertNotNull("Null client unexpected", client);

        //set the baseless merge option
        debugPrint("Setting baselessMergeVal to: " + baselessMergeVal);
        intOptions = new IntegrationOptions();
        intOptions.setBaselessMerge(baselessMergeVal);
        if (baselessMergeVal) {
            assertTrue("IntegrationOption value should be true.", intOptions.isBaselessMerge());
        } else {
            assertFalse("IntegrationOption value should be false.", intOptions.isBaselessMerge());

        }

        //client = server.getClient(defaultTestClientName);
        //server.setCurrentClient(client);
        //assertNotNull("Null client unexpected", client);

        //build the specs
        List<IFileSpec> fromFile = FileSpecBuilder.makeFileSpecList(new String[]{fromFileString});
        List<IFileSpec> toFile = FileSpecBuilder.makeFileSpecList(new String[]{toFileString});

        //integrate
        debugPrint("client.integrate() args - " + " changelist: " + changelistId + " baselessMergeVal: " +
                baselessMergeVal, "from: " + fromFile.get(0), "to: " + toFile.get(0));
        fSpecs = client.integrateFiles(changelistId, false, intOptions, null,
                fromFile.get(0), toFile.get(0));
        dumpFileSpecInfo(fSpecs, "fSpecs after client.integrateFiles()");

        return fSpecs;
    }


    private List<IFileSpec> resolveTestFilesAuto(IClient client, List<IFileSpec> fSpecs) throws ConnectionException, AccessException {

        List<IFileSpec> resolvedFiles = null;
        //resolve and submit
        resolvedFiles = client.resolveFilesAuto(fSpecs, true, false, false, false, true);
        dumpFileSpecInfo(resolvedFiles, "fSpecs returned from client.resolveFilesAuto(...)");

        return resolvedFiles;
    }


    private String[] prepareTestDir(String fromPath, String toPath) throws Exception {

        String fromPathStr = null;
        String toPathStr = null;
        String[] fileList = null;
        String pathEnd = File.separator + "...";
        String cmd = null;
        if (fromPath.contains(pathEnd)) {
            fromPathStr = fromPath.substring(0, fromPath.length() - 4);
        } else {
            fromPathStr = fromPath;
        }

        if (toPath.contains(pathEnd)) {
            toPathStr = toPath.substring(0, toPath.length() - 4);
        } else {
            toPathStr = toPath;
        }
        String osName = getCurrentOsName();
        if (osName.toLowerCase().contains("windows")) {
            FileUtils.createDirectory(toPath);
            cmd = "xcopy " + fromPathStr + " " + toPathStr + " /E /H";
        } else {
            cmd = "cp -R " + fromPathStr + " " + toPathStr;
        }

        debugPrint("Copying directories:", fromPathStr, toPathStr);
        localSystemCmd(cmd);

        //now verify copy
        fileList = getTestFileList(toPathStr);
        if (!osName.toLowerCase().contains("windows")) {
            for (String fileName : fileList) {
                cmd = "chmod +rw " + fileName;
                localSystemCmd(cmd);
            }
        }
        debugPrintArray("fileList", fileList);

        return fileList;

    }

    /**
     * Returns the complete paths for each file. Optionally remove
     * the drive letter on Windows
     */
    private String[] getTestFileList(String pathStr) {

        String[] tmp = new String[100];
        int i = 0;
        File dir = new File(pathStr);
        File[] topDir = dir.listFiles();

        tmp[0] = pathStr;
        if (topDir != null) {
            for (File file : topDir) {
                if (file.isDirectory()) {
                    File[] dirFiles = file.listFiles();
                    for (File cFile : dirFiles) {
                        tmp[i] = cFile.getPath();
                        i++;
                    }
                } else {
                    tmp[i] = file.getPath();
                    i++;
                }
            }
        }
        return Arrays.copyOf(tmp, i);
    }


    private void debugPrintArray(String name, String[] strArray) {

        debugPrint("Array: " + name + "[]  Length: " + strArray.length);
        for (int i = 0; i <= strArray.length - 1; i++) {
            debugPrint(name + "[" + i + "] = " + strArray[i]);
        }

    }

    private String[] localSystemCmd(String cmdStr) throws Exception {

        String line = null;
        String errLine = null;
        String[] tmp = new String[100];
        int i = 0;
        debugPrint("** localSystemCmd **");
        debugPrint("Executing cmd: " + cmdStr);
        Process p = Runtime.getRuntime().exec(cmdStr);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        // read the output from the command
        while ((line = stdError.readLine()) != null) {
            errLine += line;
        }
        if (errLine != null) {
            if (errLine.length() > 0) {
                debugPrint("The following error was returned: " + errLine);
                fail("Unexpected Error: " + errLine);
            } else {
                debugPrint("Copy succeeded.");
            }
        }

        line = "";
        i = 0;
        while ((line = stdInput.readLine()) != null) {
            tmp[i] = line;
            i++;
        }
        return Arrays.copyOf(tmp, i);
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


    /**
     * Returns the number of valid fileSpecs for the passed in List of FileSpecs.
     */
    public int countValidFileSpecs(List<IFileSpec> fileSpecs) {

        int validFSpecCount = 0;

        if (fileSpecs != null) {
            if (fileSpecs.size() > 0) {
                for (IFileSpec fileSpec : fileSpecs) {
                    if (fileSpec != null && (fileSpec.getClientPath() != null || fileSpec.getOriginalPath() != null || fileSpec.getDepotPath() != null) && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
                        validFSpecCount++;
                    }
                }
            }
        }

        debugPrint("Valid Spec Count : " + validFSpecCount);

        return validFSpecCount;

    }

    public List<IFileSpec> taskEditNewChangelistSubmitTestFiles(IServer server, String[] filePaths, int returnType, String fileType) throws Exception{

        List<IFileSpec> editFSpecs = FileSpecBuilder.makeFileSpecList(filePaths);

        return (taskEditNewChangelistSubmitTestFiles(server, editFSpecs, returnType, fileType));
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
        //verifyFileAction(testFiles, countValidFileSpecs(editFSpecs), FileAction.EDIT);
        //FIXME: verify null below is appropriate with p4 commandline
        verifyFileAction(testFiles, countValidFileSpecs(editFSpecs), null);
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
    public List<IFileSpec> taskEditSubmitTestFiles(IServer server, List<IFileSpec> editFSpecs, int returnType, String fileType) throws Exception{

        return taskEditSubmitTestFiles(server, editFSpecs, false, returnType, fileType);
    }


    /**
     * Opens files for Edit and Submits the passed in fileSpecs and returns the list of VALID submitted fileSpecs.
     * No actual modification of the file takes place before submitting.
     */
    public List<IFileSpec> taskEditSubmitTestFiles(IServer server, List<IFileSpec> editFSpecs, boolean useDefaultChangelist,
                                                   int returnType, String fileType) throws Exception {

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
        if (useDefaultChangelist) {
            testFiles = editTestFiles(client, server, IChangelist.DEFAULT,
                    editFSpecs, returnType);
            changelist.setId(IChangelist.DEFAULT);
            debugPrint("Getting Default Changelist Info", "ID: " + changelist.getId(), "Files: " + changelist.getFiles(true));
        } else {
            testFiles = editTestFiles(client, server, changelist.getId(),
                    editFSpecs, returnType);
        }
        assertNotNull("testFiles returned Null!!", testFiles);
        verifyFileAction(testFiles, countValidFileSpecs(editFSpecs), FileAction.EDIT);

        //submit files
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
        client = server.getClient(getPlatformClientName(defaultTestClientName));
        server.setCurrentClient(client);
        assertNotNull("Null client returned", client);

        //create a changelist
        changelist = createTestChangelist(server, client,
                "Changelist to submit edited files " + getName());
        //add the test files
//			client.refresh();
        testFiles = client.reopenFiles(editFSpecs, changelist.getId(), fileType);
        writeToTestFiles(FileSpecBuilder.getValidFileSpecs(editFSpecs), testText);
        assertNotNull("FileSpec testFiles returned Null!!", testFiles);
        assertFalse("FileSpec testFiles should not be empty.", testFiles.isEmpty());
        //FIXME: verify null below is appropriate with p4 commandline
        verifyFileAction(testFiles, countValidFileSpecs(editFSpecs), null);

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

        //BufferedWriter bw = null;
        int numWritten = 0;
        String filePath = null;

        for (IFileSpec fSpec : fileSpecs) {
            //must divine a path here because the Path getters for the filespec don't reliably return a path we can use
            String origPath = fSpec.getOriginalPathString();
            if (origPath == null) {
                filePath = "" + fSpec;
            } else {
                filePath = origPath;
            }
            if (filePath == null) {
                fail("fSpec.getOriginalPathString() returned null");
            }

            debugPrint("getClientPathString: " + fSpec.getClientPathString(), "getLocalPathString: " + fSpec.getLocalPath(),
                    "getOriginalPathString: " + origPath);
            debugPrint("FSpec Info: ", "OpStatus: " + fSpec.getOpStatus() + " StatusMsg: " + fSpec.getStatusMessage(), "Action: " + fSpec.getAction());
            //bw = new BufferedWriter(new FileWriter(filePath, true));
            debugPrint("** writeToTestFiles **", "adding line: " + testText, " to file: " + fSpec);

            boolean bCopyWorked = writeFileBytes(filePath, testText, true);
            debugPrint("Write to File Successful?: " + bCopyWorked);
            //bw.write(testText);
            //bw.newLine();
            //bw.close();
            numWritten++;
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
    public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType, boolean validOnly) throws Exception{

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

        debugPrint("\n** verifyFileAction **");
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
        if (errLine.length() > 0) {
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

        return filesMatch;

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
            }
        }
        debugPrint("Submitted expNumAdded: " + expNumSubmitted, "validFSpecCount: " + validFSpecCount);
        assertEquals("Expected number of files were not submitted.", expNumSubmitted, validFSpecCount);
        assertEquals("Expected number of invalid FileSpecs should be zero", 0, invalidFSpecCount);

    }


    private void modifyFileAttribs(String filePath, int modType) throws Exception{
        //create file object
        File modFile = new File(filePath);

        if (modFile.exists()) {
            switch (modType) {
                //future ref touch command touch -d 2001-01-31 bindetmi2.dll
                case P4JTEST_MODIFYFILE_MAKENEWER:
                    modFile.setLastModified(1);
                    debugPrint("File: " + filePath, "Now Modified: " + modFile.lastModified());
                    break;
                case P4JTEST_MODIFYFILE_MAKEOLDER:
                    modFile.setLastModified(0);
                    debugPrint("File: " + filePath, "Now Modified: " + modFile.lastModified());
                    break;
                case P4JTEST_MODIFYFILE_MAKEWRITABLE:
                    SysFileHelperBridge.getSysFileCommands().setWritable(modFile.getCanonicalPath(), true);
                    //modFile.setWritable(true);
                    debugPrint("File: " + filePath, "Writable?: " + modFile.canWrite());
                    break;
                case P4JTEST_MODIFYFILE_MAKEREADONLY:
                    modFile.setReadOnly();
                    debugPrint("File: " + filePath, "ReadOnly?: " + modFile.canRead());
                    break;
                default: //P4JTEST_TIMESTAMP_MAKEOLD

            }
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
