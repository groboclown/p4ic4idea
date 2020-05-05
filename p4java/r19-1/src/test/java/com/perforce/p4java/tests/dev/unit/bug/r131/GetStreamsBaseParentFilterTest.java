package com.perforce.p4java.tests.dev.unit.bug.r131;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 streams -F "baseParent=//Ace/MAIN"' command.
 */

@Jobs({"job059625"})
@TestId("Dev131_GetStreamsBaseParentFilterTest")
public class GetStreamsBaseParentFilterTest extends P4JavaTestCase {

  private IOptionsServer server = null;


  @Before
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = server.getClient("p4TestUserWS");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }

  @After
  public void tearDown() {
    if (server != null) {
      endServerSession(server);
    }
  }

  /**
   * Test 'p4 streams -F "baseParent=//p4java_stream/main"'.
   */
  @Test
  public void testGetStreamsBaseParentFilter() throws Exception {
    String streamFilter = "baseParent=//p4java_stream/main";

    GetStreamsOptions opts = new GetStreamsOptions();
    opts.setFilter(streamFilter);
    opts.setMaxResults(2);

    List<IStreamSummary> streams = server.getStreams(null, opts);
    assertThat(streams, notNullValue());
    assertThat(streams.size(), is(2));
  }
}
