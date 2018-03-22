/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.server.GetFixesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple standalone test scaffold for experimenting with and testing
 * the Options and FixListOptions functionality.
 */

@Standalone
@Jobs({"job039408"})
@TestId("Dev101_FixListOptionsTest")
public class FixListOptionsTest extends P4JavaTestCase {

	public FixListOptionsTest() {
	}

	@Test
	public void testConstructors() {
		try {
			GetFixesOptions opts = new GetFixesOptions();
			assertNotNull(opts.processOptions(null));
			assertEquals(0, opts.processOptions(null).size());
			
			opts = new GetFixesOptions("-m10", "-cdefault");
			assertNotNull(opts.getOptions());
			String[] optsStrs = opts.getOptions().toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(optsStrs.length, 2);
			assertEquals("-m10", optsStrs[0]);
			assertEquals("-cdefault", optsStrs[1]);
			
			opts = new GetFixesOptions(56, "job67890", true, 0);
			assertEquals(0, opts.getMaxFixes());
			assertNotNull(opts.getJobId());
			assertEquals("job67890", opts.getJobId());
			assertTrue(opts.isIncludeIntegrations());
			assertEquals(56, opts.getChangelistId());
			
			opts = new GetFixesOptions(IChangelist.DEFAULT, null, false, 120);
			assertEquals(IChangelist.DEFAULT, opts.getChangelistId());
			assertNull(opts.getJobId());
			assertFalse(opts.isIncludeIntegrations());
			assertEquals(120, opts.getMaxFixes());
		} catch (OptionsException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	
	@Test
	public void testToStrings() {
		try {
			GetFixesOptions opts = new GetFixesOptions(56, "job67890", true, 0);

			assertNotNull(opts.processOptions(null));
			String[] optsStrs = opts.processOptions(null).toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(3, optsStrs.length);

			// Order is not guaranteed here, so we have to
			// search for the expected strings at each position
			
			boolean foundChangelistId = false;
			boolean foundIncludeIntegrations = false;
			boolean foundJobId = false;
			boolean foundMaxFixes = false;
			
			for (String optStr : optsStrs) {
				if (optStr.equals("-c56")) foundChangelistId = true;
				if (optStr.equals("-jjob67890")) foundJobId = true;
				if (optStr.equals("-i")) foundIncludeIntegrations  = true;
			}
			
			assertTrue(foundChangelistId);
			assertTrue(foundJobId);
			assertTrue(foundIncludeIntegrations);
			
			opts = new GetFixesOptions(IChangelist.DEFAULT, null, false, 120);
			assertNotNull(opts.processOptions(null));
			optsStrs = opts.processOptions(null).toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(2, optsStrs.length);
			
			foundChangelistId = foundIncludeIntegrations = foundJobId = foundMaxFixes = false;
			for (String optStr : optsStrs) {
				if (optStr.equals("-cdefault")) foundChangelistId = true;
				if (optStr.equals("-m120")) foundMaxFixes  = true;
			}
			
			assertTrue(foundChangelistId);
			assertTrue(foundMaxFixes);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * Test setters and chaining
	 */
	@Test
	public void testSetters() {
		try {
			GetFixesOptions opts = new GetFixesOptions();
			opts.setChangelistId(58).setIncludeIntegrations(true).setJobId("TestJobID").setMaxFixes(-1);
			assertEquals(58, opts.getChangelistId());
			assertEquals(true, opts.isIncludeIntegrations());
			assertEquals("TestJobID", opts.getJobId());
			assertEquals(-1, opts.getMaxFixes());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
