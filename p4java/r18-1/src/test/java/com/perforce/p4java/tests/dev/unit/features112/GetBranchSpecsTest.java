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
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test get branch specs with case-insensitive name filter.
 */
@Jobs({ "job046825" })
@TestId("Dev112_GetBranchSpecsTest")
public class GetBranchSpecsTest extends P4JavaTestCase {

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
	 * Test get branch specs with case-insensitive name filter.
	 */
	@Test
	public void testGetBranchSpecs() {
		int randNum = getRandomInt();
		String branchName = "Test-Branch-job046825-" + randNum;

		try {
			// Create a new branch with a mixed of lower and upper case letters
			IBranchSpec newBranch = BranchSpec
					.newBranchSpec(
							server,
							branchName,
							"a new branch " + branchName,
							new String[] { "//depot/112Dev/Attributes/... //depot/112Dev/"
									+ branchName + "/Attributes/... " });
			String message = server.createBranchSpec(newBranch);
			assertNotNull(message);
			assertEquals("Branch " + branchName + " saved.", message);
			
			// Setting a default case-sensitive name filter with lower case
			List<IBranchSpecSummary> branchSpecs = server
					.getBranchSpecs(new GetBranchSpecsOptions()
							.setNameFilter("test-branch-job046825-*"));
			assertNotNull(branchSpecs);

			// Should get an empty list, since the filter is case sensitive
			assertEquals(0, branchSpecs.size());

			// Setting a name filter with lower case
			branchSpecs = server.getBranchSpecs(new GetBranchSpecsOptions()
					.setCaseInsensitiveNameFilter("test-branch-job046825-*"));
			assertNotNull(branchSpecs);

			// Should get one in the list, since the filter is case sensitive
			assertEquals(1, branchSpecs.size());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				// Delete the test branch
				server = getServerAsSuper();
				if (server != null) {
					String message = server.deleteBranchSpec(branchName, true);
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
