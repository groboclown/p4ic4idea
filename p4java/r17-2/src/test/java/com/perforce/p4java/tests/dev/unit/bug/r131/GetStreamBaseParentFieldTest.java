package com.perforce.p4java.tests.dev.unit.bug.r131;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 stream -o //Ace/DEV' command.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job059632"})
@TestId("Dev131_GetStreamBaseParentFieldTest")
public class GetStreamBaseParentFieldTest extends P4JavaTestCase {

  private IOptionsServer server = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = server.getClient("p4TestUserWS");
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
   * Test 'p4 streams -o //p4java_stream/dev'.
   */
  @Test
  public void testGetStreamBaseParentField() throws Exception {
    String streamPath = "//p4java_stream/dev";

    IStreamSummary stream = server.getStream(streamPath, new GetStreamOptions());
    assertThat(stream, notNullValue());
    assertThat(stream.getBaseParent(), is("//p4java_stream/main"));
  }
}
