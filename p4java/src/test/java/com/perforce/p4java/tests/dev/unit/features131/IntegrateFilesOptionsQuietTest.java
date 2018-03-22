/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for the various IntegrateFilesOptions values.
 */
@Jobs({ "job059637" })
@TestId("Dev131_IntegrateFilesOptionsTest")
public class IntegrateFilesOptionsQuietTest extends P4JavaTestCase {

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
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
	}

	/**
	 * Test default values.
	 */
	@Test
    public void testDefaultIntegrateFilesOptionsValues() {
        try {
            IntegrateFilesOptions opts = new IntegrateFilesOptions();
            assertFalse(opts.isBranchResolves());
            assertFalse(opts.isDeleteResolves());
            assertFalse(opts.isSkipIntegratedRevs());
            assertFalse(opts.isBidirectionalInteg());
            assertFalse(opts.isDeleteTargetAfterDelete());
            assertFalse(opts.isDisplayBaseDetails());
            assertFalse(opts.isDoBaselessMerge());
            assertFalse(opts.isDontCopyToClient());
            assertFalse(opts.isForceIntegration());
            assertFalse(opts.isIntegrateAllAfterReAdd());
            assertFalse(opts.isIntegrateAroundDeletedRevs());
            assertFalse(opts.isPropagateType());
            assertFalse(opts.isQuiet());
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

	/**
	 * Test all positive values.
	 */
    @Test
    public void testAllPositiveIntegrateFilesOptionsValues() {
    	try {
            IntegrateFilesOptions opts = new IntegrateFilesOptions()
                    .setBranchResolves(true).setDeleteResolves(true)
                    .setSkipIntegratedRevs(true)
                    .setDeleteTargetAfterDelete(true)
                    .setBidirectionalInteg(true).setDisplayBaseDetails(true)
                    .setDoBaselessMerge(true).setDontCopyToClient(true)
                    .setForceIntegration(true).setIntegrateAllAfterReAdd(true)
                    .setIntegrateAroundDeletedRevs(true).setPropagateType(true)
                    .setRebranchSourceAfterDelete(true).setReverseMapping(true)
                    .setShowActionsOnly(true).setUseHaveRev(true)
                    .setChangelistId(120).setMaxFiles(154).setQuiet(true);
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
            assertEquals(17, optsStrs.size());
            assertTrue(optsStrs.contains("-c120"));
            assertTrue(optsStrs.contains("-m154"));
            assertTrue(optsStrs.contains("-s"));
            assertTrue(optsStrs.contains("-Di"));
            assertTrue(optsStrs.contains("-Dt"));
            assertTrue(optsStrs.contains("-Ds"));
            assertTrue(optsStrs.contains("-Rbds"));
            assertTrue(optsStrs.contains("-d"));
            assertTrue(optsStrs.contains("-n"));
            assertTrue(optsStrs.contains("-i"));
            assertTrue(optsStrs.contains("-t"));
            assertTrue(optsStrs.contains("-q"));
            assertTrue(optsStrs.contains("-v"));
            assertTrue(optsStrs.contains("-o"));
            assertTrue(optsStrs.contains("-r"));
            assertTrue(optsStrs.contains("-d"));
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
