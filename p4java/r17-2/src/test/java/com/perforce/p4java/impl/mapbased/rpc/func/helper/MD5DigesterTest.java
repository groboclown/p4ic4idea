package com.perforce.p4java.impl.mapbased.rpc.func.helper;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcUnicodeInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * MD5Digester Tester.
 *
 * @author Sean
 * @version 1.0
 * @since Jul 11, 2016
 */
@RunWith(JUnitPlatform.class)
public class MD5DigesterTest {
    private MD5Digester md5Digester;
    private static File testFile;
    private static String expectedTestFileMd5 = "566DA5D93D0F71FFAA2FED952081BE3F";
    private static String expectedTestFileMd5Win = "B3EA4FC48B20C4F8F0263CBBB7562A76";
    private static String expectedTestFileMd5WinNonNullEnding = "74B41CEF4E60E72C5C9C078692971F90";
    private static boolean isWindows;

    @BeforeAll
    public static void beforeAll() {
        isWindows = System.getProperty("os.name").startsWith("Windows");
        testFile = new File(MD5DigesterTest.class.getClassLoader()
                .getResource(
                        "com/perforce/p4java/impl/mapbased/rpc/func/helper/md5_digest_test.txt")
                .getFile());
    }

    @BeforeEach
    public void beforeEach() {
        md5Digester = new MD5Digester();
    }

    /**
     * Method: update(byte[] bytes)
     */
    @DisplayName("update(byte[] bytes) with non-null argument")
    @Test
    public void testUpdateBytes_withNonNullArgument() throws Exception {
        byte[] bytes = new byte[] {};
        MessageDigest mockMessageDigest = mock(MessageDigest.class);
        md5Digester.setMessageDigest(mockMessageDigest);

        md5Digester.update(bytes);
        verify(mockMessageDigest, times(1)).update(bytes);
    }

    /**
     * Method: update(byte[] bytes)
     */
    @DisplayName("update(byte[] bytes)  with given null argument")
    @Test
    public void testUpdateBytes_withNullArgument() throws Exception {
        byte[] bytes = null;
        MessageDigest mockMessageDigest = mock(MessageDigest.class);
        md5Digester.setMessageDigest(mockMessageDigest);

        md5Digester.update(bytes);
        verify(mockMessageDigest, times(0)).update(bytes);
    }

    /**
     * Method: update(String value)
     */
    @DisplayName("update(String value) without exception")
    @Test
    public void testUpdateString_withoutException() throws Exception {
        String value = "abc123";
        MessageDigest mockMessageDigest = mock(MessageDigest.class);
        md5Digester.setMessageDigest(mockMessageDigest);
        md5Digester.update(value);

        verify(mockMessageDigest, times(1)).update(value.getBytes(CharsetDefs.UTF8.name()));
    }

    /**
     * Method: update(String value)
     */
    @DisplayName("update(String value) with exception")
    @Test
    public void testUpdateString_withException() throws Exception {
        String value = "abc123";
        MessageDigest mockMessageDigest = mock(MessageDigest.class);
        doThrow(UnsupportedEncodingException.class).when(mockMessageDigest)
                .update(any(byte[].class));
        md5Digester.setMessageDigest(mockMessageDigest);

        Assertions.expectThrows(P4JavaError.class, () -> md5Digester.update(value));
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset, boolean
     * doesNeedConvertLineEndings)
     */
    @Test
    public void testDigestFileAs32ByteHexForFileCharsetConvertLineEndings() throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile, Charset.forName("UTF-8"), true);
        assertThat(actual, is(expectedTestFileMd5));
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset, boolean
     * doesNeedConvertLineEndings, ClientLineEnding clientLineEnding)
     */
    @Test
    public void testDigestFileAs32ByteHexForFileCharsetConvertLineEndingsClientLineEnding()
            throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile, Charset.forName("UTF-8"), false,
                null);
        assertThat(actual, is(isWindows ? expectedTestFileMd5Win : expectedTestFileMd5));
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset, boolean
     * doesNeedConvertLineEndings, ClientLineEnding clientLineEnding)
     */
    @Test
    public void testDigestFileAs32ByteHexForFileCharsetConvertLineEndingsClientLineEndingNonCharset()
            throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile, null, false, null);
        assertThat(actual, is(isWindows ? expectedTestFileMd5Win : expectedTestFileMd5));
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset, boolean
     * doesNeedConvertLineEndings, ClientLineEnding clientLineEnding)
     */
    @Test
    public void testDigestFileAs32ByteHexForFileCharsetConvertLineEndingsClientLineEndingNonCharset_needConvertLindEnding()
            throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile, Charset.forName("UTF-8"), true,
                null);
        assertThat(actual, is(expectedTestFileMd5));
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset, boolean
     * doesNeedConvertLineEndings, ClientLineEnding clientLineEnding)
     */
    @DisplayName("test digestFileAs32ByteHex(File file, Charset charset, boolean doesNeedConvertLineEndings, ClientLineEnding clientLineEnding) throw exception")
    @Test
    public void testDigestFileAs32ByteHexForFileCharsetConvertLineEndingsClientLineEndingNonCharset_throwException()
            throws Exception {
        File dirAsFile = new File(System.getProperty("java.io.tmpdir"));
        String actual = md5Digester.digestFileAs32ByteHex(dirAsFile, Charset.forName("UTF-8"), true,
                null);
        assertNull(actual);
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset, boolean
     * doesNeedConvertLineEndings, ClientLineEnding clientLineEnding)
     */
    @Test
    public void testDigestFileAs32ByteHexForFileCharsetConvertLineEndingsClientLineEndingNonCharset_nonNullClientLineEnding()
            throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile, Charset.forName("UTF-8"), true,
                ClientLineEnding.FST_L_CR);
        assertThat(actual,
                is(isWindows ? expectedTestFileMd5WinNonNullEnding : expectedTestFileMd5));
    }

    /**
     * Method: digestFileAs32ByteHex(File file)
     */
    @Test
    public void testDigestFileAs32ByteHexFile() throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile);
        assertThat(actual, is(isWindows ? expectedTestFileMd5Win : expectedTestFileMd5));
    }

    /**
     * Method: digestFileAs32ByteHex(File file, Charset charset)
     */
    @Test
    public void testDigestFileAs32ByteHex_file_charset() throws Exception {
        String actual = md5Digester.digestFileAs32ByteHex(testFile, Charset.forName("UTF-8"));
        assertThat(actual, is(isWindows ? expectedTestFileMd5Win : expectedTestFileMd5));
    }

    @DisplayName("test digestFileAs32ByteHexFile(File file) throws an exception")
    @Test
    public void testDigestFileAs32ByteHexFile_withException() throws Exception {
        File dirAsFile = new File(System.getProperty("java.io.tmpdir"));

        String actual = md5Digester.digestFileAs32ByteHex(dirAsFile);
        assertNull(actual);
    }

    /**
     * Method: digestStream(InputStream inStream, boolean convertLineEndings,
     * ClientLineEnding clientLineEnding)
     */
    @DisplayName("test digestStream() with normal file")
    @Test
    public void testDigestStream1() throws Exception {
        InputStream in = new FileInputStream(getClass().getClassLoader()
                .getResource(
                        "com/perforce/p4java/impl/mapbased/rpc/func/helper/md5_digest_test.txt")
                .getFile());
        ClientLineEnding clientLineEnding = ClientLineEnding.FST_L_CR;
        Method method = getPrivate_method_of_digestStream();
        method.setAccessible(true);
        method.invoke(md5Digester, in, true, clientLineEnding);
    }

    @DisplayName("test digestStream() with mock bytes")
    @Test
    public void testDigestStream2() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchAlgorithmException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\r', '\n', '\r', '\n', 8, 11, 59, '\n', '\r',
                '\n' };
        InputStream inStream = new ByteArrayInputStream(sourceBytes);

        Method method = getPrivate_method_of_digestStream();
        MD5Digester mock = new MD5Digester(4);
        method.invoke(mock, inStream, true, ClientLineEnding.FST_L_CRLF);
        byte[] actual = mock.digestAsBytes();

        // expected
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(new byte[] { 2, 5, 7, '\n', '\n', 8, 11, 59, '\n', '\n' });
        byte[] expect = messageDigest.digest();

        assertThat(actual, is(expect));
    }

    private Method getPrivate_method_of_digestStream() throws NoSuchMethodException {
        Method method = MD5Digester.class.getDeclaredMethod("digestStream", InputStream.class,
                boolean.class, ClientLineEnding.class);
        method.setAccessible(true);

        return method;
    }

    @Test
    public void testIsRequireConvertClientOrLocalLineEndingToServerFormat_withNullArguments()
            throws Exception {
        boolean expected = ClientLineEnding.CONVERT_TEXT;
        ClientLineEnding nullObj = null;
        Method method = MD5Digester.class.getDeclaredMethod(
                "isRequireConvertClientOrLocalLineEndingToServerFormat", ClientLineEnding.class);
        method.setAccessible(true);
        boolean actual = (boolean) method.invoke(md5Digester, nullObj);
        assertThat(actual, is(expected));
    }

    @Test
    public void testIsRequireConvertClientOrLocalLineEndingToServerFormat_withNonNullArguments()
            throws Exception {
        Method method = MD5Digester.class.getDeclaredMethod(
                "isRequireConvertClientOrLocalLineEndingToServerFormat", ClientLineEnding.class);
        method.setAccessible(true);
        boolean actual = (boolean) method.invoke(md5Digester, ClientLineEnding.FST_L_CR);

        assertThat(actual, is(true));
    }

    @DisplayName("findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding() with need covert client line ending argument")
    @Test
    public void testFindPotentialOffsetOfNextClientLineEnding_needConvertText() throws Exception {
        ClientLineEnding clientLineEnding = ClientLineEnding.FST_L_CR;
        byte lastByte = ClientLineEnding.FST_L_CR_BYTES[0];

        Method method = MD5Digester.class.getDeclaredMethod(
                "findOffsetOfNextClientLineEndingIfReadBytesEndWithFirstByteOfClientLineEnding",
                byte.class, ClientLineEnding.class);
        method.setAccessible(true);
        int actual = (int) method.invoke(md5Digester, lastByte, clientLineEnding);
        int expected = ClientLineEnding.FST_L_CR_BYTES.length - 1;
        assertThat(actual, is(expected));
    }

    @DisplayName("update(ByteBuffer byteBuf) with nonNull argument")
    @Test
    public void testUpdate_with_nonNull_argument() {
        byte[] mockBytes = { 1, 2, 3 };
        ByteBuffer byteBuffer = ByteBuffer.wrap(mockBytes, 0, 3);
        MessageDigest mockMessageDigest = mock(MessageDigest.class);
        md5Digester.setMessageDigest(mockMessageDigest);
        md5Digester.update(byteBuffer);

        verify(mockMessageDigest, times(1)).update(mockBytes);
    }

    @DisplayName("update(ByteBuffer byteBuf) with nonNull argument")
    @Test
    public void testUpdate_with_null_argument() {
        ByteBuffer byteBuffer = null;
        MessageDigest mockMessageDigest = mock(MessageDigest.class);
        md5Digester.setMessageDigest(mockMessageDigest);
        md5Digester.update(byteBuffer);

        verify(mockMessageDigest, times(0)).update(any(byte[].class));
    }

    @DisplayName("doesSourceBytesUseSameClientLineEnding() and it's converted")
    @Test
    public void testIsAlreadyConverted_return_converted()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\n', '\r', '\n', 12, 13 };
        int indexOfSourceBytes = 3;
        int length = 8;
        byte[] clientLineEndBytes = new byte[] { '\n', '\r', '\n' };

        boolean actual = invokePrivate_isAlreadyConverted(sourceBytes, indexOfSourceBytes, length,
                clientLineEndBytes);

        assertThat(actual, is(true));
    }

    @DisplayName("doesSourceBytesUseSameClientLineEnding() and it's not converted but first byte is same")
    @Test
    public void testIsAlreadyConverted_not_converted_but_firstByteIsSame()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\n', '\n', '\r', '\n', 12, 13 };
        int indexOfSourceBytes = 3;
        int length = 8;
        byte[] clientLineEndBytes = new byte[] { '\n', '\r', '\n' };

        boolean actual = invokePrivate_isAlreadyConverted(sourceBytes, indexOfSourceBytes, length,
                clientLineEndBytes);

        assertThat(actual, is(false));
    }

    @DisplayName("doesSourceBytesUseSameClientLineEnding() and it's not converted")
    @Test
    public void testIsAlreadyConverted_not_converted()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\r', '\n', '\r', '\n', 12, 13 };
        int indexOfSourceBytes = 3;
        int length = 8;
        byte[] clientLineEndBytes = new byte[] { '\n', '\r', '\n' };

        boolean actual = invokePrivate_isAlreadyConverted(sourceBytes, indexOfSourceBytes, length,
                clientLineEndBytes);

        assertThat(actual, is(false));
    }

    private boolean invokePrivate_isAlreadyConverted(byte[] sourceBytes,
            final int indexOfSourceBytes, final int length, byte[] clientLineEndBytes)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = MD5Digester.class.getDeclaredMethod(
                "doesSourceBytesUseSameClientLineEnding", byte[].class, int.class, int.class,
                byte[].class);
        method.setAccessible(true);
        return (boolean) method.invoke(md5Digester, sourceBytes, indexOfSourceBytes, length,
                clientLineEndBytes);
    }

    @DisplayName("test convertToP4dServerEndingsIfRequired() - 1. source bytes use same client line endings")
    @Test
    public void testConvertToP4dServerEndingsIfRequired_sourceBytesUseSameClientLineEndings()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\r', '\n', 12, 13 };
        int start = 3;
        int length = 7;
        ClientLineEnding clientLineEnding = ClientLineEnding.FST_L_CRLF;

        ByteBuffer actual = invokePrivate_convertToP4dServerEndingsIfRequired(sourceBytes, start,
                length, clientLineEnding);
        ByteBuffer expected = ByteBuffer.wrap(new byte[] { '\n', 12, 13 }, 0, 3);
        assertThat(actual, is(expected));
    }

    @DisplayName("test convertToP4dServerEndingsIfRequired() - 2. source bytes use same client line endings")
    @Test
    public void testConvertToP4dServerEndingsIfRequired()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\n', '\r', '\n', 12, 13 };
        int start = 3;
        int length = 8;
        ClientLineEnding clientLineEnding = ClientLineEnding.FST_L_CRLF;

        ByteBuffer actual = invokePrivate_convertToP4dServerEndingsIfRequired(sourceBytes, start,
                length, clientLineEnding);
        ByteBuffer expected = ByteBuffer.wrap(new byte[] { '\n', '\n', 12, 13 }, 0, 4);
        assertThat(actual, is(expected));
    }

    @DisplayName("test convertToP4dServerEndingsIfRequired() - 3. is not require convert client or local line ending to server format")
    @Test
    public void testConvertToP4dServerEndingsIfRequired3()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\n', '\r', '\n', 12, 13 };
        int start = 3;
        int length = 5;
        ClientLineEnding clientLineEnding = ClientLineEnding.FST_L_LF;

        ByteBuffer actual = invokePrivate_convertToP4dServerEndingsIfRequired(sourceBytes, start,
                length, clientLineEnding);
        ByteBuffer expected = ByteBuffer.wrap(sourceBytes, start, length);
        assertThat(actual, is(expected));
    }

    private ByteBuffer invokePrivate_convertToP4dServerEndingsIfRequired(
            @Nonnull byte[] sourceBytes, final int start, final int length,
            @Nullable ClientLineEnding clientLineEnding)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = MD5Digester.class.getDeclaredMethod("convertToP4dServerEndingsIfRequired",
                byte[].class, int.class, int.class, ClientLineEnding.class);
        method.setAccessible(true);
        return (ByteBuffer) method.invoke(md5Digester, sourceBytes, start, length,
                clientLineEnding);
    }

    @DisplayName("test findAndReplaceNonEncodedClientLineEndingIfRequireLineEndingConvert() - 1. verify if last byte ends with first byte of client line ending.")
    @Test
    public void testContinueReadBytesIfReadBytesEndWithFirstByteOfClientLineEnding2()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            IOException {
        byte[] sourceBytes = new byte[] { 2, 5, 7, '\r', '\n', '\r', '\n', 8, 11, 59, '\n', '\r',
                '\n' };
        InputStream inStream = new ByteArrayInputStream(sourceBytes);
        byte[] readBuffer = new byte[10];
        readBuffer[0] = 2;
        readBuffer[1] = 5;
        readBuffer[2] = 7;
        readBuffer[3] = '\r';
        int totalBytesReadIntoBuffer = 4;
        inStream.read(new byte[totalBytesReadIntoBuffer]);

        ByteBuffer actual = invokePrivate_findAndReplaceNonEncodedClientLineEndingIfRequireLineEndingConvert(
                inStream, readBuffer, totalBytesReadIntoBuffer, ClientLineEnding.FST_L_CRLF);

        byte[] expectedBytes = new byte[] { 2, 5, 7, '\n' };
        ByteBuffer expected = ByteBuffer.wrap(expectedBytes, 0, 4);

        assertThat(actual, equalTo(expected));
    }

    private ByteBuffer invokePrivate_findAndReplaceNonEncodedClientLineEndingIfRequireLineEndingConvert(
            @Nonnull InputStream inStream, @Nonnull final byte[] readBuffer,
            final int totalBytesReadIntoBuffer, @Nullable ClientLineEnding clientLineEnding)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = MD5Digester.class.getDeclaredMethod(
                "findAndReplaceNonEncodedClientLineEndingIfRequireLineEndingConvert",
                InputStream.class, byte[].class, int.class, ClientLineEnding.class);
        method.setAccessible(true);

        return (ByteBuffer) method.invoke(md5Digester, inStream, readBuffer,
                totalBytesReadIntoBuffer, clientLineEnding);
    }

    @Test
    public void test_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert()
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        byte[] sourceBytes = new byte[] { 'q', 'c', 'e', '\r', '\n', '\r', '\n', 'd', 'h', 'b',
                '\n', '\r', '\n' };
        RpcUnicodeInputStream unicodeInputStream = new RpcUnicodeInputStream(
                new ByteArrayInputStream(sourceBytes));
        InputStreamReader encodedStreamReader = new InputStreamReader(unicodeInputStream,
                StandardCharsets.US_ASCII);
        CharsetEncoder utf8CharsetEncoder = CharsetDefs.UTF8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        char[] buffer = new char[5];
        buffer[0] = 'q';
        buffer[1] = 'c';
        buffer[2] = 'e';
        buffer[3] = '\r';
        ByteBuffer utf8ByteBuffer = utf8CharsetEncoder.encode(CharBuffer.wrap(buffer, 0, 4));
        encodedStreamReader.read(new char[4]);

        ByteBuffer actual = invokePrivate_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert(
                encodedStreamReader, utf8CharsetEncoder, utf8ByteBuffer,
                ClientLineEnding.FST_L_CRLF);

        byte[] expectedBytes = new byte[] { 'q', 'c', 'e', '\n' };
        ByteBuffer expected = ByteBuffer.wrap(expectedBytes, 0, 4);

        assertThat(actual, is(expected));
    }

    @Test
    public void test_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert_2()
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        byte[] sourceBytes = new byte[] { 'q', 'c', 'e', '\r', '\n', '\r', '\n', 'd', 'h', 'b',
                '\n', '\r', '\n' };
        RpcUnicodeInputStream unicodeInputStream = new RpcUnicodeInputStream(
                new ByteArrayInputStream(sourceBytes));
        InputStreamReader encodedStreamReader = new InputStreamReader(unicodeInputStream,
                StandardCharsets.US_ASCII);
        CharsetEncoder utf8CharsetEncoder = CharsetDefs.UTF8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        char[] buffer = new char[7];
        buffer[0] = 'q';
        buffer[1] = 'c';
        buffer[2] = 'e';
        buffer[3] = '\r';
        buffer[4] = '\n';
        ByteBuffer utf8ByteBuffer = utf8CharsetEncoder.encode(CharBuffer.wrap(buffer, 0, 5));
        encodedStreamReader.read(new char[5]);

        ByteBuffer actual = invokePrivate_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert(
                encodedStreamReader, utf8CharsetEncoder, utf8ByteBuffer,
                ClientLineEnding.FST_L_CRLF);

        byte[] expectedBytes = new byte[] { 'q', 'c', 'e', '\n' };
        ByteBuffer expected = ByteBuffer.wrap(expectedBytes, 0, 4);

        assertThat(actual, is(expected));
    }

    @Test
    public void test_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert_3()
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        byte[] sourceBytes = new byte[] { 'q', 'c', 'e', '\r', '\n', '\r', '\n', 'd', 'h', 'b',
                '\n', '\r', '\n' };
        RpcUnicodeInputStream unicodeInputStream = new RpcUnicodeInputStream(
                new ByteArrayInputStream(sourceBytes));
        InputStreamReader encodedStreamReader = new InputStreamReader(unicodeInputStream,
                StandardCharsets.US_ASCII);
        CharsetEncoder utf8CharsetEncoder = CharsetDefs.UTF8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        char[] buffer = new char[9];
        buffer[0] = 'q';
        buffer[1] = 'c';
        buffer[2] = 'e';
        buffer[3] = '\r';
        buffer[4] = '\n';
        buffer[5] = '\r';
        buffer[6] = '\n';
        buffer[7] = 'd';
        ByteBuffer utf8ByteBuffer = utf8CharsetEncoder.encode(CharBuffer.wrap(buffer, 0, 8));
        encodedStreamReader.read(new char[8]);

        ByteBuffer actual = invokePrivate_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert(
                encodedStreamReader, utf8CharsetEncoder, utf8ByteBuffer,
                ClientLineEnding.FST_L_CRLF);

        byte[] expectedBytes = new byte[] { 'q', 'c', 'e', '\n', '\n', 'd' };
        ByteBuffer expected = ByteBuffer.wrap(expectedBytes, 0, 6);

        assertThat(actual, is(expected));
    }

    private ByteBuffer invokePrivate_findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert(
            @Nonnull InputStreamReader encodedStreamReader,
            @Nonnull CharsetEncoder utf8CharsetEncoder, @Nonnull ByteBuffer utf8ByteBuffer,
            @Nullable ClientLineEnding clientLineEnding)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = MD5Digester.class.getDeclaredMethod(
                "findAndReplaceEncodedClientLineEndingIfRequireLineEndingCovert",
                InputStreamReader.class, CharsetEncoder.class, ByteBuffer.class,
                ClientLineEnding.class);
        method.setAccessible(true);

        return (ByteBuffer) method.invoke(md5Digester, encodedStreamReader, utf8CharsetEncoder,
                utf8ByteBuffer, clientLineEnding);
    }

}
