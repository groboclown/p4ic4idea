package com.perforce.p4java.tests.dev.unit.bug.r131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test 'p4 stream -o //Ace/DEV' command.
 */

@Jobs({"job059632"})
@TestId("Dev131_GetStreamBaseParentFieldTest")
public class GetStreamBaseParentFieldTest extends P4JavaRshTestCase {

  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetStreamBaseParentFieldTest.class.getSimpleName());

  String streamsDepotName = "p4java_stream";
  String streamDepth = "//" + streamsDepotName + "/1";
  String mainStreamPath = "//" + streamsDepotName + "/main";
  String childStreamPath = "//" + streamsDepotName + "/dev";

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, props);
    IClient client = server.getClient("p4TestUserWS");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
    createStreamsDepot(streamsDepotName, server, streamDepth);
    IStream mainStream = Stream.newStream(server, mainStreamPath,
            "mainline", null, null, null, null, null, null, null);
    server.createStream(mainStream);
    IStream childStream = Stream.newStream(server, childStreamPath,
            "development", mainStreamPath, null, null, null, null, null, null);
    childStream.setBaseParent(mainStreamPath);
    server.createStream(childStream);
  }

  @After
  public void tearDown() {
    if (server != null) {
      endServerSession(server);
    }
  }

  /**
   * Test 'p4 streams -o //p4java_stream/dev'.
   */
  @Test
  public void testGetStreamBaseParentField() throws Exception {
    IStreamSummary stream = server.getStream(childStreamPath, new GetStreamOptions());
    assertThat(stream, notNullValue());
    assertThat(stream.getBaseParent(), is(mainStreamPath));
  }
}
