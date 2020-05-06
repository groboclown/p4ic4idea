/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.option.server.GetBranchSpecsOptions;
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
 * Test get branch specs with case-insensitive name filter.
 */
@Jobs({ "job046825" })
@TestId("Dev112_GetBranchSpecsTest")
public class GetBranchSpecsTest extends P4JavaRshTestCase {

	IOptionsServer superServer = null;
	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetBranchSpecsTest.class.getSimpleName());

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
	 * Test get branch specs with case-insensitive name filter.
	 */
	@Test
	public void testGetBranchSpecs() {
		int randNum = getRandomInt();
		String branchName = "Test-Branch-job046825-" + randNum;

		try {
			// Create a new branch with a mixed of lower and upper case letters
			IBranchSpec newBranch = BranchSpec.newBranchSpec(server,branchName,"a new branch " + branchName,
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
			    superServer = getSuperConnection(p4d.getRSHURL());
				if (superServer != null) {
					String message = superServer.deleteBranchSpec(branchName, true);
					assertNotNull(message);
				}
			} catch (Exception e) {
				// Can't do much here...
			} 
		}
	}
}
