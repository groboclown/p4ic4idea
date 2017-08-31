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
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for the various MergeFilesOptions values.
 */
@Jobs({ "job046667" })
@TestId("Dev112_MergeFilesOptionsTest")
public class MergeFilesOptionsTest extends P4JavaTestCase {

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
	public void testDefaultMergeFilesOptionsValues() {
		try {
			MergeFilesOptions opts = new MergeFilesOptions();
			assertFalse(opts.isBidirectionalInteg());
			assertFalse(opts.isReverseMapping());
			assertFalse(opts.isShowActionsOnly());
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
	public void testAllPositiveMergeFilesOptionsValues() {
		// Note -- we're not testing for sanity in the resulting
		// options... they just don't have to make sense, in other words.

		try {
			MergeFilesOptions opts = new MergeFilesOptions()
					.setBidirectionalInteg(true).setReverseMapping(true)
					.setShowActionsOnly(true).setChangelistId(120)
					.setMaxFiles(154);
			assertTrue(opts.isReverseMapping());
			assertTrue(opts.isShowActionsOnly());
			assertEquals(120, opts.getChangelistId());
			assertEquals(154, opts.getMaxFiles());
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(5, optsStrs.size());
			assertTrue(optsStrs.contains("-c120"));
			assertTrue(optsStrs.contains("-m154"));
			assertTrue(optsStrs.contains("-s"));
			assertTrue(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-r"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

	@Test
	public void testMergeFilesOptionsConstructorWithBranch() {
		// Some fairly simple random tests...
		try {
			MergeFilesOptions opts = new MergeFilesOptions(
					IChangelist.DEFAULT, // changelistId
					false, // showActionsOnly
					-17, // maxFiles
					"testbranch", // branch
					true, // bidirectionalInteg
					true // reverseMapping
			);
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(3, optsStrs.size());
			assertFalse(optsStrs.contains("-c0"));
			assertFalse(optsStrs.contains("-cdefault"));
			assertTrue(optsStrs.contains("-s"));
			assertFalse(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-btestbranch"));
			assertTrue(optsStrs.contains("-r"));
			assertFalse(optsStrs.contains("-m"));
			assertFalse(optsStrs.contains("-S"));
			assertFalse(optsStrs.contains("-P"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

	@Test
	public void testMergeFilesOptionsConstructorWithStream() {
		// Some fairly simple random tests...
		try {
			MergeFilesOptions opts = new MergeFilesOptions(
					IChangelist.DEFAULT, // changelistId
					false, // showActionsOnly
					17, // maxFiles
					"//p4java_stream/dev", // dev stream
					null, // specify a parent stream other than the stream's
							// actual parent
					false, // forceStreamMerge
					true // reverseMapping
			);
			List<String> optsStrs = opts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(3, optsStrs.size());
			assertFalse(optsStrs.contains("-c0"));
			assertFalse(optsStrs.contains("-cdefault"));
			assertFalse(optsStrs.contains("-s"));
			assertFalse(optsStrs.contains("-n"));
			assertTrue(optsStrs.contains("-r"));
			assertTrue(optsStrs.contains("-m17"));
			assertTrue(optsStrs.contains("-S//p4java_stream/dev"));
			assertFalse(optsStrs.contains("-P"));
			assertFalse(optsStrs.contains("-F"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
