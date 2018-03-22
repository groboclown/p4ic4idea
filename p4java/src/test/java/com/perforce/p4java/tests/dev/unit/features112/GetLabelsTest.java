/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test get labels with case-insensitive name filter.
 */
@Jobs({ "job046825" })
@TestId("Dev112_GetLabelsTest")
public class GetLabelsTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getServer();
			assertNotNull(server);
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
