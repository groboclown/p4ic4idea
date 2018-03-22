package com.perforce.p4java.tests.dev.unit.bug.r161;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;


import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

public class SubmitAndSyncUnicodeFileTypeOnUnicodeEnabledServerTest extends P4JavaRshTestCase {
    private static final String CLASS_PATH_PREFIX = "com/perforce/p4java/impl/mapbased/rpc/sys";
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job085433/" + System.currentTimeMillis();
    private static final String TEST_FILE_PARENT_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    private IChangelist changelist = null;
    private List<IFileSpec> submittedOrPendingFileSpecs = null;

    
    @ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", SubmitAndSyncUnicodeFileTypeOnUnicodeEnabledServerTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }

    @Test
    public void testEncodeByJpShiftJisWithUnixLineEndingUnicodeFileUnderServerUnicodeEnabled_UnixClientLineEnding() throws Exception{
        String perforceCharset = "shiftjis";
        login(perforceCharset);

        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/shift_jis.txt");
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize,
                originalFileSize
        );
    }

    @Test
    public void testEncodeByJpShiftJisWithUnixLineEndingUnicodeFileUnderServerUnicodeEnabled_WinClientLineEnding() throws Exception{
        String perforceCharset = "shiftjis";
        login(perforceCharset);

        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/shift_jis.txt");
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        int totalUnixLineEndings = 1;
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);
        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize,
                originalFileSize + totalUnixLineEndings
        );
    }

    //@Test
    public void testEncodedByeEncJpWithUnixLineEndingFileWhenClientCharsetIsMatchUnderServerUnicodeEnabled_WinClientLineEnding() throws Exception {
        String perforceCharset = "eucjp";
        login(perforceCharset);

        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/euc-jp.txt");
        int totalUnixLineEndings = 10;
        long originalFileSize = Files.size(testResourceFile.toPath());
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);

        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize,
                originalFileSize + totalUnixLineEndings
        );
    }

    @Test
    public void testEncodedByeGb18030WithUnixLineEndingFileWhenClientCharsetIsMatchUnderServerUnicodeEnabled_UnixClientLineEnding() throws Exception{
        String perforceCharset = "cp936";

        login(perforceCharset);
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/gb18030.txt");

        long originalFileSize = Files.size(testResourceFile.toPath());
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);

        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize,
                originalFileSize
        );
    }

    @Test
    public void testEncodedByeGb18030WithUnixLineEndingFileWhenClientCharsetIsMatchUnderServerUnicodeEnabled_WinClientLineEnding() throws Exception{
        String perforceCharset = "cp936";

        login(perforceCharset);
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/gb18030.txt");

        int totalUnixLineEndings = 1;
        long originalFileSize = Files.size(testResourceFile.toPath());
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);

        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize,
                originalFileSize + totalUnixLineEndings
        );
    }

    @Test
    public void testEncodedByeGb18030WithWinLineEndingFileWhenClientCharsetIsMatchUnderServerUnicodeEnabled_UnixClientLineEnding() throws Exception{
        String perforceCharset = "cp936";
        login(perforceCharset);

        File testResourceFile = loadFileFromClassPath("com/perforce/p4java/common/io/gb18030_win_line_endings.txt");
        long originalFileSize = Files.size(testResourceFile.toPath());
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);

        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize,
                originalFileSize
        );
    }

    @Test
    public void testEncodedByeGb18030WithWinLineEndingFileWhenClientCharsetIsMatchUnderServerUnicodeEnabled_WinClientLineEnding() throws Exception{
        String perforceCharset = "cp936";
        login(perforceCharset);

        File testResourceFile = loadFileFromClassPath("com/perforce/p4java/common/io/gb18030_win_line_endings.txt");
        int totalWinLineEndings = 4;
        long originalFileSize = Files.size(testResourceFile.toPath());
        Charset matchCharset = Charset.forName(PerforceCharsets.getJavaCharsetName(perforceCharset));
        long utf8EncodedOriginalFileSize = getUnicodeFileSizeAfterEncodedByUtf8(
                testResourceFile,
                matchCharset);

        testSubmitFile(
                "p4TestWinLineend",
                testResourceFile,
                "unicode",
                utf8EncodedOriginalFileSize - totalWinLineEndings,
                originalFileSize
        );
    }

    /*  ---- unnormalized scenario --*/
    @Test
    public void testEncodedByeEncJpWithUnixLineEndingFileExpectedDetectAsTextWhenClientCharsetIsNullUnderServerUnicodeEnabled_UnixClientLineEnding() throws Exception {
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/euc-jp.txt");
        login("none");

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
    public void testEncodedByeEncJpWithUnixLineEndingFileWhenClientCharsetIsNotMatchRealFileEncodeUnderServerUnicodeEnabledThenTradeAsBinary_WinClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/euc-jp.txt");
        login("shiftjis");

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

    @Test
    public void testEncodedByeGb18030WithUnixLineEndingFileWhenClientCharsetIsNotMatchRealFileEncodeUnderServerUnicodeEnabledThenTradeAsBinary_UnixClientLineEnding() throws Exception{
        File testResourceFile = loadFileFromClassPath(CLASS_PATH_PREFIX + "/gb18030.txt");
        login("shiftjis");

        long originalFileSize = Files.size(testResourceFile.toPath());
        testSubmitFile(
                "p4TestUnixLineend",
                testResourceFile,
                "text",
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

    private void login(String clientCharset) throws P4JavaException {
        setClientCharsetIfServerSupportUnicode(server, clientCharset);
        server.setUserName(getUserName());
        server.login(getPassword(), new LoginOptions());
    }


    private long getUnicodeFileSizeAfterEncodedByUtf8(File testResourceFile, Charset clientCharset) throws Exception {
        try (InputStream fileInputStream = new FileInputStream(testResourceFile)) {
            byte[] fileBytes = IOUtils.toByteArray(fileInputStream);
            ByteBuffer buf = ByteBuffer.wrap(fileBytes);
    		CharsetConverter convert = new CharsetConverter(clientCharset, CharsetDefs.UTF8);
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
