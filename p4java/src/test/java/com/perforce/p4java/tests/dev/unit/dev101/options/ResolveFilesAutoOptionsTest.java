/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple standalone tests for the ResolveFilesAutoOptions class.
 */
@Standalone
@TestId("Dev101_ResolveFilesAutoOptionsTest")
public class ResolveFilesAutoOptionsTest extends P4JavaTestCase {

	public ResolveFilesAutoOptionsTest() {
	}

	@Test
	public void testResolveFilesAutoOptions() {
		try {
			ResolveFilesAutoOptions opts = new ResolveFilesAutoOptions();
			assertFalse(opts.isAcceptTheirs());
			assertFalse(opts.isAcceptYours());
			assertFalse(opts.isForceResolve());
			assertFalse(opts.isSafeMerge());
			assertFalse(opts.isShowActionsOnly());
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(0, optsStrs.size());
			
			opts = new ResolveFilesAutoOptions();
			String[] params = Parameters.processParameters(opts, null, "-am", null);
			assertNotNull(params);
			assertEquals(1, params.length);
			assertEquals("-am", params[0]);

			opts = new ResolveFilesAutoOptions(
								true,	// showActionsOnly
								true,	// safeMerge
								true,	// acceptTheirs
								true,	// acceptYours,
								true	// forceResolve
					);
			
			assertTrue(opts.isAcceptTheirs());
			assertTrue(opts.isAcceptYours());
			assertTrue(opts.isForceResolve());
			assertTrue(opts.isSafeMerge());
			assertTrue(opts.isShowActionsOnly());
			optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(5, optsStrs.size());
			assertTrue(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-af"));
			assertTrue(optsStrs.contains("-as"));
			assertTrue(optsStrs.contains("-at"));
			assertTrue(optsStrs.contains("-ay"));
			
			opts = new ResolveFilesAutoOptions()
						.setAcceptTheirs(false)
						.setAcceptYours(true)
						.setForceResolve(false)
						.setSafeMerge(true)
						.setShowActionsOnly(false);

			assertFalse(opts.isAcceptTheirs());
			assertTrue(opts.isAcceptYours());
			assertFalse(opts.isForceResolve());
			assertTrue(opts.isSafeMerge());
			assertFalse(opts.isShowActionsOnly());
			optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(2, optsStrs.size());
			assertTrue(optsStrs.contains("-ay"));
			assertTrue(optsStrs.contains("-as"));
			
			opts = new ResolveFilesAutoOptions()
						.setAcceptTheirs(true)
						.setAcceptYours(false)
						.setForceResolve(true)
						.setSafeMerge(false)
						.setShowActionsOnly(true);

			assertTrue(opts.isAcceptTheirs());
			assertFalse(opts.isAcceptYours());
			assertTrue(opts.isForceResolve());
			assertFalse(opts.isSafeMerge());
			assertTrue(opts.isShowActionsOnly());
			optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(3, optsStrs.size());
			assertTrue(optsStrs.contains("-at"));
			assertTrue(optsStrs.contains("-af"));
			assertTrue(optsStrs.contains("-n"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
