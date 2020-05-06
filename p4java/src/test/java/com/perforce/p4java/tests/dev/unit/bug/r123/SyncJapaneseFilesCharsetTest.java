package com.perforce.p4java.tests.dev.unit.bug.r123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.features123.InMemoryAuthTicketsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test sync Japanese files
 */

@Jobs({"job036721"})
@TestId("Dev112_SyncJapaneseFilesTest")
public class SyncJapaneseFilesCharsetTest extends P4JavaRshTestCase {

  @ClassRule
  public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", InMemoryAuthTicketsTest.class.getSimpleName());

  private IClient client = null;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), superUserName, superUserPassword, true, null);
    client = createClient(server, "p4jtestsuper_mac");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }


  @After
  public void tearDown() {
    if (server != null) {
      endServerSession(server);
    }
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
      debugPrint(perforceCharset + " ... " + PerforceCharsets.getJavaCharsetName(perforceCharset));
    }
    debugPrint("-----------------------------------------");

    debugPrint("-----------------------------------------");
    debugPrint("Charset.defaultCharset().name(): " + Charset.defaultCharset().name());
    debugPrint("-----------------------------------------");
  }

  /**
   * Test sync Japanese files
   */
  @Test
  public void testSyncJapaneseFiles() throws Exception {

    String[] depotPath = new String[]{"//depot/viv/test/\u306f\u3042\u3075test/...",
        "//depot/toyo_problem/...", "//depot/toyo1/..."};
    // Sync Japanese files from depot
    List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(depotPath),
        new SyncOptions().setForceUpdate(true));
    assertThat(files, notNullValue());
    assertThat(files.size() > 0, is(true));
  }
}
