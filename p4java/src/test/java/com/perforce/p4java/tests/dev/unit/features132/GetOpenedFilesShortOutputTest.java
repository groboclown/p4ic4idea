package com.perforce.p4java.tests.dev.unit.features132;

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
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test get opened files with short output: 'p4 opened -s'
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job062832"})
@TestId("GetOpenedFilesTest")
public class GetOpenedFilesShortOutputTest extends P4JavaTestCase {

  private IOptionsServer server = null;
  private IClient client = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServerAsSuper();
    assertThat(server, notNullValue());
    client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }

  @AfterEach
  public void tearDown() {
    afterEach(server);
  }

  /**
   * Test get opened files with short output: 'p4 opened -s'
   */
  @Test
  public void testGetOpenedFilesShortOutput() throws Exception {
    List<IFileSpec> openedFiles = client.openedFiles(null, new OpenedFilesOptions().setMaxFiles(10));
    assertThat(openedFiles, notNullValue());
    assertThat(openedFiles.size() > 0, is(true));
    assertThat(openedFiles.get(0), notNullValue());
    assertThat(openedFiles.get(0).getEndRevision() > 0, is(true));

    List<IFileSpec> openedFilesShortOutput = client.openedFiles(null, new OpenedFilesOptions().setShortOutput(true).setMaxFiles(10));
    assertThat(openedFilesShortOutput, notNullValue());
    assertThat(openedFilesShortOutput.size() > 0, is(true));
    assertThat(openedFilesShortOutput.get(0), notNullValue());
    assertThat(openedFilesShortOutput.get(0).getEndRevision() > 0, is(false));
  }
}
