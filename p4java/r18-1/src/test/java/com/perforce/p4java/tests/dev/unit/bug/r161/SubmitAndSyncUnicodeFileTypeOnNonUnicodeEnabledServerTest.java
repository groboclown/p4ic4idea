package com.perforce.p4java.tests.dev.unit.bug.r161;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

@TestId("SubmitAndSyncUnicodeFileTypeOnNonUnicodeEnabledServerTest")
public class SubmitAndSyncUnicodeFileTypeOnNonUnicodeEnabledServerTest extends P4JavaRshTestCase {
    private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job085433/" + System.currentTimeMillis();
    private static final String TEST_FILE_PARENT_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    private IChangelist changelist = null;
    private List<IFileSpec> submittedOrPendingFileSpecs = null;
    
    
    @ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SubmitAndSyncUnicodeFileTypeOnNonUnicodeEnabledServerTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }

    @Test
    public void testEncodeByJpShiftJisWithUnixLineEndingUnicodeFileUnderServerUnicodeNotEnabledExpectedTradeAsText_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/shift_jis.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "text",
                originalFileSize,
                originalFileSize
        );
    }

    @Test
    public void testEncodeByJpShiftJisWithUnixLineEndingUnicodeFileUnderServerUnicodeNotEnabledExpectedTradeAsText_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/shift_jis.txt");
        int totalUnixLineEndings = 1;
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "text",
                originalFileSize,
                originalFileSize + totalUnixLineEndings
        );
    }

    @Test
    public void testEncodeByEncJpWithUnixLineEndingUnicodeFileUnderServerUnicodeNotEnabledExpectedTradeAsText_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/euc-jp.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "text",
                originalFileSize,
                originalFileSize
        );
    }

    @Test
    public void testEncodeByEncJpWithUnixLineEndingUnicodeFileUnderServerUnicodeNotEnabledExpectedTradeAsText_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/euc-jp.txt");
        int totalUnixLineEndings = 10;
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "text",
                originalFileSize,
                originalFileSize + totalUnixLineEndings
        );
    }

    private void testSubmitFile(
            final String clientName,
            final File testResourceFile,
            final String expectedFileTypeString,
            final long expectedServerSideFileSize,
            final long expectedSyncedLocalFileSize) throws Exception {

        String depotPath = TEST_FILE_PARENT_DEPOT_PATH + "/" + testResourceFile.getName();
        SubmittingSupplier submittingSupplier = submitFileThatLoadFromClassPath(clientName,
                depotPath,
                RELATIVE_DEPOT_PATH,
                testResourceFile);

        submittedOrPendingFileSpecs = new ArrayList<>();
        submittedOrPendingFileSpecs.add(submittingSupplier.submittedFileSpec());
        changelist = submittingSupplier.changelist();
        Path targetLocalFile = submittingSupplier.targetLocalFile();

        verifyServerSideFileType(depotPath, expectedFileTypeString);
        verifyServerSideFileSize(depotPath, expectedServerSideFileSize);
        verifyFileSizeAfterSyncFromDepot(depotPath, targetLocalFile, expectedSyncedLocalFileSize);
    }

    @After
    public void afterEach() throws Exception {
        revertChangelist(changelist, submittedOrPendingFileSpecs);
    }

    @AfterClass
    public static void afterAll() throws Exception {
       afterEach(server);
    }
}
