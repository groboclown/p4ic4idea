package com.perforce.p4java.tests.dev.unit.bug.r131;

import static java.util.Objects.nonNull;
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
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the "replaced" action.
 *
 * <pre>
 * $ p4 files //app/portal/...
 * //app/portal/dev/java7/file1.txt#2 - integrate change 1199 (text)
 * //app/portal/main/file1.txt#3 - edit change 1202 (text)
 * $ p4 have ...
 * //app/portal/main/file1.txt#3 - /Users/jkovisto/perforce/workspaces/20111_ws/app/file1.txt
 * # Update view
 * 	//app/portal/dev/java7/... //20111_ws/app/...
 * $ p4 sync ...
 * //app/portal/dev/java7/file1.txt#2 - replacing /Users/jkovisto/perforce/workspaces/20111_ws/app/file1.txt
 * </pre>
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job060376"})
@TestId("Dev131_FileActionReplacedTest")
public class FileActionReplacedTest extends P4JavaTestCase {

  private IOptionsServer server = null;


  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }

  @AfterEach
  public void tearDown() {
    if (nonNull(server)) {
      endServerSession(server);
    }
  }

  /**
   * Test the 'replaced' action.
   */
  @Test
  public void testReplacedAction() throws Exception {

    String clientName = "Job060376Client";
    String filePath = "//Job060376Client/112Dev/testreplacing1/testfile1.txt";

    IClient testClient = server.getClient(clientName);
    server.setCurrentClient(testClient);

    // Sync the file
    List<IFileSpec> files = testClient.sync(FileSpecBuilder.makeFileSpecList(filePath), new SyncOptions().setForceUpdate(true));
    assertThat(files, notNullValue());
    assertThat(files.size() > 0, is(true));

    ClientView clientView = new ClientView();

    // Update the file mapping
    int count = 0;
    for (IClientViewMapping iClientViewMapping : testClient.getClientView()) {
      String mapping;
      String depotSpec = iClientViewMapping.getDepotSpec();
      String client = iClientViewMapping.getClient();
      if (depotSpec.contentEquals("//depot/112Dev/testreplacing2/testfile1.txt")) {
        mapping = "//depot/112Dev/testreplacing1/testfile1.txt" + " " + client;
      } else if (depotSpec.contentEquals("//depot/112Dev/testreplacing1/testfile1.txt")) {
        mapping = "//depot/112Dev/testreplacing2/testfile1.txt" + " " + client;
      } else {
        mapping = iClientViewMapping.getDepotSpec(true) + " " + iClientViewMapping.getClient(true);
      }
      clientView.addEntry(new ClientViewMapping(count++, mapping));
    }

    testClient.setClientView(clientView);
    testClient.update();
    testClient = server.getClient(clientName);
    server.setCurrentClient(testClient);

    // Sync again, should get the "replaced" action
    files = testClient.sync(FileSpecBuilder.makeFileSpecList(filePath), null);
    assertThat(files, notNullValue());
    assertThat(files.size() > 0, is(true));
    assertThat(files.get(0).getAction(), is(FileAction.REPLACED));
  }
}
