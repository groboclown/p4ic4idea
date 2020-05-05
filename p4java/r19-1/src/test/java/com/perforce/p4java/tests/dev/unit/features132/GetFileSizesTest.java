package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test getting information about the size of the files in the depot.
 */

@Jobs({"job067365"})
@TestId("Dev132_GetFileSizesTest")
public class GetFileSizesTest extends P4JavaRshTestCase {

  @ClassRule
  public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", GetFileSizesTest.class.getSimpleName());

  IOptionsServer superServer;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, null);
    superServer = getSuperConnection(p4d.getRSHURL());
  }

  @After
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
