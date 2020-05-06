/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the various MergeFilesOptions values.
 */
@Jobs({ "job059637" })
@TestId("Dev131_MergeFilesOptionsQuietTest")
public class MergeFilesOptionsQuietTest extends P4JavaRshTestCase {

	/**
	 * Test default values.
	 */
	@Test
	public void testDefaultMergeFilesOptionsValues() {
		try {
			MergeFilesOptions opts = new MergeFilesOptions();
			assertFalse(opts.isBidirectionalInteg());
			assertFalse(opts.isReverseMapping());
			assertFalse(opts.isShowActionsOnly());
			assertFalse(opts.isQuiet());
			assertEquals(0, opts.getMaxFiles());
			assertTrue(opts.getChangelistId() == IChangelist.UNKNOWN);
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(0, optsStrs.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

	/**
	 * Test positive values.
	 */
	@Test
	public void testAllPositiveMergeFilesOptionsValues() {
		try {
			MergeFilesOptions opts = new MergeFilesOptions()
					.setBidirectionalInteg(true).setReverseMapping(true)
					.setShowActionsOnly(true).setQuiet(true)
					.setChangelistId(120).setMaxFiles(154);
			assertTrue(opts.isReverseMapping());
			assertTrue(opts.isShowActionsOnly());
			assertTrue(opts.isQuiet());
			assertEquals(120, opts.getChangelistId());
			assertEquals(154, opts.getMaxFiles());
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(6, optsStrs.size());
			assertTrue(optsStrs.contains("-c120"));
			assertTrue(optsStrs.contains("-m154"));
			assertTrue(optsStrs.contains("-s"));
			assertTrue(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-r"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
