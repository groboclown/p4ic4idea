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
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the annotate -q and -t options.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job059624"})
@TestId("Dev132_GetFileAnnotationsTest")
public class GetFileAnnotationsTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IClient client = null;
  private IOptionsServer superServer = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);

    superServer = getServerAsSuper();
    assertThat(superServer, notNullValue());
    IClient superClient = getDefaultClient(superServer);
    assertThat(superClient, notNullValue());
    superServer.setCurrentClient(superClient);
  }

  @AfterEach
  public void tearDown() {
    afterEach(server, superServer);
  }

  /**
   * Test the annotate -q and -t options.
   */
  @Test
  public void testGetFileAnnotations() throws Exception {
    final String testFile = "//depot/112Dev/Attributes-integ3/test-reconcile/reconcile-test01.txt";
    final String binaryFile = "//depot/112Dev/apple/icon/favicon.ico";
    List<IFileSpec> syncFiles = this.forceSyncFiles(client, testFile);
    assertThat("null sync file list returned", syncFiles, notNullValue());
    assertThat(syncFiles.size() > 0, is(true));
    syncFiles = this.forceSyncFiles(client, binaryFile);
    assertThat("null sync file list returned", binaryFile, notNullValue());
    assertThat(syncFiles.size() > 0, is(true));

    List<IFileSpec> testFileSpecs = FileSpecBuilder.makeFileSpecList(testFile);
    List<IFileAnnotation> annotations = server.getFileAnnotations(testFileSpecs, new GetFileAnnotationsOptions());
    assertThat("null annotations list returned", annotations, notNullValue());
    assertThat(annotations.size() > 1, is(true));

    List<IFileAnnotation> noheaderAnnotations = server.getFileAnnotations(testFileSpecs, new GetFileAnnotationsOptions().setSuppressHeader(true));
    assertThat("null annotations list returned", noheaderAnnotations, notNullValue());
    assertThat(noheaderAnnotations.size() > 0, is(true));

    List<IFileSpec> binaryFileSpecs = FileSpecBuilder.makeFileSpecList(binaryFile);
    List<IFileAnnotation> binaryAnnotations = server.getFileAnnotations(binaryFileSpecs, new GetFileAnnotationsOptions());
    assertThat("null annotations list returned", binaryAnnotations, notNullValue());
    assertThat(binaryAnnotations.size() > 0, is(true));

    List<IFileAnnotation> showBinaryAnnotations = server.getFileAnnotations(binaryFileSpecs, new GetFileAnnotationsOptions().setShowBinaryContent(true));
    assertThat("null annotations list returned", showBinaryAnnotations, notNullValue());
    assertThat(showBinaryAnnotations.size() > 0, is(true));
  }
}
