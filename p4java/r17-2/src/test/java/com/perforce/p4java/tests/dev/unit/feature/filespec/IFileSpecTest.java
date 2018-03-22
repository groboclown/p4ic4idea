package com.perforce.p4java.tests.dev.unit.feature.filespec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.VerifyFileSpec;

/**
 * The IFileSpecE2ETest class exercises the FileSpec class as it affects files. The fileSpecs
 * are created with various fields set, and the files are then added to the depot using
 * client.addFiles(). The results are verified at the fileSpec field level, and at the depot
 * level (i.e. does the file exist).
 */


@TestId("IFileSpecTest01")
public class IFileSpecTest extends P4JavaTestCase {

    private String clientDir = defaultTestClientName + "_Dir" + File.separator + testId;
    private String sourceFile;
    private String clientRoot;

    @Before
    public void before() throws Exception {
        server = getServer(serverUrlString, null, userName, "");
        client = getDefaultClient(server);
        assertNotNull("client should not be Null.", client);
        clientRoot = client.getRoot();
        assertNotNull("clientRoot should not be Null.", clientRoot);

        server.setCurrentClient(client);
        sourceFile = clientRoot + File.separator + textBaseFile;
        createTestSourceFile(sourceFile, false);
    }


    /**
     * Create FileSpec without the originalPath. Verify the
     * error in the getInvalidFileSpecs() info.
     */
    @Test
    public void testFileSpecGetInvalidFileSpecsNoPath() throws Exception {
        IServer server = null;
        int expNumValidFSpecs = 1;
        int expNumInvalidFSpecs = 0;
        debugPrintTestName();

        //create the files
        String sourceFile = clientRoot + File.separator + textBaseFile;
        String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
        String newFilePath = clientRoot + File.separator + clientDir;

        File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));

        //create the filePaths
        final String[] filePaths = {
                file1.getAbsolutePath(),
        };

        //set up the expected values
        VerifyFileSpec fSpec0 = new VerifyFileSpec();
        fSpec0.setExpClientName(defaultTestClientName);
        fSpec0.setExpUserName(userName);
        fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
        fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);

        //now build the spec without specifying the original filepath
        List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
        dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
        dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");

        assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(buildFileSpecs).size());
        assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(buildFileSpecs).size());
    }


    /**
     * Create FileSpec without the originalPath. Verify the
     * error in the getInvalidFileSpecs() info.
     */
    @Test
    public void testFileSpecGetInvalidFileSpecsEmptyPath() throws Exception {
        int expNumValidFSpecs = 1;
        int expNumInvalidFSpecs = 0;

        debugPrintTestName();

        final String[] filePaths = {
                "",
        };

        //set up the expected values
        VerifyFileSpec fSpec0 = new VerifyFileSpec();
        fSpec0.setExpClientName(defaultTestClientName);
        fSpec0.setExpUserName(userName);
        fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
        fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
        fSpec0.setExpOriginalPath("");
        //now build the spec without specifying the original filepath
        List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
        dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
        dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");

        assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(buildFileSpecs).size());
        assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(buildFileSpecs).size());
    }


    /**
     * Create null FileSpec. Verify that getValidFileSpecs() returns 0 specs.
     */
    @Test
    public void testFileSpecGetValidFileSpecsNull() throws Exception {
        int expNumValidFSpecs = 0;
        debugPrintTestName();

        //Just make a new, null fileSpec
        FileSpec nullFileSpecs = new FileSpec();
        dumpFileSpecMethods(nullFileSpecs, "Null file Specs");
        List<IFileSpec> specList = null;

        assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(specList).size());
    }

    /**
     * Create null FileSpec. Verify that getInvalidFileSpecs() returns 1 spec.
     * This fails because neither getInvalidFileSpecs() nor getValidFileSpecs()
     * returns a spec if it's null. Javadoc indicates each returns non-null specs
     * so technically the failure is invalid. Using it as a placeholder.
     * Perhaps we need a fileSpec validating function.
     */
    @Test
    public void testFileSpecGetInvalidFileSpecsNull() throws Exception {
        int expNumInvalidFSpecs = 0;

        debugPrintTestName();

        //Just make a new, null fileSpec
        FileSpec nullFileSpecs = new FileSpec();
        dumpFileSpecMethods(nullFileSpecs, "Null file Specs");
        List<IFileSpec> specList = null;

        assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(specList).size());
    }


    /**
     * Create FileSpec using basic FileSpec() constructor. Verify the
     * new object is valid per getValidFileSpecs().
     */
    @Test
    public void testFileSpecGetValidSpecsBasicConstructor() throws Exception {
        int expNumValidFSpecs = 1;
        int expNumInvalidFSpecs = 0;
        debugPrintTestName();

        //Make a new fileSpec list and check it.
        FileSpec nullFileSpecs = new FileSpec();
        dumpFileSpecMethods(nullFileSpecs, "Null file Specs");

        List<IFileSpec> specList = new ArrayList<IFileSpec>();
        specList.add(nullFileSpecs);

        assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(specList).size());
        assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(specList).size());
    }


    /**
     * Create FileSpec list but don't populate it. Verify the
     * new object via getValidFileSpecs(), getInvalidFileSpecs().
     */
    @Test
    public void testFileSpecGetValidSpecsEmptyList() throws Exception {
        int expNumValidFSpecs = 0;
        int expNumInvalidFSpecs = 0;
        debugPrintTestName();

        //Make a new fileSpec list and check it.
        FileSpec nullFileSpecs = new FileSpec();
        dumpFileSpecMethods(nullFileSpecs, "Null file Specs");

        //create a list, but don't populate it
        List<IFileSpec> specList = new ArrayList<IFileSpec>();

        assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(specList).size());
        assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(specList).size());
    }


    /**
     * Creates a file with 3 revs, and then a filespec that can be used
     * to sync to an previous rev. Verify the sync works by checking the haveRev.
     */
    @Test
    public void testFileSpecSetRev() throws Exception {
        IServer server = null;
        int expNumValidFSpecs = 1;
        int expNumInvalidFSpecs = 0;
        debugPrintTestName();

        String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
        String newFilePath = clientRoot + File.separator + clientDir;

        File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));

        //create the filePaths
        final String[] filePaths = {
                file1.getAbsolutePath() + "#2",
        };

        //set up the expected values
        VerifyFileSpec fSpec0 = new VerifyFileSpec();
        fSpec0.setExpClientName(defaultTestClientName);
        fSpec0.setExpUserName(userName);
        fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
        fSpec0.setExpAction(FileAction.ADD);
        fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
        fSpec0.setExpChangelistId(IChangelist.UNKNOWN);
        fSpec0.setExpFileRev(2);
        fSpec0.setExpOriginalPath(filePaths[0]);

        List<IFileSpec> builtFileSpecs = buildFileSpecs(filePaths, fSpec0);

        assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(builtFileSpecs).size());
        assertEquals("Number of valid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(builtFileSpecs).size());
        assertEquals("The FileSpec Revs should match.", fSpec0.getExpFileRev(), builtFileSpecs.get(0).getEndRevision());
        assertEquals("The FileSpec Actions should match.", fSpec0.getExpAction(), builtFileSpecs.get(0).getAction());
    }


    //*****************//
    //**** Helper ****//
    //****************//

    /**
     * Takes the verification spec and creates a filespec from it.
     */
    private List<IFileSpec> buildFileSpecs(String[] filePaths, VerifyFileSpec verifySpec) {

        FileSpec fileSpec = null;
        List<IFileSpec> fileSpecList = null;

        if (filePaths != null) {
            fileSpecList = new ArrayList<IFileSpec>();
            for (String filePath : filePaths) {
                debugPrint("Building FileSpec: " + filePath);
                if (filePath != null) {
                    fileSpec = new FileSpec(filePath);
                    fileSpec.setClientName(verifySpec.getExpClientName());
                    fileSpec.setUserName(verifySpec.getExpUserName());
                    fileSpec.setFileType(verifySpec.getExpFileType());
                    fileSpec.setAction(verifySpec.getExpAction());
                    fileSpec.setChangelistId(verifySpec.getExpChangelistId());
                    fileSpec.setOriginalPath(verifySpec.getExpOriginalPath());
                    fileSpec.setOpStatus(verifySpec.getExpOpStatus());

                    fileSpecList.add(new FileSpec(fileSpec));

                    debugPrint("New FileSpec SETTINGS");
                    debugPrint("FileSpec: " + fileSpec);
                    debugPrint("ClientName: " + fileSpec.getClientName());
                    debugPrint("UserName: " + fileSpec.getUserName());
                    debugPrint("FileType: " + fileSpec.getFileType());
                    debugPrint("Action: " + fileSpec.getAction());
                    debugPrint("ChangelistId: " + fileSpec.getChangelistId());
                    debugPrint("OriginalPath: " + fileSpec.getOriginalPath());
                    debugPrint("OpStatus: " + fileSpec.getOpStatus());

                    dumpFileSpecInfo(fileSpecList, "Built file Specs");
                }
            }
        }

        return fileSpecList;
    }


    public void verifyFileSpecMethods(String comments, IFileSpec fileSpec, VerifyFileSpec spec, int verifyType) {

        debugPrint("** verifyFileSpecMethods **" + "\n" + comments);

        if (fileSpec != null) {
            dumpFileSpecMethods(fileSpec, "Dump before Verify");

            if (verifyType == P4JTEST_VERIFYTYPE_BASIC) {
                assertEquals("The fileSpec OpStatus does not match.", spec.expOpStatus, fileSpec.getOpStatus());
                assertEquals("The fileSpec Actions are not equal.", spec.expAction, fileSpec.getAction());
                assertEquals("The fileSpec fileTypes are not equal.", spec.expFileType, fileSpec.getFileType());
            }
            if (verifyType == P4JTEST_VERIFYTYPE_EXTENDED || verifyType == P4JTEST_VERIFYTYPE_ALL) {
                assertEquals("The fileSpec changelistIds are not equal.", spec.expChangelistId, fileSpec.getChangelistId());
            }
            if (verifyType == P4JTEST_VERIFYTYPE_ALL) {
                assertEquals("The fileSpec clientNames are not equal.", spec.expClientName, fileSpec.getClientName());
                assertEquals("The fileSpec userNames are not equal.", spec.expUserName, fileSpec.getUserName());
            }
            if (verifyType == P4JTEST_VERIFYTYPE_MESSAGE) {
                debugPrint("Verifying Message: " + spec.getExpStatusMessage());
                debugPrint("FileSpec StatusMsg: " + fileSpec.getStatusMessage());
                assertTrue("The fileSpec messages are not equal.", fileSpec.getStatusMessage().contains(spec.getExpStatusMessage()));
            }
            if (spec.getExpClientPath() != null) {
                if (fileSpec.getClientPath() != null) {
                    assertEquals("The fileSpec preferredPaths are not equal.", spec.expClientPath, "" + fileSpec.getClientPath());
                } else {
                    fail("The fileSpec preferredPaths are not equal. " + fileSpec.getClientPath());
                }
            }
            if (spec.getExpOriginalPath() != null) {
                if (fileSpec.getOriginalPath() != null) {
                    assertEquals("The fileSpec originalPaths are not equal.", spec.expOriginalPath, "" + fileSpec.getOriginalPath());
                } else {
                    fail("The fileSpec originalPaths are not equal. " + fileSpec.getOriginalPath());
                }
            }
            if (spec.getExpPreferredPath() != null) {
                if (fileSpec.getPreferredPath() != null) {
                    assertEquals("The fileSpec preferredPaths are not equal.", spec.expPreferredPath, "" + fileSpec.getPreferredPath());
                } else {
                    fail("The fileSpec preferredPaths are not equal. " + fileSpec.getPreferredPath());
                }
            }
        }
    }

    private void dumpFileSpecMethods(IFileSpec fileSpec, String comments) {

        debugPrint("** verifyFileSpecMethods **" + "\n" + comments);
        debugPrint("Dump info on fileSpec: " + fileSpec);

        if (fileSpec != null) {
            debugPrint("The returned fileSpecOpStatus: " + fileSpec.getOpStatus());
            debugPrint("The returned fileSpec Action: " + fileSpec.getAction());
            debugPrint("The returned fileSpec fileType: " + fileSpec.getFileType());
            debugPrint("The returned fileSpec clientName: " + fileSpec.getClientName());
            debugPrint("The returned fileSpec userName: " + fileSpec.getUserName());
            debugPrint("The returned fileSpec changelistId: " + fileSpec.getChangelistId());
            if (fileSpec.getDepotPath() != null) {
                debugPrint("The returned fileSpec depotPath: " + fileSpec.getDepotPath().toString());
            }
            if (fileSpec.getClientPath() != null) {
                debugPrint("The returned fileSpec originalPath: " + fileSpec.getClientPath().toString());
            }
            if (fileSpec.getOriginalPath() != null) {
                debugPrint("The returned fileSpec originalPath: " + fileSpec.getOriginalPath().toString());
            }
            if (fileSpec.getPreferredPath() != null) {
                debugPrint("The returned fileSpec preferredPath: ", fileSpec.getPreferredPath().toString());
            }
        }
    }


}
