/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test sync using IStreamingCallback.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job059658"})
@TestId("Dev123_CancelStreamingCallbackTest")
public class CancelStreamingCallbackTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IClient client = null;

  @BeforeEach
  public void setUp() throws Exception {
    Properties props = new Properties();

    props.put("enableProgress", "true");

    server = ServerFactory.getOptionsServer("p4jrpcnts://eng-p4java-vm.perforce.com:20121", props);
    assertThat(server, notNullValue());

    // Register callback
    server.registerCallback(createCommandCallback());
    // Connect to the server.
    server.connect();
    setUtf8CharsetIfServerSupportUnicode(server);

    // Set the server user
    server.setUserName(getUserName());

    // Login using the normal method
    server.login(getPassword(), new LoginOptions());

    client = getDefaultClient(server);
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
   * Test cancel sync files using IStreamingCallback
   */
  @Test
  public void testCancelSynFiles() throws Exception {
    String depotFile = "//depot/112Dev/GetOpenedFilesTest/bin/com/perforce/p4cmd/...";

    List<Map<String, Object>> resultsList = newArrayList();
    int key = getRandomInt();
    ListCallbackHandler handler = new ListCallbackHandler(
        this,
        key,
        resultsList, 3);

    SyncOptions syncOptions = new SyncOptions()
        .setForceUpdate(true)
        .setQuiet(true);
    client.sync(
        FileSpecBuilder.makeFileSpecList(depotFile),
        syncOptions,
        handler,
        key);

    assertThat(resultsList, notNullValue());
    assertThat(resultsList.size() > 0, is(true));

    List<IFileSpec> fileList = newArrayList();
    populateFileSpecs(resultsList, fileList);
    assertThat(fileList, notNullValue());
    assertThat(fileList.size() > 0, is(true));

    // Second sync using the same RPC connection (NTS)
    resultsList = newArrayList();
    key = getRandomInt();
    handler = new ListCallbackHandler(
        this,
        key,
        resultsList,
        1000);

    client.sync(
        FileSpecBuilder.makeFileSpecList(depotFile),
        syncOptions,
        handler,
        key);

    assertThat(resultsList, notNullValue());
    assertThat(resultsList.size() > 0, is(true));

    fileList = newArrayList();
    populateFileSpecs(resultsList, fileList);

    assertThat(fileList, notNullValue());
    assertThat(fileList.size() > 0, is(true));
  }

  private void populateFileSpecs(
      final List<Map<String, Object>> resultsList,
      final List<IFileSpec> fileList) throws AccessException, ConnectionException {

    for (Map<String, Object> resultMap : resultsList) {
      if (nonNull(resultMap)) {
        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
          String k = entry.getKey();
          Object v = entry.getValue();
          System.out.println(k + "=" + v);
        }

        fileList.add(ResultListBuilder.handleFileReturn(resultMap, server));
      }
    }
  }

  private static class ListCallbackHandler implements IStreamingCallback {
    int expectedKey = 0;
    final CancelStreamingCallbackTest testCase;
    final List<Map<String, Object>> resultsList;

    int limit = 3;

    int count = 0;

    public ListCallbackHandler(
        final CancelStreamingCallbackTest testCase,
        final int key,
        final List<Map<String, Object>> resultsList,
        final int limit) {

      this.expectedKey = key;
      this.testCase = testCase;
      this.resultsList = resultsList;
      this.limit = limit;
    }

    public boolean startResults(final int key) throws P4JavaException {
      failIfKeyNotEqualsExpected(key, expectedKey);
      return true;
    }

    public boolean endResults(final int key) throws P4JavaException {
      failIfKeyNotEqualsExpected(key, expectedKey);
      return true;
    }

    public boolean handleResult(
        final Map<String, Object> resultMap,
        final int key) throws P4JavaException {

      count++;
      failIfConditionFails(count <= limit, "this callback method should not have been called after reaching the limit");
      failIfKeyNotEqualsExpected(key, expectedKey);
      failIfConditionFails(nonNull(resultMap), "null resultMap passed to handleResult callback");

      resultsList.add(resultMap);
      return count != limit;
    }

    public List<Map<String, Object>> getResultsList() {
      return resultsList;
    }
  }
}
