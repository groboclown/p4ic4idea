/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test get labels with case-insensitive name filter.
 */
@Jobs({ "job046825" })
@TestId("Dev112_GetLabelsTest")
public class GetLabelsTest extends P4JavaRshTestCase {

	IOptionsServer superServer = null;
	IClient client = null;
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetLabelsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
			server.setCurrentClient(client);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test get labels with case-insensitive name filter.
	 */
	@Test
	public void testGetLabels() {
		int randNum = getRandomInt();
		String labelName = "Test-Label-job046825-" + randNum;

		try {
			// Create a new label with a mixed of lower and upper case letters
			ILabel label = Label.newLabel(server, labelName, "a new label "
					+ labelName,
					new String[] { "//depot/112Dev/Attributes/..." });
			String message = server.createLabel(label);
			assertNotNull(message);
			assertEquals("Label " + labelName + " saved.", message);

			// Setting a default case-sensitive name filter with lower case
			List<ILabelSummary> labels = server.getLabels(null,
					new GetLabelsOptions()
							.setNameFilter("test-label-job046825-*"));
			assertNotNull(labels);

			// Should get an empty list, since the filter is case sensitive
			assertEquals(0, labels.size());

			// Setting a name filter with lower case
			labels = server.getLabels(null, new GetLabelsOptions()
					.setCaseInsensitiveNameFilter("test-label-job046825-*"));
			assertNotNull(labels);

			// Should get one in the list, since the filter is case sensitive
			assertEquals(1, labels.size());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				// Delete the test label
				superServer = getSuperConnection(p4d.getRSHURL());
				if (superServer != null) {
					String message = superServer.deleteLabel(labelName, true);
					assertNotNull(message);
				}
			} catch (Exception e) {
				// Can't do much here...
			} 
		}
	}
}
