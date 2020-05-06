package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test get opened files with short output: 'p4 opened -s'
 */

@Jobs({"job062832"})
@TestId("GetOpenedFilesTest")
public class GetOpenedFilesShortOutputTest extends P4JavaRshTestCase {

  @ClassRule
  public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", GetOpenedFilesShortOutputTest.class.getSimpleName());

  private List<IFileSpec> fileSpecs = null;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, null);
    client = getClient(server);
    fileSpecs = FileSpecBuilder.makeFileSpecList("//depot/test", "//depot/test2");
    client.addFiles(fileSpecs, null);
  }

  @After
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
