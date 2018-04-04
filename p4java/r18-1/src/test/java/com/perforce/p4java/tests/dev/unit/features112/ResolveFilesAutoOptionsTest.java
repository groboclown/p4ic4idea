/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for the various ResolveFilesAutoOptions values.
 */
@Jobs({ "job046062", "job046102" })
@TestId("Dev112_ResolveFilesAutoOptionsTest")
public class ResolveFilesAutoOptionsTest extends P4JavaTestCase {

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
    public void testResolveFilesAutoOptions() {
        try {
            ResolveFilesAutoOptions opts = new ResolveFilesAutoOptions();
            assertFalse(opts.isAcceptTheirs());
            assertFalse(opts.isAcceptYours());
            assertFalse(opts.isForceResolve());
            assertFalse(opts.isSafeMerge());
            assertFalse(opts.isShowActionsOnly());
            assertFalse(opts.isResolveFileBranching());
            assertFalse(opts.isResolveFileContentChanges());
            assertFalse(opts.isResolveFileDeletions());
            assertFalse(opts.isResolveMovedFiles());
            assertFalse(opts.isResolveFiletypeChanges());
            assertEquals(IChangelist.UNKNOWN, opts.getChangelistId());

            List<String> optsStrs = opts.processOptions(null);
            assertNotNull(optsStrs);
            assertEquals(0, optsStrs.size());

            opts = new ResolveFilesAutoOptions();
            String[] params = Parameters.processParameters(opts, null, "-am",
                    null);
            assertNotNull(params);
            assertEquals(1, params.length);
            assertEquals("-am", params[0]);

            opts = new ResolveFilesAutoOptions(true, // showActionsOnly
                    true, // safeMerge
                    true, // acceptTheirs
                    true, // acceptYours
                    true, // forceResolve
                    true, // resolveFileBranching
                    true, // resolveFileContentChanges
                    true, // resolveFileDeletions
                    true, // resolveMovedRenamedFiles
                    true, // resolveFiletypeChanges
                    IChangelist.DEFAULT // IChangelist.DEFAULT
            );

            assertTrue(opts.isAcceptTheirs());
            assertTrue(opts.isAcceptYours());
            assertTrue(opts.isForceResolve());
            assertTrue(opts.isSafeMerge());
            assertTrue(opts.isShowActionsOnly());
            assertTrue(opts.isResolveFileBranching());
            assertTrue(opts.isResolveFileContentChanges());
            assertTrue(opts.isResolveFileDeletions());
            assertTrue(opts.isResolveMovedFiles());
            assertTrue(opts.isResolveFiletypeChanges());
            assertEquals(IChangelist.DEFAULT, opts.getChangelistId());
            optsStrs = opts.processOptions(null);
            assertNotNull(optsStrs);
            assertEquals(7, optsStrs.size());
            assertTrue(optsStrs.contains("-n"));
            assertTrue(optsStrs.contains("-af"));
            assertTrue(optsStrs.contains("-as"));
            assertTrue(optsStrs.contains("-at"));
            assertTrue(optsStrs.contains("-ay"));
            assertTrue(optsStrs.contains("-Abcdmt"));
            assertTrue(optsStrs.contains("-cdefault"));
            opts = new ResolveFilesAutoOptions().setAcceptTheirs(false)
                    .setAcceptYours(true).setForceResolve(false)
                    .setSafeMerge(true).setShowActionsOnly(false);

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

            opts = new ResolveFilesAutoOptions().setAcceptTheirs(true)
                    .setAcceptYours(false).setForceResolve(true)
                    .setSafeMerge(false).setShowActionsOnly(true);

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
