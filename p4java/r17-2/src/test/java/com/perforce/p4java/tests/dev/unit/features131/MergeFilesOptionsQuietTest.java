/**
 *
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
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for the various MergeFilesOptions values.
 */
@Jobs({ "job059637" })
@TestId("Dev131_MergeFilesOptionsQuietTest")
public class MergeFilesOptionsQuietTest extends P4JavaTestCase {

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
