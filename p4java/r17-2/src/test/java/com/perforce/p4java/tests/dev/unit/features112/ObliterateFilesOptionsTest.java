/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the Options and ObliterateFilesOptions functionality.
 */
@Jobs({ "job041780", "job041824" })
@TestId("Dev112_ObliterateFilesOptionsTest")
public class ObliterateFilesOptionsTest extends P4JavaTestCase {

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
	 * Test the ObliterateFilesOptions constructors
	 */
	@Test
	public void testConstructors() {
		try {
			ObliterateFilesOptions opts = new ObliterateFilesOptions();
			assertNotNull(opts.processOptions(null));
			assertEquals(0, opts.processOptions(null).size());

			opts = new ObliterateFilesOptions("-y", "-a");
			assertNotNull(opts.getOptions());
			String[] optsStrs = opts.getOptions().toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(optsStrs.length, 2);
			assertEquals("-y", optsStrs[0]);
			assertEquals("-a", optsStrs[1]);

			opts = new ObliterateFilesOptions();
			assertFalse(opts.isExecuteObliterate());
			assertFalse(opts.isBranchedFirstHeadRevOnly());
			assertFalse(opts.isSkipArchiveSearchRemoval());
			assertFalse(opts.isSkipHaveSearch());

			opts = new ObliterateFilesOptions(false, true, false, true);
			assertFalse(opts.isExecuteObliterate());
			assertTrue(opts.isSkipArchiveSearchRemoval());
			assertFalse(opts.isBranchedFirstHeadRevOnly());
			assertTrue(opts.isSkipHaveSearch());
		} catch (OptionsException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testToStrings() {
		try {
			ObliterateFilesOptions opts = new ObliterateFilesOptions(true,
					false, true, false);

			assertNotNull(opts.processOptions(null));
			String[] optsStrs = opts.processOptions(null)
					.toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(2, optsStrs.length);

			// Order is not guaranteed here, so we have to
			// search for the expected strings at each position

			boolean foundExecuteObliterate = false;
			boolean foundSkipArchiveSearchRemoval = false;
			boolean foundBranchedFirstHeadRevOnly = false;
			boolean foundSkipHaveSearch = false;

			for (String optStr : optsStrs) {
				if (optStr.equals("-y"))
					foundExecuteObliterate = true;
				if (optStr.equals("-a"))
					foundSkipArchiveSearchRemoval = true;
				if (optStr.equals("-b"))
					foundBranchedFirstHeadRevOnly = true;
				if (optStr.equals("-h"))
					foundSkipHaveSearch = true;
			}

			assertTrue(foundExecuteObliterate);
			assertFalse(foundSkipArchiveSearchRemoval);
			assertTrue(foundBranchedFirstHeadRevOnly);
			assertFalse(foundSkipHaveSearch);

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
			ObliterateFilesOptions opts = new ObliterateFilesOptions();
			opts.setExecuteObliterate(true);
			opts.setSkipArchiveSearchRemoval(false);
			opts.setBranchedFirstHeadRevOnly(true);
			opts.setSkipHaveSearch(false);

			assertEquals(true, opts.isExecuteObliterate());
			assertEquals(false, opts.isSkipArchiveSearchRemoval());
			assertEquals(true, opts.isBranchedFirstHeadRevOnly());
			assertEquals(false, opts.isSkipHaveSearch());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
