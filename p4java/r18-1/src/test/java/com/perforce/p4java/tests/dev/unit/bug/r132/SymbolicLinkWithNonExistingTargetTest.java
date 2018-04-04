package com.perforce.p4java.tests.dev.unit.bug.r132;



import java.io.File;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test add symlinks with non-existing targets.
 */

@Jobs({"job066811"})
@TestId("Dev132_SymbolicLinkWithNonExistingTargetTest")
public class SymbolicLinkWithNonExistingTargetTest extends P4JavaRshTestCase {

  
  private static IClient client = null;
  
  private static IClient superClient = null;
  
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SymbolicLinkWithNonExistingTargetTest.class.getSimpleName());

  @BeforeClass
  public static void beforeAll() throws Exception {
	Properties serverProps = new Properties();
	serverProps.put("socketPoolSize", "1");
  	setupServer(p4d.getRSHURL(), null, null, true, serverProps);
  }

  @Before
  public  void setUp() throws Exception {
    // Socket pool size
    
    client = server.getClient(SystemInfo.isWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112");
    Assert.assertNotNull(client);
    server.setCurrentClient(client);
    
    superClient = server.getClient(SystemInfo.isWindows() ? "p4TestSuperWindows" : "p4TestSuperWS20112");
    
  }

  
  /**
   * Test symlink support - sync command. This only works with JDK 7 or above
   * and non-Windows environment.
   */
  @Test
  public void testSyncSymlinks() throws Exception {
    String[] repoPaths = new String[]{
        "//depot/symlinks/testdira/testdir2/...",
        "//depot/symlinks/testdira/testdirb/testfile.txt",
        "//depot/symlinks/testdira/testdir2",
        "//depot/symlinks/testdir/testdir3/testdir2-non-existing-888"};

    List<IFileSpec> files = client.sync(
        FileSpecBuilder.makeFileSpecList(repoPaths),
        new SyncOptions().setForceUpdate(true));
    Assert.assertNotNull(files);
  }

  /**
   * Test add symlinks with non-existing targets.
   */
  @Test
  public void testAddSymlinks() throws Exception{
    IChangelist changelist = null;
    String clientRoot = client.getRoot();
    String path = null;
    String depotPath = null;
    int rand = getRandomInt();

    for (int i = 0; i < 3; i++) {
      try {
        // Check if symlink capable
        if (SymbolicLinkHelper.isSymbolicLinkCapable()) {

          String target = clientRoot + File.separator + "symlinks" + File.separator + "testdir" + File.separator + "testdir2-non-existing";
          String link = clientRoot + File.separator + "symlinks" + File.separator + "testdir" + File.separator + "testdir3" + File.separator + "testdir2-symlink-" + rand;
          depotPath = "//depot/symlinks/testdir/testdir3/testdir2-symlink-" + rand;

          // Create symbolic link
          path = SymbolicLinkHelper.createSymbolicLink(link, target);

          boolean isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
          Assert.assertNotNull(isSymlink);

          changelist = getNewChangelist(server, client, "Dev123_SymbolicLinkSyncTest add symbolic link.");
          Assert.assertNotNull(changelist);
          changelist = client.createChangelist(changelist);
          Assert.assertNotNull(changelist);

          AddFilesOptions afo = new AddFilesOptions();
          afo.setChangelistId(changelist.getId());
          afo.setFileType("symlink");
          // Add the symbolic link
          List<IFileSpec> files = client.addFiles(
              FileSpecBuilder.makeFileSpecList(link),
              afo);
          Assert.assertNotNull(files);

          changelist.refresh();
          files = changelist.submit(new SubmitOptions());
          Assert.assertNotNull(files);

          // Delete the local symbolic link
          File delFile = new File(path);
          boolean delSuccess = delFile.delete();
          Assert.assertEquals(true, delSuccess);

          // Force sync of the submitted symbolic link
          files = client.sync(
              FileSpecBuilder.makeFileSpecList(path),
              new SyncOptions().setForceUpdate(true));
          Assert.assertNotNull(files);

          // Read the target path of the symbolic link
          // Verify the symbolic link has the correct target path
          String linkTarget = SymbolicLinkHelper.readSymbolicLink(path);
          Assert.assertNotNull(linkTarget);
          Assert.assertEquals(target,  linkTarget);
        }
      } finally {
        if ((client != null)
            && (changelist != null)
            && (changelist.getStatus() == ChangelistStatus.PENDING)) {

          try {
            // Revert files in pending changelist
            client.revertFiles(
                changelist.getFiles(true),
                new RevertFilesOptions().setChangelistId(changelist.getId()));
          } catch (P4JavaException e) {
            // Can't do much here...
          }
        }
        if ((path) != null) {
          if ((client != null) && (server != null)) {
            try {
              // Delete submitted test files
              IChangelist deleteChangelist = getNewChangelist(
                  server,
                  client,
                  "Dev132_SymbolicLinkWithNonExistingTargetTest delete submitted files");
              deleteChangelist = client.createChangelist(deleteChangelist);
              client.deleteFiles(
                  FileSpecBuilder.makeFileSpecList(path),
                  new DeleteFilesOptions().setChangelistId(deleteChangelist.getId()));
              deleteChangelist.refresh();
              deleteChangelist.submit(null);
            } catch (P4JavaException e) {
              // Can't do much here...
            }
          }

          if ((superClient != null) && (server != null)) {
            try {
              List<IObliterateResult> obliterateFiles = server.obliterateFiles(
                      FileSpecBuilder.makeFileSpecList(depotPath),
                      new ObliterateFilesOptions().setExecuteObliterate(true));
              Assert.assertNotNull(obliterateFiles);
            } catch (P4JavaException e) {
              // Can't do much here...
            }
          }
          File file = new File(path);
          file.delete();
        }
      }
    }
  }
}
