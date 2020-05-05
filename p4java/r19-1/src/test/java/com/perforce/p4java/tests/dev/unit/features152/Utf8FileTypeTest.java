package com.perforce.p4java.tests.dev.unit.features152;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

import static org.junit.Assert.*;

@TestId("Dev152_Utf8FileTypeTestTest")
public class Utf8FileTypeTest extends P4JavaRshTestCase {

    @ClassRule
    public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", SpecDepotTest.class.getSimpleName());

    IClient client = null;
    String clientroot;

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        try {
            setupServer(p4d.getRSHURL(), userName, password, true, null);
            client = getClient(server);
            clientroot = client.getRoot();
            File testFile = new File(clientroot + "/utf8");
            testFile.mkdirs();

            // Write a plain ascii file (UTF8 compliant)
            testFile = new File(clientroot + "/utf8/test1.txt");
            testFile.delete();
            testFile.createNewFile();

            // Writes bytes "t", "e", "s", "t" to the file
            FileOutputStream stream = new FileOutputStream(testFile);
            stream.write(new byte[] { 0x74, 0x65, 0x73, 0x74});
            stream.close();

            // Write a plain ascii file (UTF8 BOM)
            testFile = new File(clientroot + "/utf8/test2.txt");
            testFile.delete();
            testFile.createNewFile();

            // Writes bytes BOM + "t", "e", "s", "t" to the file
            stream = new FileOutputStream(testFile);
            stream.write(new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf, 0x74, 0x65, 0x73, 0x74});
            stream.close();

            // Write a high-ascii file
            testFile = new File(clientroot + "/utf8/test3.txt");
            testFile.delete();
            testFile.createNewFile();

            stream = new FileOutputStream(testFile);
            stream.write(new byte[] { 0x74, 0x65, 0x73, 0x74, (byte) 250 });
            stream.close();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testUtf8Files() throws Exception {
        server.setWorkingDirectory(new File(clientroot).getAbsolutePath());
        server.execMapCmd("revert", new String[] { "-k", new File( clientroot + "/utf8").getAbsolutePath() + "/..." }, null);
        Map<String, Object>[] outmap = null;

        outmap = server.execMapCmd("add", new String[] { new File( clientroot + "/utf8/test1.txt").getAbsolutePath() }, null);
        assertTrue(outmap.length > 0);
        assertEquals("text", (String) outmap[0].get("type") );

        outmap = server.execMapCmd("add", new String[] { new File( clientroot + "/utf8/test2.txt").getAbsolutePath() }, null);
        assertTrue(outmap.length > 0);
        assertEquals("utf8", (String) outmap[0].get("type") );

        outmap = server.execMapCmd("add", new String[] { new File( clientroot + "/utf8/test3.txt").getAbsolutePath() }, null);
        assertTrue(outmap.length > 0);
        assertEquals("text", (String) outmap[0].get("type") );
    }
    
}
