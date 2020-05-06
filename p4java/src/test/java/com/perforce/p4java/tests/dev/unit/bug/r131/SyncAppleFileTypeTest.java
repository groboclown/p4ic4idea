package com.perforce.p4java.tests.dev.unit.bug.r131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test sync 'apple' type (AppleSingle) file. This apple file will be unpacked
 * into two files.
 * <pre>
 * 1. Data fork
 * 2. Resource fork
 * </pre>
 */

@Jobs({"job061056"})
@TestId("Dev13.1_SyncAppleFileTypeTest")
public class SyncAppleFileTypeTest extends P4JavaRshTestCase {
  
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncAppleFileTypeTest.class.getSimpleName());

  private IClient client = null;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, props);
    assertThat(server, notNullValue());
    client = getDefaultClient(server);
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
   * Test sync apple file type.
   */
  @Test
  public void testSyncAppleFileType() throws Exception {
    String filePath = "//depot/112Dev/apple/icon/favicon.ico";

    // Sync the file
    List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(filePath), new SyncOptions().setForceUpdate(true));
    assertThat(files, notNullValue());
    assertThat(files.size() > 0, is(true));
  }
}
