package com.perforce.p4java.tests.dev.unit.bug.r132;



import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf8FileTypeTest;



/**
 * Test diff2 of unicode file revisions using a stream cmd method
 */

@Jobs({"job066294"})
@TestId("Dev132_StreamCmdDiff2UnicodeTest")
public class StreamCmdDiff2UnicodeTest extends P4JavaRshTestCase {
  // Unicode server
  private static final String serverURL = "p4java://localhost:30132";
  private static final String p4Charset = "utf16";
  
  
  @ClassRule
  public static SimpleServerRule p4d = new UnicodeServerRule("r16.1", StreamCmdDiff2UnicodeTest.class.getSimpleName());

  @BeforeClass
  public static void beforeAll() throws Exception {
  	setupServer(p4d.getRSHURL(), null, null, true, null);
  }

  

  @After
  public void tearDown() {
    afterEach(server);
  }

  /**
   * Test diff2 of unicode file revisions using a stream cmd method
   */
  @Test
  public void testStreamCmdDiff2Unicode() throws Exception {
    // diff2 of utf16 file revisions
	IClient client = server.getClient(this.defaultTestClientName);//p4TestUserWS
	Assert.assertNotNull(client);
	server.setCurrentClient(client);
    try (InputStream is = server.execQuietStreamCmd(
        CmdSpec.DIFF2.toString(),
        new String[]{"//depot/FileTypeTest/0000034002-汉字-漢字-ü-test_utf16.txt#1", "//depot/FileTypeTest/0000034002-汉字-漢字-ü-test_utf16.txt#2"})) {

      Assert.assertNotNull(is);
      String line;
      BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(PerforceCharsets.getJavaCharsetName(p4Charset))));
      while ((line = br.readLine()) != null) {
        System.out.println(line);
      }
    }
  }
}
