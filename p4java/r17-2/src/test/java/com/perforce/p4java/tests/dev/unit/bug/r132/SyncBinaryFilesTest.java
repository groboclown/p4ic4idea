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
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test sync binary files (PNG format) with have-list.
 */

@Jobs({"job068751"})
@TestId("Dev132_SyncBinaryFilesTest")
public class SyncBinaryFilesTest extends P4JavaRshTestCase {
  
  private IClient client = null;
  
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncBinaryFilesTest.class.getSimpleName());

  @BeforeClass
  public static void beforeAll() throws Exception {
	Properties serverProps = new Properties();
	serverProps.put("socketPoolSize", "1");
  	setupServer(p4d.getRSHURL(), null, null, true, serverProps);
  }


  @Before
  public void setUp() throws Exception {
    
    String clientName = SystemInfo.isWindows() ? "p4TestUserWS20112Windows" : "p4TestUserWS20112";
    client = server.getClient(clientName);
    Assert.assertNotNull(client);
    server.setCurrentClient(client);
  }


  /**
   * Test force sync binary file.
   */
  @Test
  public void testForceSyncBinaryFile() throws Exception {
    //String depotFile = "//depot/cases/45580/binary-coverity-analysis-commons.tgz";
    //String depotFile = "//depot/cases/45580/ubinary-coverity-analysis-commons.tgz";
    String depotFile = "//depot/cases/45580/binaryfile2.png";
    //String depotFile = "//depot/cases/45580/ubinaryfile2.png";

    List<IFileSpec> syncFiles = client.sync(
        FileSpecBuilder.makeFileSpecList(depotFile + "#none"),
        new SyncOptions().setForceUpdate(false));
    Assert.assertNotNull(syncFiles);

    syncFiles = client.sync(
        FileSpecBuilder.makeFileSpecList(depotFile + "#head"),
        new SyncOptions().setForceUpdate(false));
    Assert.assertNotNull(syncFiles);

    syncFiles = client.sync(
        FileSpecBuilder.makeFileSpecList(depotFile + "#head"),
        new SyncOptions().setForceUpdate(true));
    Assert.assertNotNull(syncFiles);
  }
}
