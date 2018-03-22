package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test getting information about the size of the files in the depot.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job067365"})
@TestId("Dev132_GetFileSizesTest")
public class GetFileSizesTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IOptionsServer superServer = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = getDefaultClient(server);
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
   * Test getting information about the size of the files in the depot.
   */
  @Test
  public void testGetFileSizes() throws Exception {
    String depotFile = "//depot/client/ResolveFileStreamTest/...";

    List<IFileSize> fileSizes = server.getFileSizes(
        FileSpecBuilder.makeFileSpecList(depotFile),
        new GetFileSizesOptions()
            .setAllRevisions(true)
            .setMaxFiles(10));

    assertThat(fileSizes, notNullValue());
  }
}
