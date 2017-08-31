package com.perforce.p4java.tests.dev.unit.bug.r123;

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
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test update label using ILabel.updateOnServer().
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job049896"})
@TestId("Dev123_UpdateLabelTest")
public class UpdateLabelTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IClient client = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
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
   * Test update label using ILabel.updateOnServer().
   */
  @Test
  public void testUpdateLabel() throws Exception {
    int randNum = getRandomInt();
    String labelName = "Test-Label-" + testId + "-" + randNum;

    try {
      // Create a new label with a mixed of lower and upper case letters
      ILabel label = Label.newLabel(
          server,
          labelName,
          "a new label " + labelName,
          new String[]{"//depot/112Dev/Attributes/..."});

      String message = label.updateOnServer();
      assertThat(message, notNullValue());
      assertThat(message, is("Label " + labelName + " saved."));

      // Setting a name filter with lower case
      GetLabelsOptions getLabelsOptions = new GetLabelsOptions()
          .setCaseInsensitiveNameFilter("test-label-" + testId + "-*");
      List<ILabelSummary> labels = server.getLabels(null, getLabelsOptions);
      assertThat(labels, notNullValue());
      // Should get one in the list, since the filter is case sensitive
      assertThat(labels.size(), is(1));
    } finally {
      // Delete the test label
      server = getServerAsSuper();
      if (nonNull(server)) {
        String message = server.deleteLabel(labelName, true);
        assertThat(message, notNullValue());
      }
    }
  }
}
