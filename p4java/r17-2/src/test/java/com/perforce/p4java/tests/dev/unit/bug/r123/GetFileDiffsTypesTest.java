package com.perforce.p4java.tests.dev.unit.bug.r123;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test diff2 with two identical files differ only by filetype.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job057913"})
@TestId("Dev123_GetFileDiffsTypesTest")
public class GetFileDiffsTypesTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IClient client = null;
  private IChangelist changelist = null;

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
    // cleanup code (after each test).
    if (server != null) {
      this.endServerSession(server);
    }
  }

  /**
   * Test diff2 with two identical files differ only by filetype.
   */
  @Test
  public void testGetFileDiffsTypes() throws Exception {
    int randNum = getRandomInt();
    String depotFile = null;

    try {
      String path = "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/";
      String name = "P4JCommandCallbackImpl";
      String ext = ".java";
      String file = client.getRoot() + path + name + ext;
      String file2 = client.getRoot() + path + name + "-" + randNum + ext;
      depotFile = "//depot" + path + name + "-" + randNum + ext;

      List<IFileSpec> files = client.sync(
          FileSpecBuilder.makeFileSpecList(file),
          new SyncOptions().setForceUpdate(true));
      assertThat(files, notNullValue());

      // Copy a file to be used for add
      copyFile(file, file2);

      changelist = getNewChangelist(
          server,
          client,
          "Dev112_EditFilesTest add files");
      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());

      // Add the file with type "text"
      AddFilesOptions addFilesOptions = new AddFilesOptions()
          .setChangelistId(changelist.getId())
          .setFileType("text");

      files = client.addFiles(
          FileSpecBuilder.makeFileSpecList(file2),
          addFilesOptions);

      assertThat(files, notNullValue());
      changelist.refresh();
      files = changelist.submit(new SubmitOptions());
      assertThat(files, notNullValue());

      changelist = getNewChangelist(
          server,
          client,
          "Dev112_EditFilesTest edit files");

      assertThat(changelist, notNullValue());
      changelist = client.createChangelist(changelist);
      assertThat(changelist, notNullValue());

      // Edit the file with type "ktext"
      EditFilesOptions editFilesOptions = new EditFilesOptions()
          .setChangelistId(changelist.getId())
          .setFileType("ktext");
      files = client.editFiles(
          FileSpecBuilder.makeFileSpecList(depotFile),
          editFilesOptions);

      assertThat(files, notNullValue());
      changelist.refresh();
      files = changelist.submit(new SubmitOptions());
      assertThat(files, notNullValue());

      List<IFileDiff> diffsList = server.getFileDiffs(
          new FileSpec(depotFile),
          new FileSpec(depotFile + "#1"),
          null, new GetFileDiffsOptions());

      assertThat(diffsList, notNullValue());
      assertThat(diffsList.size() > 0, is(true));
      assertThat(diffsList.get(0), notNullValue());
      IFileDiff.Status status = diffsList.get(0).getStatus();
      assertThat(status, notNullValue());
      assertThat(status, is(IFileDiff.Status.TYPES));
    } finally {
      if (nonNull(client)) {
        if (nonNull(changelist) && (changelist.getStatus() == ChangelistStatus.PENDING)) {
          // Revert files in pending changelist
          client.revertFiles(
              changelist.getFiles(true),
              new RevertFilesOptions()
                  .setChangelistId(changelist.getId()));
        }
      }

      if (nonNull(client) && nonNull(server)) {
        if (isNotBlank(depotFile)) {
          // Delete submitted test files
          IChangelist deleteChangelist = getNewChangelist(
              server,
              client,
              "Dev112_EditFilesTest delete submitted files");
          deleteChangelist = client.createChangelist(deleteChangelist);
          client.deleteFiles(
              FileSpecBuilder.makeFileSpecList(depotFile),
              new DeleteFilesOptions()
                  .setChangelistId(deleteChangelist.getId()));
          deleteChangelist.refresh();
          deleteChangelist.submit(null);
        }
      }
    }
  }
}
