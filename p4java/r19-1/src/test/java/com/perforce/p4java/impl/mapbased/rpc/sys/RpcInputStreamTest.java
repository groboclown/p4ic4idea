package com.perforce.p4java.impl.mapbased.rpc.sys;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Spy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;

/**
 * RpcInputStream Tester.
 */
public class RpcInputStreamTest extends AbstractP4JavaUnitTest {
    /*
     * od -c utf8_win_line_endings.txt
     *  ﻿ 0000000 a \r \n b \r \n
     *   0000006
     */
    private String mockFileName = loadFileFromClassPath(
            "com/perforce/p4java/impl/mapbased/rpc/sys/utf8_win_line_endings.txt").getPath();
    private RpcPerforceFile file;
    @Spy
    private RpcInputStream rpcInputStream;

    @Before
    public void beforeEach() throws IOException, FileEncoderException {
        file = new RpcPerforceFile(mockFileName, RpcPerforceFileType.FST_UTF8,
                ClientLineEnding.FST_L_CRLF);
        rpcInputStream = new RpcInputStream(file, null);
    }

    @After
    public void afterEach() throws Exception {
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_with_null_file() throws IOException, FileEncoderException {
        RpcInputStream stream = new RpcInputStream(null, null);
        stream.close();
    }

    @Test
    public void testConstructor_with_nonnull_file_1() throws IOException {
        // assertThat(rpcInputStream.getFileType(), is(file.getFileType()));
        assertThat(rpcInputStream, instanceOf(RpcInputStream.class));
    }

    @Test
    public void testConstructor_with_nonnull_file_and_set_charsetis_non_utf8()
            throws IOException, FileEncoderException {
        rpcInputStream = new RpcInputStream(file, Charset.forName("ISO-8859-1"));
        // assertThat(rpcInputStream.getFileType(), is(file.getFileType()));
        assertThat(rpcInputStream, instanceOf(RpcInputStream.class));
    }

    @Test
    public void testConstructor_with_nonnull_file_with_charset()
            throws IOException, FileEncoderException {
        rpcInputStream = new RpcInputStream(file, StandardCharsets.UTF_8);

        // assertThat(rpcInputStream.getFileType(), is(file.getFileType()));
        assertThat(rpcInputStream, instanceOf(RpcInputStream.class));
    }

    @Test
    public void testConstructor_with_nonnull_file_and_need_lineEndingFiltering()
            throws IOException, FileEncoderException {
        mockCRLRLineEndingAndUTF8EncodingFile();

        assertNotNull(rpcInputStream);
        assertThat(rpcInputStream, instanceOf(RpcInputStream.class));
    }

    /**
     * Method: close()
     */
    @Test(expected = IOException.class)
    public void testClose_with_normal_FileInputStream() throws Exception {
        rpcInputStream.close();
        byte[] targetBytes = new byte[1001];
        rpcInputStream.read(targetBytes);
    }

    /**
     * Method: close()
     */
    @Test(expected = IOException.class)
    public void testClose_with_normal_RpcLineEndFilterInputStream() throws Exception {
        mockCRLRLineEndingAndUTF8EncodingFile();
        assertNotNull(rpcInputStream);
        rpcInputStream.close();
        byte[] targetBytes = new byte[1001];
        rpcInputStream.read(targetBytes);
    }

    /**
     * Method: read()
     */
    @Test(expected = UnimplementedError.class)
    public void testRead() throws Exception {
        rpcInputStream.read();
    }

    /**
     * Method: read(byte[] targetBytes, int targetOffset, int
     * targetLen)
     */
    @Test(expected = NullPointerError.class)
    public void testReadForTargetBytesTargetOffsetTargetLen_throw_exception_if_targetBytes_isNull()
            throws Exception {
        rpcInputStream.read(null, 1, 10);
    }

    @Test(expected = P4JavaError.class)
    public void testReadForTargetBytesTargetOffsetTargetLen_throw_exception_if_offset_less_than_zero()
            throws Exception {
        byte[] targetBytes = new byte[1001];
        rpcInputStream.read(targetBytes, -1, 10);
    }

    @Test(expected = P4JavaError.class)
    public void testReadForTargetBytesTargetOffsetTargetLen_throw_exception_if_target_length_less_than_zero()
            throws Exception {
        byte[] targetBytes = new byte[1001];
        rpcInputStream.read(targetBytes, 0, -1);
    }

    @Test(expected = P4JavaError.class)
    public void testReadForTargetBytesTargetOffsetTargetLen_throw_exception_if_target_length_great_than_length_of_targetBytes()
            throws Exception {
        byte[] targetBytes = new byte[10];
        rpcInputStream.read(targetBytes, 1, 15);
    }

    @Test
    public void testReadForTargetBytes() throws Exception {
        byte[] targetBytes = new byte[1001];
        int read = rpcInputStream.read(targetBytes);
        int fileLengthExcludeBom = Files.readAllBytes(file.toPath()).length - 2;
        assertThat(read, is(fileLengthExcludeBom));
    }

    @Test
    public void testReadForTargetBytesTargetOffsetTargetLen_withoutExpectedTextFileType()
            throws Exception {
        byte[] targetBytes = new byte[1001];
        int read = rpcInputStream.read(targetBytes, 0, 1000);
        int fileLengthExcludeBom = Files.readAllBytes(file.toPath()).length - 2;
        assertThat(read, is(fileLengthExcludeBom));
    }

    @Test
    public void testReadForTargetBytesTargetOffsetTargetLen_read_from_RpcLineEndFilterInputStream()
            throws Exception {
        mockCRLRLineEndingAndUTF8EncodingFile();

        byte[] targetBytes = new byte[1001];

        int read = rpcInputStream.read(targetBytes, 0, 1000);
        /*
         * od -c utf8_win_line_endings.txt
         *  ﻿ 0000000 a \r \n b \r \n
         *   0000006
         * 
         * after replace line ending with P4D server line ending
         * 
         *   0000000 a \n b \n
         */
        int expectedLengthAsCRLFWasReplaceByLF = 4;
        assertThat(read, is(expectedLengthAsCRLFWasReplaceByLF));
    }

    @Test
    public void testReadUtf16LEWithBomAndUnixLineEnding() throws IOException, FileEncoderException {
        mockFileName = loadFileFromClassPath(
                "com/perforce/p4java/common/io/utf-16le_with_bom_unix_line_ending_ko.txt")
                        .getPath();
        file = new RpcPerforceFile(mockFileName, RpcPerforceFileType.FST_UTF16);
        rpcInputStream = new RpcInputStream(file, StandardCharsets.UTF_16LE);

        byte[] targetBytes = new byte[1001];
        int read = rpcInputStream.read(targetBytes, 0, 1000);
        int fileLengthExcludeBom = 343;
        assertThat(read, is(fileLengthExcludeBom));
    }

    @Test
    public void testReadUtf16LEWithBomAndWinLineEnding() throws IOException, FileEncoderException {
        mockFileName = loadFileFromClassPath(
                "com/perforce/p4java/common/io/utf_16LE_win_line_ending.txt").getPath();
        file = new RpcPerforceFile(mockFileName, RpcPerforceFileType.FST_UTF16,
                ClientLineEnding.FST_L_CRLF);
        rpcInputStream = new RpcInputStream(file, StandardCharsets.UTF_16LE);

        byte[] targetBytes = new byte[1001];
        int read = rpcInputStream.read(targetBytes, 0, 1000);
        assertThat(read, is(5));

        file.setFileType(RpcPerforceFileType.FST_UTF16);
        rpcInputStream = new RpcInputStream(file, null);
        read = rpcInputStream.read(targetBytes, 0, 1000);
        int fileLengthExcludeBom = Files.readAllBytes(file.toPath()).length - 2;
        assertThat(read, is(fileLengthExcludeBom));
    }

    @Test
    public void testReadHasUtf16LEBomButItaAudioFile() throws IOException, FileEncoderException {
        File file = loadFileFromClassPath(
                "com/perforce/p4java/common/io/file_has_utf_16LE_bom_but_its_actually_a_audio_file.mp1");
        mockFileName = file.getPath();
        this.file = new RpcPerforceFile(mockFileName, RpcPerforceFileType.FST_BINARY);
        rpcInputStream = new RpcInputStream(this.file, null);

        int length = (int) file.length(); // this file will not overflow for int
        byte[] targetBytes = new byte[length];
        int read = rpcInputStream.read(targetBytes, 0, length);
        assertThat(read, is(length));
    }

    private void mockCRLRLineEndingAndUTF8EncodingFile() throws IOException, FileEncoderException {
        char[] chars = { (char) 51, (char) 51 };
        String lineEndings = new String(chars);
        file = new RpcPerforceFile(mockFileName, lineEndings);
        file.setFileType(RpcPerforceFileType.FST_UTF8);
        rpcInputStream = new RpcInputStream(file, null);
    }
}
