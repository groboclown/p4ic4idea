package com.perforce.p4java.tests.dev.unit.bug.r123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test update label using ILabel.updateOnServer().
 */

@Jobs({ "job049896" })
@TestId("Dev123_UpdateLabelTest")
public class UpdateLabelTest extends P4JavaRshTestCase {
    
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", UpdateLabelTest.class.getSimpleName());

    private IClient client = null;

    @Before
    public void setUp() throws Exception {
        setupServer(p4d.getRSHURL(), userName, password, true, props);
        client = getClient(server);
    }

    @After
    public void tearDown() {
        if (server != null) {
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
            ILabel label = Label.newLabel(server, labelName, "a new label " + labelName,
                    new String[] { "//depot/112Dev/Attributes/..." });

            @SuppressWarnings("deprecation")
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
            server = getSuperConnection(p4d.getRSHURL());
            if (server != null) {
                String message = server.deleteLabel(labelName, true);
                assertThat(message, notNullValue());
            }
        }
    }
}
