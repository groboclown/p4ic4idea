package com.perforce.p4java.tests.dev.unit.bug.r161;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;


/**
 * @author Sean Shou
 * @since 18/07/2016
 */

public class SubmitAndSyncUtf16BETest extends P4JavaRshTestCase {
    private static final String RELATIVE_DEPOT_PATH = "/152Bugs/job089214/utf16be/" + System.currentTimeMillis();
    private static final String TEST_FILE_PARENT_DEPOT_PATH = "//depot" + RELATIVE_DEPOT_PATH;
    private IChangelist changelist = null;
    private List<IFileSpec> submittedOrPendingFileSpecs = null;
    private Path clientPath;
    private MD5Digester md5Digester = new MD5Digester();
    private byte[] UTF_16BE_BOM = new byte[]{(byte) 0xFE, (byte) 0xFF};
    private byte[] UTF_16LE_BOM = new byte[]{(byte) 0xFF, (byte) 0xFE};
    private static final String UTF_16BE_WITH_BOM = "utf_16be_with_bom_";

    @ClassRule
	public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", SubmitAndSyncUtf16BETest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }
    /**
     * Test that when a big endian file, with CRLF as a line ending, is submitted using a workspace
     * client that specifies a unix line ending the CR characters are left in and a subsequent sync
     * will match the endian characteristics of the local platform.
     * @throws Exception - when any error occurs
     */
    @Test
    public void testUnixClient() throws Exception {
        login("utf8");
        connectToServer("p4TestUnixLineend");

        clientPath = getClientPath(RELATIVE_DEPOT_PATH);
        String utf16BEFileName = UTF_16BE_WITH_BOM + System.currentTimeMillis() + ".txt";
        byte[] fileContent = "\0a\0\r\0\n\0b\0\r\0\n\0c\0\r\0\n\0d".getBytes();
        byte[] fileContentBeforeSubmit = new byte[2 + fileContent.length];
        System.arraycopy(UTF_16BE_BOM, 0, fileContentBeforeSubmit, 0, UTF_16BE_BOM.length);
        System.arraycopy(fileContent,
                0,
                fileContentBeforeSubmit,
                UTF_16BE_BOM.length,
                fileContent.length);

        byte[] fileContentLE = "a\0\r\0\n\0b\0\r\0\n\0c\0\r\0\n\0d\0".getBytes();
        byte[] fileContentExpectedAfterSyncSubmit = new byte[2 + fileContentLE.length];
        System.arraycopy(UTF_16LE_BOM, 0, fileContentExpectedAfterSyncSubmit, 0, UTF_16LE_BOM.length);
        System.arraycopy(fileContentLE,
                0,
                fileContentExpectedAfterSyncSubmit,
                UTF_16LE_BOM.length,
                fileContentLE.length);

        Path tmpUtf16BEFile = createTmpUtf16BEFile(utf16BEFileName, fileContent);
        String fileChecksumBeforeSubmit = md5Digester.digestFileAs32ByteHex(tmpUtf16BEFile.toFile(), Charset.forName("UTF-16BE"));

        // submit
        String fileDepotPath = TEST_FILE_PARENT_DEPOT_PATH + "/" + utf16BEFileName;
        List<IFileSpec> submittingFileSpecs = FileSpecBuilder.makeFileSpecList(fileDepotPath);
        changelist = submitFileToDepot(submittingFileSpecs);
        assertThat(changelist, notNullValue());

        // verify
        verifyServerSideFileType(fileDepotPath, "utf16");
        verifyServerSideFileSize(fileDepotPath, fileContent.length/2);

        // force sync
        List<IFileSpec> files = client.sync(
                FileSpecBuilder.makeFileSpecList(fileDepotPath),
                new SyncOptions().setForceUpdate(true));
        if (files.size() < 1) {
            fail("Sync test file: " + fileDepotPath + "failed");
        }
        IFileSpec fileSpec = files.get(0);
        assertThat(fileSpec.getDepotPathString(), containsString(fileDepotPath));

        // verify md5 checksum
        String fileChecksumAfterSync = getMd5ChecksumLE(tmpUtf16BEFile);
        assertThat(fileChecksumAfterSync, is(fileChecksumBeforeSubmit));

        // verify raw bytes
        byte[] fileContentAfterSync = Files.readAllBytes(tmpUtf16BEFile);
        assertArrayEquals("The output after sync should be UTF16LE with win line endings",
        		fileContentExpectedAfterSyncSubmit, fileContentAfterSync);
    }

    /**
     * Test that when a big endian file, with CRLF as a line ending, is submitted using a workspace
     * client that specifies a windows line ending the CR characters are removed and a subsequent
     * sync into a unix client will match the endian characteristics of the local platform.
     * @throws Exception - when any error occurs
     */
    @Test
    public void testWindowsSubmitUnixSyncClient() throws Exception {
        login("utf8");
        connectToServer("p4TestWinLineend");

        clientPath = getClientPath(RELATIVE_DEPOT_PATH);
        String utf16BEFileName = UTF_16BE_WITH_BOM + System.currentTimeMillis() + ".txt";
        byte[] fileContent = "\0a\0\r\0\n\0b\0\r\0\n\0c\0\r\0\n\0d\0\r\0\n\0e".getBytes();
        byte[] fileContentBeforeSubmit = new byte[2 + fileContent.length];
        System.arraycopy(UTF_16BE_BOM, 0, fileContentBeforeSubmit, 0, UTF_16BE_BOM.length);
        System.arraycopy(fileContent,
                0,
                fileContentBeforeSubmit,
                UTF_16BE_BOM.length,
                fileContent.length);

        byte[] fileContentLE = "a\0\n\0b\0\n\0c\0\n\0d\0\n\0e\0".getBytes();
        byte[] fileContentExpectedAfterSyncSubmit = new byte[2 + fileContentLE.length];
        System.arraycopy(UTF_16LE_BOM, 0, fileContentExpectedAfterSyncSubmit, 0, UTF_16LE_BOM.length);
        System.arraycopy(fileContentLE,
                0,
                fileContentExpectedAfterSyncSubmit,
                UTF_16LE_BOM.length,
                fileContentLE.length);

        Path tmpUtf16BEFile = createTmpUtf16BEFile(utf16BEFileName, fileContent);
        /*
         *  The original file is utf-16BE with windows line endings (CRLF), the line endings will
         *  be normalized by the submit.
         *  
         *  To compare before and after, we need to work out the md5 by normalizing from utf-16BE
         *  with CRLF to utf-8 LF.
         */
        String fileChecksumBeforeSubmit = md5Digester.digestFileAs32ByteHex(tmpUtf16BEFile.toFile(),
                Charset.forName("UTF-16BE"), true, ClientLineEnding.FST_L_CRLF);

        // submit
        String fileDepotPath = TEST_FILE_PARENT_DEPOT_PATH + "/" + utf16BEFileName;
        List<IFileSpec> submittingFileSpecs = FileSpecBuilder.makeFileSpecList(fileDepotPath);
        changelist = submitFileToDepot(submittingFileSpecs);
        // TODO: This assertion is pointless, we should be checking the submit status
        assertNotNull("A changelist was not returned for a submit", changelist);

        // verify file type as utf16, there was a bom present
        verifyServerSideFileType(fileDepotPath, "utf16");
        // Verify that the server converted from utf16 to utf8 and stripped both CR and the bom
        verifyServerSideFileSize(fileDepotPath, fileContent.length/2 - 4);

        // Sync as Unix client, this changes the line endings
        connectToServer("p4TestUnixLineend");

        List<IFileSpec> files = client.sync(
                FileSpecBuilder.makeFileSpecList(fileDepotPath),
                new SyncOptions().setForceUpdate(true));
        if (files.size() < 1) {
            fail("Sync test file: " + fileDepotPath + "failed");
        }
        IFileSpec fileSpec = files.get(0);
        assertTrue("The sync operation failed, " + fileSpec,
                fileSpec.getDepotPathString().contains(fileDepotPath));

        /*
         *  The sync'ed file will be utf-16LE with linux line endings (LF), the line endings will
         *  be normalized by the submit.
         *  
         *  To compare before and after, we need to work out the md5 by normalizing from utf-16BE
         *  with CRLF to utf-8 LF.
         */
        String fileChecksumAfterSync = getMd5ChecksumLE(tmpUtf16BEFile);
        assertEquals("The checksum of <" + tmpUtf16BEFile + ">, changed after the sync.",
                fileChecksumAfterSync, fileChecksumBeforeSubmit);

        // verify raw bytes
        byte[] fileContentAfterSync = Files.readAllBytes(tmpUtf16BEFile);
        assertArrayEquals("The output after sync should be UTF16LE with unix line endings",
        		fileContentExpectedAfterSyncSubmit, fileContentAfterSync);
    }


    private void login(String clientCharset) throws P4JavaException {
        setClientCharsetIfServerSupportUnicode(server, clientCharset);
        server.setUserName(getUserName());
        server.login(getPassword(), new LoginOptions());
    }

    private Path createTmpUtf16BEFile(String utf16BEFileName, byte[] fileContent) throws Exception {
        Path testFile = clientPath.resolve(utf16BEFileName);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(100);
        outputStream.write(UTF_16BE_BOM);
        outputStream.write(fileContent);
        com.google.common.io.Files.write(outputStream.toByteArray(), testFile.toFile());
        return testFile;
    }

    private Path getClientPath(String relativeDepotPath) throws IOException {
        Path clientFileParentPath = Paths.get(client.getRoot(), relativeDepotPath);
        if (Files.notExists(clientFileParentPath)) {
            Files.createDirectories(clientFileParentPath);
        }

        return clientFileParentPath;
    }

    private String getMd5ChecksumLE(Path file) {
        return md5Digester.digestFileAs32ByteHex(file.toFile(), Charset.forName("UTF-16LE"));
    }

    @After
    public void tearDown() throws Exception {
        revertChangelist(changelist, submittedOrPendingFileSpecs);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
