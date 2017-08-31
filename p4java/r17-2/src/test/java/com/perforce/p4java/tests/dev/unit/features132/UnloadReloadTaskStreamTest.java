package com.perforce.p4java.tests.dev.unit.features132;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.option.server.GetStreamsOptions;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.option.server.StreamOptions;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test unload and reload task stream
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job066055"})
@TestId("Dev132_UnloadReloadTaskStreamTest")
public class UnloadReloadTaskStreamTest extends P4JavaTestCase {

  private IOptionsServer server = null;
  private IOptionsServer superServer = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);

    superServer = getServerAsSuper();
    assertThat(superServer, notNullValue());
    IClient superClient = getDefaultClient(superServer);
    assertThat(superClient, notNullValue());
    superServer.setCurrentClient(superClient);
  }

  @AfterEach
  public void tearDown() {
    afterEach(server, superServer);
  }

  /**
   * Test unload and reload task stream
   */
  @Test
  public void testUnloadReloadTaskStreams() throws Exception {
    int randNum = getRandomInt();
    String streamName = "testmain" + randNum;
    String newStreamPath = "//p4java_stream/" + streamName;

    String streamName2 = "testtask" + randNum;
    String newStreamPath2 = "//p4java_stream/" + streamName2;

    try {
      // Create a stream
      IStream newStream = Stream.newStream(server, newStreamPath,
          "mainline", null, null, null, null, null, null, null);

      String retVal = server.createStream(newStream);

      // The stream should be created
      assertThat(retVal, notNullValue());
      assertThat(retVal, is("Stream " + newStreamPath + " saved."));

      // Get the newly created stream
      IStream returnedStream = server.getStream(newStreamPath);
      assertThat(returnedStream, notNullValue());

      // Validate the content of the stream
      assertThat(returnedStream.getStream(), is(newStreamPath));
      assertThat(returnedStream.getParent(), is("none"));
      assertThat(returnedStream.getType().toString().toLowerCase(Locale.ENGLISH), is("mainline"));
      assertThat(returnedStream.getOptions().toString(), is("allsubmit unlocked toparent fromparent"));
      assertThat(returnedStream.getName(), is(streamName));
      assertThat(returnedStream.getDescription(), containsString(Stream.DEFAULT_DESCRIPTION));
      assertThat(returnedStream.getStreamView().getSize(), is(1));
      assertThat(returnedStream.getRemappedView().getSize(), is(0));
      assertThat(returnedStream.getIgnoredView().getSize(), is(0));

      String options = "locked ownersubmit notoparent nofromparent";
      String[] viewPaths = new String[]{
          "share ...",
          "share core/GetOpenedFilesTest/src/gnu/getopt/...",
          "isolate readonly/sync/p4cmd/*",
          "import core/GetOpenedFilesTest/bin/gnu/... //p4java_stream/main/core/GetOpenedFilesTest/bin/gnu/...",
          "exclude core/GetOpenedFilesTest/src/com/perforce/p4cmd/..."};
      String[] remappedPaths = new String[]{
          "core/GetOpenedFilesTest/... core/GetOpenedFilesTest/src/...",
          "core/GetOpenedFilesTest/src/... core/GetOpenedFilesTest/src/gnu/..."};
      String[] ignoredPaths = new String[]{"/temp", "/temp/...",
          ".tmp", ".class"};

      IStream newStream2 = Stream.newStream(server, newStreamPath2,
          "task", newStreamPath, "Task stream",
          "The task stream of " + newStreamPath, options,
          viewPaths, remappedPaths, ignoredPaths);

      retVal = server.createStream(newStream2);

      // The stream should be created
      assertThat(retVal, notNullValue());
      assertThat(retVal, is("Stream " + newStreamPath2 + " saved."));

      // Get the newly created stream
      returnedStream = server.getStream(newStreamPath2);
      assertThat(returnedStream, notNullValue());

      // Validate the content of the stream
      assertThat(returnedStream.getStream(), is(newStreamPath2));
      assertThat(returnedStream.getParent(), is(newStreamPath));
      assertThat(returnedStream.getType().toString().toLowerCase(Locale.ENGLISH), is("task"));
      assertThat(returnedStream.getOptions().toString(), is("ownersubmit locked notoparent nofromparent"));
      assertThat(returnedStream.getName(), is("Task stream"));
      assertThat(returnedStream.getDescription(), containsString("The task stream of " + newStreamPath));
      assertThat(returnedStream.getStreamView().getSize(), is(5));
      assertThat(returnedStream.getRemappedView().getSize(), is(2));
      assertThat(returnedStream.getIgnoredView().getSize(), is(4));

      // Use stream update() and refresh() methods
      returnedStream.setDescription("New updated description.");
      returnedStream.update();
      returnedStream.refresh();
      assertThat(returnedStream.getDescription(), containsString("New updated description."));

      // Get all the streams
      List<IStreamSummary> streams = server.getStreams(
          null,
          new GetStreamsOptions());
      assertThat(streams, notNullValue());

      // Get only the two new streams
      streams = server.getStreams(
          newArrayList(Arrays.asList(newStreamPath, newStreamPath2)),
          new GetStreamsOptions());
      assertThat(streams, notNullValue());
      assertThat(streams.size(), is(2));

      // Unload task stream
      retVal = server.unload(new UnloadOptions().setStream(newStreamPath2).setLocked(true));
      assertThat(retVal, notNullValue());
      assertThat(retVal, containsString("Stream " + newStreamPath2 + " unloaded."));

      // Check temp task stream has been unloaded
      boolean found = false;
      List<IStreamSummary> unloadedStreams = server.getStreams(null, new GetStreamsOptions().setUnloaded(true));
      assertThat(unloadedStreams, notNullValue());
      for (IStreamSummary ss : unloadedStreams) {
        if (ss.getStream().equalsIgnoreCase(newStreamPath2)) {
          found = true;
          break;
        }
      }
      assertThat(found, is(true));

      // Reload task stream
      retVal = server.reload(new ReloadOptions().setStream(newStreamPath2));
      assertThat(retVal, containsString("Stream " + newStreamPath2 + " reloaded."));

    } finally {
      if (nonNull(superServer)) {
        try {
          String serverMessage = superServer.deleteStream(
              newStreamPath2,
              new StreamOptions().setForceUpdate(true));
          assertThat(serverMessage, notNullValue());
          serverMessage = superServer.deleteStream(
              newStreamPath,
              new StreamOptions().setForceUpdate(true));
          assertThat(serverMessage, notNullValue());
        } catch (Exception ignore) {
          // Nothing much we can do here...
        }
      }
    }
  }
}
