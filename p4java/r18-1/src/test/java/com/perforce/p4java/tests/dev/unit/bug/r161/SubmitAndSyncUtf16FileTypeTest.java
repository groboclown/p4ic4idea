package com.perforce.p4java.tests.dev.unit.bug.r161;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;


import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

/**
 * @author Sean Shou
 * @since 18/07/2016
 */

public class SubmitAndSyncUtf16FileTypeTest extends P4JavaRshTestCase {
    private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job085433/" + System.currentTimeMillis();
    private static final String TEST_FILE_PARENT_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    private IChangelist changelist = null;
    private List<IFileSpec> submittedOrPendingFileSpecs = null;

    @ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", SubmitAndSyncUtf16FileTypeTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }
    
    
    @Test
    public void testValidUtf16LEWithoutBomWithWinLineEnding_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_16LE_without_bom.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "binary",
                originalFileSize,
                originalFileSize
        );
    }

    
    @Test
    public void testValidUtf16LEWithoutBomWithWinLineEnding_WinClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_16LE_without_bom.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "binary",
                originalFileSize,
                originalFileSize
        );
    }

    
    @Test
    public void testValidUtf16LEWithBomWithWinLineEnding_UnixClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_16LE_with_bom_WIN_line_end.txt");
        long utf8EncodedOriginalFileSize = getUtf16FileSizeAfterRemoveBomAndEncodedByUtf8(testResourceFile, UTF_16LE);
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "utf16",
                utf8EncodedOriginalFileSize,
                originalFileSize
        );
    }

    
    @Test
    public void testValidUtf16LEWithBomWithWinLineEnding_WinClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_16LE_with_bom_WIN_line_end.txt");
        int totalWinLineEndings = 9;
        long utf8EncodedOriginalFileSize = getUtf16FileSizeAfterRemoveBomAndEncodedByUtf8(testResourceFile, UTF_16LE);
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "utf16",
                utf8EncodedOriginalFileSize - totalWinLineEndings,
                originalFileSize
        );
    }

   
    @Test
    public void testValidUtf16BEWithoutBomWithUnixLineEnding_UnixClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf-16be_without_bom.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "binary",
                originalFileSize,
                originalFileSize
        );
    }

    //@DisplayName("test valid UTF_16BE with unix line ending but without BOM under win client line-ending")
    @Test
    public void testValidUtf16BEWithoutBomWithUnixLineEnding_WinClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf-16be_without_bom.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "binary",
                originalFileSize,
                originalFileSize
        );
    }

    //@DisplayName("test valid UTF_16BE with win line ending but with BOM under unix client line-ending")
    @Test
    public void testValidUtf16BEWithBomWithWinLineEnding_UnixClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_16BE_with_bom_and_win_line_ending.txt");
        long utf8EncodedOriginalFileSize = getUtf16FileSizeAfterRemoveBomAndEncodedByUtf8(testResourceFile, UTF_16BE);
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "utf16",
                utf8EncodedOriginalFileSize,
                originalFileSize
        );
    }

    
    @Test
    public void testValidUtf16BEWithBomWithWinLineEnding_WinClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/utf_16BE_with_bom_and_win_line_ending.txt");
        int totalWinLineEndings = 2;
        long utf8EncodedOriginalFileSize = getUtf16FileSizeAfterRemoveBomAndEncodedByUtf8(testResourceFile, UTF_16BE);
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "utf16",
                utf8EncodedOriginalFileSize - totalWinLineEndings,
                originalFileSize
        );
    }

    
    @Test
    public void testNotValidUtf16LEFileEventItHasBomButItsActuallyAnAudioFile() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/file_has_utf_16LE_bom_but_its_actually_a_audio_file.mp1");
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "binary",
                originalFileSize,
                originalFileSize
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

    private long getUtf16FileSizeAfterRemoveBomAndEncodedByUtf8(File testResourceFile, Charset utf16) throws Exception {
        try (BOMInputStream bomSkipedInputStream = new BOMInputStream(
                new FileInputStream(testResourceFile),
                false,
                ByteOrderMark.UTF_16LE,
                ByteOrderMark.UTF_16BE)) {
            byte[] bomSkippedBytes = IOUtils.toByteArray(bomSkipedInputStream);
            ByteBuffer buf = ByteBuffer.wrap(bomSkippedBytes);
    		CharsetConverter convert = new CharsetConverter(utf16, CharsetDefs.UTF8);
    		return convert.convert(buf).limit();
        }
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
