package com.perforce.p4java.tests.dev.unit.bug.r131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
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
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test problem with text file identified as a symlink during an add operation.
 */

@Jobs({"job061935"})
@TestId("Bug131_AddFilesCheckSymlinkTest")
public class AddFilesCheckSymlinkTest extends P4JavaRshTestCase {

  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", AddFilesCheckSymlinkTest.class.getSimpleName());

  private IOptionsServer superServer = null;
  private IClient client = null;
  private IChangelist changelist = null;

  @Before
  public void setUp() throws Exception {
    superServer = getSuperConnection(p4d.getRSHURL());
    IClient superClient = superServer.getClient("p4TestSuperWS20112");
    assertThat(superClient, notNullValue());
    superServer.setCurrentClient(superClient);

    setupServer(p4d.getRSHURL(), userName, password, true, props);
    client = server.getClient(SystemInfo.isWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }

  @After
  public void tearDown() {
    if (superServer != null) {
      endServerSession(superServer);
    }
    if (server != null) {
      endServerSession(server);
    }
  }

  /**
   * Test problem with text file identified as a symlink during an add operation.
   */
  @Test
  public void testAddFilesCheckSymlink() throws Exception {
    int randNum = getRandomInt();

    try {
      String path = "/112Dev/CopyFilesTest/src/test01";
      String ext = ".txt";
      String test = ".ü.";
      String file = client.getRoot() + path + ext;
      String file2 = client.getRoot() + path + test + randNum + ext;

      List<IFileSpec> files = client.sync(
          FileSpecBuilder.makeFileSpecList(file),
          new SyncOptions().setForceUpdate(true));
      assertThat(files, notNullValue());

      // Copy a file to be used for add
      copyFile(file, file2);

      changelist = getNewChangelist(server, client,
          "Bug131_AddFilesCheckSymlinkTest add files");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());

      // Add a file
      files = client.addFiles(
          FileSpecBuilder.makeFileSpecList(file2),
          new AddFilesOptions().setChangelistId(changelist.getId()));

      assertThat(files, notNullValue());
    } finally {
      if (client != null
          && changelist != null
          && (changelist.getStatus() == ChangelistStatus.PENDING)) {

        client.revertFiles(
            changelist.getFiles(true),
            new RevertFilesOptions().setChangelistId(changelist.getId()));
        // Delete changelist
        if (superServer != null) {
          server.deletePendingChangelist(changelist.getId());
        }
      }
    }
  }
}
