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
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

/**
 * @author Sean Shou
 * @since 18/07/2016
 */

public class SubmitAndSyncUtf8FileTypeTest extends P4JavaRshTestCase {
    private static final long UTF_8_BOM_SIZE = 3;
    private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job086058/" + System.currentTimeMillis();
    private static final String TEST_FILE_PARENT_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    private IChangelist changelist = null;
    private List<IFileSpec> submittedOrPendingFileSpecs = null;

    @ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", SubmitAndSyncUtf8FileTypeTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    	//server.setCurrentClient(server.getClient("p4TestUnixLineend"));
    }
    
    @Test
    public void testSubmitUtf8UnixLineEndingFileWithBom_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_unix_lineending.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "utf8",
                originalFileSize - UTF_8_BOM_SIZE,
                originalFileSize
        );
    }

    
    @Test
    public void testSubmitUtf8UnixLineEndingFileWithBom_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_unix_lineending.txt");
        int totalUnixLineEndings = 1;
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "utf8",
                originalFileSize - UTF_8_BOM_SIZE,
                originalFileSize + totalUnixLineEndings
        );
    }

 
    @Test
    public void testSubmitValidUtf8FileWithUnixLineEndingButWithoutBOM_UnlixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/has_utf8_bom_but_its_text.txt");

        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "text",
                originalSize,
                originalSize
        );
    }

    @Test
    @Ignore("issue with data in unicode file")
    public void testSubmitValidUtf8FileWithUnixLineEndingButWithouBOM_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_8-jp_without_bom.txt");
        int totalUnixLineEndings = 9;
        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "text",
                originalSize,
                originalSize + totalUnixLineEndings
        );
    }

    
    @Test
    public void testSubmitValidUtf8FileWithWinLineEndingButWithouBOM_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "utf8",
                originalSize - UTF_8_BOM_SIZE,
                originalSize
        );
    }

    //@DisplayName("test valid UTF-8 file with win line-ending and with BOM under win client line-ending")
    @Test
    public void testSubmitValidUtf8FileWithWinLineEndingButWithouBOM_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
        int totalWinLineEndings = 2;
        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "utf8",
                originalSize - UTF_8_BOM_SIZE - totalWinLineEndings,
                originalSize
        );
    }

    
    //@Test
    public void testSubmitValidUtf8FileWithWinLineEndingButWithBOM_LocalClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
        int totalWinLineEndings = 2;
        long originalSize = Files.size(testResourceFile.toPath());
        if (OSUtils.isWindows()) {
            testSubmitFile(
                    "p4TestUserWS",
                    testResourceFile,
                    "utf8",
                    originalSize - UTF_8_BOM_SIZE - totalWinLineEndings,
                    originalSize
            );
        }

        if (OSUtils.isOSX() || OSUtils.isUnix()) {
            testSubmitFile(
                    "p4TestUserWS",
                    testResourceFile,
                    "utf8",
                    originalSize - UTF_8_BOM_SIZE,
                    originalSize
            );
        }
    }

   
    @Test
    public void testSubmitValidUtf8FileWithWinLineEndingButWithBOM_ShareClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf8_with_bom_win_line_ending.txt");
        int totalWinLineEndings = 2;
        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestShareLineend",
                testResourceFile,
                "utf8",
                originalSize - UTF_8_BOM_SIZE - totalWinLineEndings,
                originalSize - totalWinLineEndings
        );
    }

    
    @Test
    public void testSubmitTextFileWithClassicMacLineEnding_MacClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/text_file_with_classic_mac_cr_lineend.txt");
        long originalSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestMacLineend",
                testResourceFile,
                "text",
                originalSize,
                originalSize
        );
    }



    @Test
    public void testSubmitFileWithUtf8BomButItIsNotValidUtf8FileTradeAsText_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/has_utf8_bom_but_its_text.txt");
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
    public void testSubmitFileWithUtf8BomButItIsNotValidUtf8FileTradeAsText_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/has_utf8_bom_but_its_text.txt");
        int totalWinLineEndings = 2;
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "text",
                originalFileSize - totalWinLineEndings,
                originalFileSize
        );
    }

   
    @Test
    public void testSubmitFileHasUtf8BomButItsABinaryFile_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/has_utf8_bom_but_its_a_binary.txt");
        long originalSizeAsBinary = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "binary",
                originalSizeAsBinary,
                originalSizeAsBinary
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
        //afterEach(server);
    }
}
