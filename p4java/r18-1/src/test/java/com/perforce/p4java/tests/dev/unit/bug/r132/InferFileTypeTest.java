package com.perforce.p4java.tests.dev.unit.bug.r132;

import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.FST_BINARY;
import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.FST_XBINARY;
import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.FST_CBINARY;
import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.FST_UNICODE;
import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.FST_XUNICODE;
import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.inferFileType;
import static com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.isKnownCBinary;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.junit.*;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the inferFileType() method.
 */
@Jobs({ "job066260" })
@TestId("Dev131_InferFileTypeTest")
public class InferFileTypeTest extends P4JavaRshTestCase {
    // Unicode server
    private static final String serverURL = "p4java://localhost:20132";
    private static final String p4Charset = "utf8";
   
    
    @ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", InferFileTypeTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }

    public static int intToHex(int n) {
        return Integer.valueOf(String.valueOf(n), 16);
    }

    public int byteToInt(byte b) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[] { b });
        return bb.getShort(); // Implicitly widened to an int per JVM spec.
    }

    /**
     * Test printout the JVM's supported charsets.
     */
    @Test
    public void testPrintoutSupportedCharsets() {
        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
        debugPrint("------------- availableCharsets ----------------");
        for (Map.Entry<String, Charset> entry : charsetMap.entrySet()) {
            String canonicalCharsetName = entry.getKey();
            debugPrint(canonicalCharsetName);
            Charset charset = entry.getValue();
            Set<String> aliases = charset.aliases();
            for (String alias : aliases) {
                debugPrint("\t" + alias);
            }
        }
        debugPrint("-----------------------------------------");
        String[] perforceCharsets = PerforceCharsets.getKnownCharsets();
        debugPrint("------------- perforceCharsets ----------------");
        for (String perforceCharset : perforceCharsets) {
            debugPrint(perforceCharset + " ... "
                    + PerforceCharsets.getJavaCharsetName(perforceCharset));
        }
        debugPrint("-----------------------------------------");
        debugPrint("-----------------------------------------");
        debugPrint("Charset.defaultCharset().name(): " + Charset.defaultCharset().name());
        debugPrint("-----------------------------------------");
    }

    /**
     * Assert expected conditions for a file type test..
     * @param fileName the file name
     * @param expectedFileType the expected file type
     * @param charset the charset
     */
    private void assertFileType(String fileName, RpcPerforceFileType expectedFileType,
                            Charset charset, IClient client) {
        Path path = Paths.get(client.getRoot(), "FileTypeTest", fileName);
        File file = new File(path.toString());
        RpcPerforceFileType fileType = inferFileType(file, -1, true, charset);
        Assert.assertNotNull(fileType);
        Assert.assertEquals(expectedFileType, fileType);
    }

    /**
     * Test the inferFileType() method.
     */
    @Test
    @Ignore
    public void testInferFileType() throws Exception {
        String depotFilePath = "//depot/FileTypeTest/...";
        byte[] bytes = new byte[RpcPropertyDefs.RPC_DEFAULT_FILETYPE_PEEK_SIZE];
        List<Map<String, Object>> result = server.execMapCmdList(CmdSpec.SYNC.toString(),
                new String[] { "-f", depotFilePath }, null);
        Assert.assertNotNull(result);
        IClient client = server.getCurrentClient();

        try (FileInputStream inStream = new FileInputStream(
                new File(Paths.get(client.getRoot(), "FileTypeTest", "Linux.jpg").toString()))) {
            int bytesRead;
            failIfConditionFails((bytesRead = inStream.read(bytes)) > 0,
                    "can't read bytes or no bytes read!");

            // Check for cbinary
            boolean cbin = isKnownCBinary(bytes, bytesRead);
            Assert.assertEquals(true ,cbin);
        }
        // Sync the utf16 and utf32 files
        List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(depotFilePath),
                new SyncOptions().setForceUpdate(true));
        Assert.assertNotNull(files);
        Assert.assertEquals(true, files.size() > 0);

        byte[] hiascii = "0000034002-utf32_カパタ.txt".getBytes(UTF_8);
        String utf32fileName = new String(hiascii, Charset.defaultCharset());
        
        assertFileType("0000034002-test_utf16.txt", FST_XUNICODE, Charset.forName("UTF-16BE"), client);
        assertFileType("0000034002-test_utf16.txt", FST_XBINARY, Charset.forName("UTF-16LE"), client);
        assertFileType("0000034002-test_utf16.txt", FST_XBINARY, UTF_8, client);
        assertFileType(utf32fileName, FST_XUNICODE, Charset.forName("UTF-32"), client);
        assertFileType("hmelo.pdf", FST_XBINARY, UTF_8, client);
        assertFileType("Linux.jpg", FST_CBINARY, UTF_8, client);
        assertFileType("file.docx", FST_CBINARY, Charset.forName("x-MacRoman"), client);
        assertFileType("NOAA-CPC-drought-monitor-sample.gif", FST_CBINARY, UTF_8, client);
        assertFileType("eclipse-setup.txt.gz", FST_CBINARY, UTF_8, client);
        assertFileType("UnlimitedJCEPolicyJDK7.zip", FST_CBINARY, UTF_8, client);
        assertFileType("FixListOptionsTest.class", FST_CBINARY, UTF_8, client);
        assertFileType("US_export_policy.jar", FST_CBINARY, UTF_8, client);
    }
}
