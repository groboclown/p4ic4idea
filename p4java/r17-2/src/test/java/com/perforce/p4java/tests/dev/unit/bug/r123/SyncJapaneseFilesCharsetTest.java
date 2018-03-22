package com.perforce.p4java.tests.dev.unit.bug.r123;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.PerforceCharsets;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test sync Japanese files
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job036721"})
@TestId("Dev112_SyncJapaneseFilesTest")
public class SyncJapaneseFilesCharsetTest extends P4JavaTestCase {

  private final static String playUnicodeServerURL = "p4java://qaplay.perforce.com:8838";
  private IOptionsServer server = null;
  private IClient client = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = ServerFactory.getOptionsServer(playUnicodeServerURL, null);
    assertThat(server, notNullValue());

    // Register callback
    server.registerCallback(createCommandCallback());
    server.connect();
    if (server.isConnected()) {
      if (server.supportsUnicode()) {
        // server.setCharsetName("utf8");
        // server.setCharsetName("windows-932");
        server.setCharsetName("shiftjis");
      }
    }
    server.setUserName("p4jtestsuper");
    server.login(null);
    client = server.getClient("p4jtestsuper_mac");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }


  @AfterEach
  public void tearDown() {
    if (nonNull(server)) {
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

    String[] depotPath = new String[]{"//depot/viv/test/はあふtest/...",
        "//depot/toyo_problem/...", "//depot/toyo1/..."};
    // Sync Japanese files from depot
    List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(depotPath),
        new SyncOptions().setForceUpdate(true));
    assertThat(files, notNullValue());
    assertThat(files.size() > 0, is(true));
  }
}
