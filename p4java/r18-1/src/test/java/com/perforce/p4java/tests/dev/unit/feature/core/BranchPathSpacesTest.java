/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test to ensure that branches work properly when there are
 * spaces in a view path or file name. Inspired by job035374.
 * 
 * @job job035374
 * @testid BranchPathSpacesTest01
 */

@Jobs({"Job035374"})
@TestId("BranchPathSpacesTest01")
public class BranchPathSpacesTest extends P4JavaTestCase {

	@Test
	public void testSpacedPathNames() {
		final int MAX_ATTEMPTS = 5;
		final String testLeft01 = "\"//depot/ratna/Java9.2 TimeLapse View test/...\"";
		final String testRight01 = "\"//depot/ratna/Java9.2 TimeLapse View test2/...\"";
		try {
			IServer server = getServer();
			assertNotNull("Null server returned", server);
			String newBranchName = getRandomName("Branch");
			
			List<IBranchSpecSummary> branchList = null;
			
			int i = 0;
			while (i <= MAX_ATTEMPTS) {
				branchList = server.getBranchSpecs(null, newBranchName + "*", 0);
				assertNotNull(branchList);
				if (i++ >= MAX_ATTEMPTS) {
					fail("Unable to find an unused branch name on server after "
							+ MAX_ATTEMPTS + " attempts");
				}
				if (branchList.size() == 0) {
					break;
				}
			}
			
			ViewMap<IBranchMapping> viewMap = new ViewMap<IBranchMapping>();
			viewMap.addEntry(new BranchSpec.BranchViewMapping(0, testLeft01 + " " + testRight01));
			
			IBranchSpec newBranch = new BranchSpec(
						newBranchName,
						getUserName(),
						testId + " branch test branch",
						false,
						null,
						null,
						viewMap
					);
			
			String createResult = server.createBranchSpec(newBranch);
			assertNotNull("Null branch create result string", createResult);
			assertTrue("Unexpected branch creation result: '" + createResult + "'",
											createResult.contains("saved"));
			
			IBranchSpec retrievedBranch = server.getBranchSpec(newBranchName);
			assertNotNull("Unable to retrieve test branch: '" + newBranchName + "'",
											retrievedBranch);
			
			ViewMap<IBranchMapping> retreivedViewMap = retrievedBranch.getBranchView();
			assertNotNull("Null view map in retrieved branchspec", retreivedViewMap);
			IBranchMapping viewMapping = retreivedViewMap.getEntry(0);
			assertNotNull("Null view mapping[0] in retrieved branchspec view map",
											viewMapping);
			
			String leftStr = viewMapping.getSourceSpec();
			String rightStr = viewMapping.getTargetSpec();
			
			assertNotNull(leftStr);
			assertNotNull(rightStr);
			
			assertEquals("//depot/ratna/Java9.2 TimeLapse View test/...", leftStr);
			assertEquals("//depot/ratna/Java9.2 TimeLapse View test2/...", rightStr);
			
			String deleteResult = server.deleteBranchSpec(newBranchName, false);
			assertNotNull(deleteResult);
			assertTrue(deleteResult.contains("deleted"));
			branchList = server.getBranchSpecs(null, newBranchName + "*", 0);
			assertNotNull("Unexpected null delete result string", branchList);
			assertTrue("test branch '" + newBranchName + "' still exists", branchList.size() == 0);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
