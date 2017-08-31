package com.perforce.p4java.tests.dev.unit.bug.r132;


import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test sync text file revision with current file head as symlinks with
 * non-existing target.
 */

@Jobs({"job066811"})
@TestId("Dev132_SyncTextRevWithNoTargetSymlinkHeadTest")
public class SyncTextRevWithNoTargetSymlinkHeadTest extends P4JavaRshTestCase {
  
  private IClient client = null;
  
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncTextRevWithNoTargetSymlinkHeadTest.class.getSimpleName());

  @BeforeClass
  public static void beforeAll() throws Exception {
	Properties serverProps = new Properties();
	serverProps.put("socketPoolSize", "1");
  	setupServer(p4d.getRSHURL(), null, null, true, serverProps);
  }

  @Before
  public void setUp() throws Exception {
    
    client = server.getClient(SystemInfo.isWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112");
    Assert.assertNotNull(client);
    server.setCurrentClient(client);
    // Register callback
    server.registerCallback(createCommandCallback());
    IClient superClient = server.getClient(SystemInfo.isWindows() ? "p4TestSuperWindows" : "p4TestSuperWS20112");
  }

  /**
   * Test sync text file revision with current file head as symlinks with
   * non-existing target.
   */
  @Test
  public void testSyncTextRevWithNoTargetSymlinkHead() throws Exception {
    // Revision #1 is a text file
    String textRev = "//depot/symlinks/non-exsting-target/bar9.txt#1";
    // Revision #2 file type changed to a symlink with non-existing target
    String symlinkRev = "//depot/symlinks/non-exsting-target/bar9.txt#2";
    List<IFileSpec> files = client.sync(
        FileSpecBuilder.makeFileSpecList(symlinkRev),
        new SyncOptions().setForceUpdate(true));
    Assert.assertNotNull(files);

    files = client.sync(
        FileSpecBuilder.makeFileSpecList(textRev),
        new SyncOptions().setForceUpdate(true));
    Assert.assertNotNull(files);
  }
}
