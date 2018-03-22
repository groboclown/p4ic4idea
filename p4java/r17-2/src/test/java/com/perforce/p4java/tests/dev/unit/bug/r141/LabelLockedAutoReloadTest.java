/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r152.ShelveChangelistClientTest;

/**
 * Test the label "locked/unlocked" statuses.
 */
@Jobs({ "job074971" })
@TestId("Dev141_LabelLockedAutoReloadTest")
public class LabelLockedAutoReloadTest extends P4JavaRshTestCase {

	
	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LabelLockedAutoReloadTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
    	setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
    
    }


	/**
	 * Test the label locked and autoreload statuses.
	 */
	@Test
	public void testLabelLockedAutoReload() {
		int randNum = getRandomInt();
		String labelName = "Test-Label-job074971-" + randNum;

		try {
			// Create a new label and set the locked and autoreload statuses
			ILabel label = Label.newLabel(server, labelName, "a new label "
					+ labelName,
					new String[] { "//depot/112Dev/Attributes/..." });
			// Set label locked and autoreload to true
			label.setLocked(true);
			label.setAutoReload(true);
			String message = server.createLabel(label);
			assertNotNull(message);
			assertEquals("Label " + labelName + " saved.", message);

			// Using the 'p4 labels' command
			// Setting a name filter with lower case
			List<ILabelSummary> labels = server.getLabels(null, new GetLabelsOptions()
					.setCaseInsensitiveNameFilter("test-label-job074971-*"));
			assertNotNull(labels);

			// Should get one in the list, since the filter is case sensitive
			assertEquals(1, labels.size());
			
			// Label locked and autoreload should be true
			assertNotNull(labels.get(0));
			assertTrue(labels.get(0).isLocked());
			assertTrue(labels.get(0).isAutoReload());

			// Using the 'p4 label -o' command
			label = server.getLabel(labelName);
			assertNotNull(label);

			// Label locked and autoreload should be true
			assertTrue(label.isLocked());
			assertTrue(label.isAutoReload());
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				// Delete the test label
				server = getServerAsSuper();
				if (server != null) {
					String message = server.deleteLabel(labelName, true);
					assertNotNull(message);
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			} catch (URISyntaxException e) {
				// Can't do much here...
			}
		}
	}
}
