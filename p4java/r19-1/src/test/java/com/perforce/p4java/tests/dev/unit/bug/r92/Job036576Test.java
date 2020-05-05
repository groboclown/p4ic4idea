/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r92;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.perforce.p4java.impl.generic.client.ClientSubmitOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests client submit options toString method formatting
 * as per job036575. As a side effect, tends to test the
 * associated constructor as well, but don't count on it...
 * 
 * @testid Job036576Test
 * @job job036576
 */

@TestId("Job036576Test")
@Jobs({"job036576"})
@Standalone
public class Job036576Test extends P4JavaTestCase {

	@Test
	public void testClientSubmitOptionsToString() {
		final String nullOptStr = "";
		final String allOptStr = "submitunchanged+reopen revertunchanged+reopen leaveunchanged+reopen";
		final String noOptStr = "";
		final String[] optString = {
				// a sample set of strings only, but representative;
				// order *within* an individual string matters...
				"",
				"submitunchanged",
				"submitunchanged+reopen",
				"revertunchanged",
				"revertunchanged+reopen",
				"leaveunchanged",
				"leaveunchanged+reopen",
				"submitunchanged+reopen revertunchanged",
				"submitunchanged revertunchanged leaveunchanged+reopen"
		};
		// Note: mapString must contain map-based input strings that correspond
		// to the optString output, otherwise the test will (probably) fail
		
		final String[] mapString = {
				"",
				"submitunchanged",
				"submitunchangedReopen",
				"revertunchanged",
				"revertunchangedReopen",
				"leaveunchanged",
				"leaveunchangedReopen",
				"submitunchangedReopen revertunchanged",
				"submitunchanged revertunchanged leaveunchangedReopen"
		};
		
		// Do a null test first:
		ClientSubmitOptions subOptions = new ClientSubmitOptions(null);
		assertEquals(subOptions.toString(), nullOptStr);
		
		// Do the full-quid tests:
		subOptions = new ClientSubmitOptions(true, true, true, true, true, true);
		assertNotNull("null submit option toString() return", subOptions.toString());
		assertEquals(subOptions.toString(), allOptStr);
		subOptions = new ClientSubmitOptions(false, false, false, false, false, false);
		assertNotNull("null submit option toString() return", subOptions.toString());
		assertEquals(subOptions.toString(), noOptStr);
		
		// Now do a tasty sampler of the rest:
		
		for (String str : optString) {
			subOptions = new ClientSubmitOptions(str);
			assertNotNull("null submit option toString() return", subOptions.toString());
			assertEquals("Submit option out didn't equal option string in",
					subOptions.toString(), str);
		}
		
		// Now some map-based tests:
		
		int i = 0;
		for (String str : mapString) {
			subOptions = new ClientSubmitOptions(str);
			assertNotNull("null submit option toString() return", subOptions.toString());
			assertEquals("Submit option out didn't equal option string in",
					subOptions.toString(), optString[i++]);
		}
	}
}
