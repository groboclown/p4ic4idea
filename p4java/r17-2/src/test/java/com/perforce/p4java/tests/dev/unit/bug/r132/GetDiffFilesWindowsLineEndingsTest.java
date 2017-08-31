package com.perforce.p4java.tests.dev.unit.bug.r132;



import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.GetDiffFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf8FileTypeTest;




/**
 * Test IClient.getDiffFiles() using a Windows client (Line Endings: win) under
 * a Mac/Linux/UNIX platform.
 */

@Jobs({"job068974"})
@TestId("Dev132_GetDiffFilesTest")
public class GetDiffFilesWindowsLineEndingsTest extends P4JavaRshTestCase {
  
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetDiffFilesWindowsLineEndingsTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), null, null, true, null);
    }



  @After
  public void tearDown() {
    afterEach(server);
  }

  /**
   * Test IClient.getDiffFiles() using a Windows client (Line Endings: win)
   * under a Mac/Linux/UNIX platform.
   */
  @Test
  public void testGetDiffFiles() throws Exception {

    String depotFile = "//depot/cases/51381/diff-sa-test.xml";
    String nonExistingDepotFile = "//depot/cases/51381/diff-sa-test.xml-non-existing";

    IChangelist changelist = null;
    List<IFileSpec> files;
    try {
      IClient client = server.getClient("p4TestUserWSLineEndWin");
	  Assert.assertNotNull(client);
	  server.setCurrentClient(client);
      // Create a changelist
      changelist = getNewChangelist(server, client, "Dev132_SubmitShelvedChangelistTest copy files");
      Assert.assertNotNull(changelist);
      changelist = client.createChangelist(changelist);

      // Sync a file for edit
      files = client.sync(
          FileSpecBuilder.makeFileSpecList(depotFile),
          new SyncOptions().setForceUpdate(true));
      Assert.assertNotNull(files);

      // Open a file for edit
      files = client.editFiles(
          FileSpecBuilder.makeFileSpecList(depotFile),
          new EditFilesOptions().setChangelistId(changelist.getId()));
      Assert.assertNotNull(files);

      // Get diff files (diff -sa)
      files = client.getDiffFiles(
          FileSpecBuilder.makeFileSpecList(depotFile),
          new GetDiffFilesOptions().setOpenedDifferentMissing(true));
      Assert.assertNotNull(files);


      // Get diff files (diff -sa) of a non-existing file
      files = client.getDiffFiles(
          FileSpecBuilder.makeFileSpecList(nonExistingDepotFile),
          new GetDiffFilesOptions().setOpenedDifferentMissing(true));
      Assert.assertNotNull(files);
      Assert.assertEquals(false, files.isEmpty());
      IFileSpec actual = files.get(0);
      Assert.assertNotNull(actual);
      Assert.assertEquals(FileSpecOpStatus.ERROR, actual.getOpStatus());
    } finally {
      if (client != null) {
        if (changelist != null) {
          if (changelist.getStatus() == ChangelistStatus.PENDING) {
            try {
              // Revert files in pending changelist
              client.revertFiles(
                  changelist.getFiles(true),
                  new RevertFilesOptions().setChangelistId(changelist.getId()));
            } catch (P4JavaException e) {
              // Can't do much here...
            }
          }
        }
      }
    }
  }
}
