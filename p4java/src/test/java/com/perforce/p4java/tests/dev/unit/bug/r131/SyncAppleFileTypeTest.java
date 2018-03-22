package com.perforce.p4java.tests.dev.unit.bug.r131;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

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
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test sync 'apple' type (AppleSingle) file. This apple file will be unpacked
 * into two files.
 * <pre>
 * 1. Data fork
 * 2. Resource fork
 * </pre>
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job061056"})
@TestId("Dev13.1_SyncAppleFileTypeTest")
public class SyncAppleFileTypeTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IClient client = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    client = getDefaultClient(server);
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
