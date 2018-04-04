/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the various IntegrateFilesOptions values.
 */
@Jobs({ "job046102" })
@TestId("Dev112_IntegrateFilesOptionsTest")
public class IntegrateFilesOptionsTest extends P4JavaTestCase {

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
                    .setBranchResolves(true).setDeleteResolves(true)
                    .setSkipIntegratedRevs(true)
                    .setDeleteTargetAfterDelete(true)
                    .setBidirectionalInteg(true).setDisplayBaseDetails(true)
                    .setDoBaselessMerge(true).setDontCopyToClient(true)
                    .setForceIntegration(true).setIntegrateAllAfterReAdd(true)
                    .setIntegrateAroundDeletedRevs(true).setPropagateType(true)
                    .setRebranchSourceAfterDelete(true).setReverseMapping(true)
                    .setShowActionsOnly(true).setUseHaveRev(true)
                    .setChangelistId(120).setMaxFiles(154)
		            .setShowBaseRevision(true).setShowScheduledResolve(true)
		            .setInteg2(true);
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
            assertEquals(18, optsStrs.size());
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
            assertTrue(optsStrs.contains("-v"));
            assertTrue(optsStrs.contains("-o"));
            assertTrue(optsStrs.contains("-r"));
            assertTrue(optsStrs.contains("-d"));
	        assertTrue(optsStrs.contains("-Obr"));
	        assertTrue(optsStrs.contains("-2"));
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
                    true, // bidirectionalInteg
                    false, // integrateAroundDeletedRevs
                    true, // rebranchSourceAfterDelete
                    false, // deleteTargetAfterDelete
                    true, // integrateAllAfterReAdd
                    true, // branchResolves
                    true, // deleteResolves
                    true, // skipIntegratedRevs
                    false, // forceIntegration
                    true, // useHaveRev
                    false, // doBaselessMerge
                    true, // displayBaseDetails
                    false, // showActionsOnly
                    true, // reverseMapping
                    false, // propagateType
                    true, // dontCopyToClient
                    -17 // maxFiles
            );
            List<String> optsStrs = opts.processOptions(null);
            assertNotNull(optsStrs);
            assertEquals(8, optsStrs.size());
            assertFalse(optsStrs.contains("-c0"));
            assertFalse(optsStrs.contains("-cdefault"));
            assertTrue(optsStrs.contains("-s"));
            assertFalse(optsStrs.contains("-d"));
            assertTrue(optsStrs.contains("-Dt"));
            assertFalse(optsStrs.contains("-Ds"));
            assertTrue(optsStrs.contains("-Di"));
            assertTrue(optsStrs.contains("-Rbds"));
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
