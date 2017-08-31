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

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Standalone test for the various IntegrateFilesOptions options.
 */
@Standalone
@TestId("Dev101_IntegrateFilesOptionsTest")
public class IntegrateFilesOptionsTest extends P4JavaTestCase {

	public IntegrateFilesOptionsTest() {
	}

	@Test
	public void testDefaultIntegrateFilesOptionsValues() {
		try {
			IntegrateFilesOptions opts = new IntegrateFilesOptions();
			assertFalse(opts.isBidirectionalInteg());
			assertFalse(opts.isDeleteTargetAfterDelete());
			assertFalse(opts.isDisplayBaseDetails());
			assertFalse(opts.isDoBaselessMerge());
			assertFalse(opts.isDontCopyToClient());
			assertFalse(opts.isForceIntegration());
			assertFalse(opts.isIntegrateAllAfterReAdd());
			assertFalse(opts.isIntegrateAroundDeletedRevs());
			assertFalse(opts.isPropagateType());
			assertFalse(opts.isRebranchSourceAfterDelete());
			assertFalse(opts.isReverseMapping());
			assertFalse(opts.isShowActionsOnly());
			assertFalse(opts.isUseHaveRev());
			assertEquals(0, opts.getMaxFiles());
			assertTrue(opts.getChangelistId() == IChangelist.UNKNOWN);
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(0, optsStrs.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	@Test
	public void testAllPositiveIntegrateFilesOptionsValues() {
		// Note -- we're not testing for sanity in the resulting
		// options... they just don't have to make sense, in other words.
		
		try {
			IntegrateFilesOptions opts = new IntegrateFilesOptions()
													.setDeleteTargetAfterDelete(true)
													.setBidirectionalInteg(true)
													.setDisplayBaseDetails(true)
													.setDoBaselessMerge(true)
													.setDontCopyToClient(true)
													.setForceIntegration(true)
													.setIntegrateAllAfterReAdd(true)
													.setIntegrateAroundDeletedRevs(true)
													.setPropagateType(true)
													.setRebranchSourceAfterDelete(true)
													.setReverseMapping(true)
													.setShowActionsOnly(true)
													.setUseHaveRev(true)
													.setChangelistId(120)
													.setMaxFiles(154);
			assertTrue(opts.isDeleteTargetAfterDelete());
			assertTrue(opts.isDisplayBaseDetails());
			assertTrue(opts.isDoBaselessMerge());
			assertTrue(opts.isDontCopyToClient());
			assertTrue(opts.isForceIntegration());
			assertTrue(opts.isIntegrateAllAfterReAdd());
			assertTrue(opts.isIntegrateAroundDeletedRevs());
			assertTrue(opts.isPropagateType());
			assertTrue(opts.isRebranchSourceAfterDelete());
			assertTrue(opts.isReverseMapping());
			assertTrue(opts.isShowActionsOnly());
			assertTrue(opts.isUseHaveRev());
			assertEquals(120, opts.getChangelistId());
			assertEquals(154, opts.getMaxFiles());
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(15, optsStrs.size());
			assertTrue(optsStrs.contains("-c120"));
			assertTrue(optsStrs.contains("-m154"));
			assertTrue(optsStrs.contains("-s"));
			assertTrue(optsStrs.contains("-Di"));
			assertTrue(optsStrs.contains("-Dt"));
			assertTrue(optsStrs.contains("-Ds"));
			assertTrue(optsStrs.contains("-d"));
			assertTrue(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-i"));
			assertTrue(optsStrs.contains("-t"));
			assertTrue(optsStrs.contains("-v"));
			assertTrue(optsStrs.contains("-o"));
			assertTrue(optsStrs.contains("-r"));
			assertTrue(optsStrs.contains("-d"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} 
	}
	
	@Test
	public void testIntegrateFilesOptionsConstructor() {
		// Some fairly simple random tests...
		try {
			IntegrateFilesOptions opts = new IntegrateFilesOptions(
													IChangelist.DEFAULT, // changelistId
													true,				// bidirectionalInteg
													false,				// integrateAroundDeletedRevs
													true,				// rebranchSourceAfterDelete
													false,				// deleteTargetAfterDelete
													true,				// integrateAllAfterReAdd
													false,				// forceIntegration
													true,				// useHaveRev
													false,				// doBaselessMerge
													true,				// displayBaseDetails
													false,				// showActionsOnly
													true,				// reverseMapping
													false,				// propagateType
													true,				// dontCopyToClient
													-17					// maxFiles
											);	
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(7, optsStrs.size());
			assertFalse(optsStrs.contains("-c0"));
			assertFalse(optsStrs.contains("-cdefault"));
			assertTrue(optsStrs.contains("-s"));
			assertFalse(optsStrs.contains("-d"));
			assertTrue(optsStrs.contains("-Dt"));
			assertFalse(optsStrs.contains("-Ds"));
			assertTrue(optsStrs.contains("-Di"));
			assertFalse(optsStrs.contains("-f"));
			assertTrue(optsStrs.contains("-h"));
			assertFalse(optsStrs.contains("-i"));
			assertTrue(optsStrs.contains("-o"));
			assertFalse(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-r"));
			assertFalse(optsStrs.contains("-t"));
			assertTrue(optsStrs.contains("-v"));
			assertFalse(optsStrs.contains("-m"));
			assertFalse(optsStrs.contains("-m17"));
			assertFalse(optsStrs.contains("-m-17"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
