/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.client.PopulateFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the Options and PopulateFilesOptionsTest functionality.
 */
@Jobs({"job058523"})
@TestId("Dev123_PopulateFilesOptionsTest")
public class PopulateFilesOptionsTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", PopulateFilesOptionsTest.class.getSimpleName());

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 * class.
	 */
	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
	}


	@Test
	public void testConstructors() {
		try {
			PopulateFilesOptions opts = new PopulateFilesOptions();
			assertNotNull(opts.processOptions(null));
			assertEquals(0, opts.processOptions(null).size());

			opts = new PopulateFilesOptions("-m10", "-dtest");
			assertNotNull(opts.getOptions());
			String[] optsStrs = opts.getOptions().toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(optsStrs.length, 2);
			assertEquals("-m10", optsStrs[0]);
			assertEquals("-dtest", optsStrs[1]);

			opts = new PopulateFilesOptions("test populate cmd", true, 1000, true, true, "test branch", true, false);
			assertEquals("test populate cmd", opts.getDescription());
			assertTrue(opts.isForceBranchDeletedFiles());
			assertEquals(1000, opts.getMaxFiles());
			assertTrue(opts.isNoUpdate());
			assertTrue(opts.isShowPopulatedFiles());
			assertEquals("test branch", opts.getBranch());
			assertTrue(opts.isReverseMapping());
			assertFalse(opts.isBidirectional());

		} catch (OptionsException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testToStrings() {
		try {
			PopulateFilesOptions opts = new PopulateFilesOptions("test populate cmd", false, 1000, true, false, "test stream", "test parent stream", true);

			assertNotNull(opts.processOptions(null));
			String[] optsStrs = opts.processOptions(null)
					.toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(6, optsStrs.length);

			// Order is not guaranteed here, so we have to
			// search for the expected strings at each position

			boolean foundDescription = false;
			boolean foundForceBranchDeletedFiles = false;
			boolean foundMaxFiles = false;
			boolean foundNoUpdate = false;
			boolean foundShowPopulatedFiles = false;
			boolean foundStream = false;
			boolean foundParentStream = false;
			boolean foundReverseMapping = false;

			for (String optStr : optsStrs) {
				if (optStr.equals("-dtest populate cmd"))
					foundDescription = true;
				if (optStr.equals("-f"))
					foundForceBranchDeletedFiles = true;
				if (optStr.equals("-m1000"))
					foundMaxFiles = true;
				if (optStr.equals("-n"))
					foundNoUpdate = true;
				if (optStr.equals("-o"))
					foundShowPopulatedFiles = true;
				if (optStr.equals("-Stest stream"))
					foundStream = true;
				if (optStr.equals("-Ptest parent stream"))
					foundParentStream = true;
				if (optStr.equals("-r"))
					foundReverseMapping = true;
			}

			assertTrue(foundDescription);
			assertFalse(foundForceBranchDeletedFiles);
			assertTrue(foundMaxFiles);
			assertTrue(foundNoUpdate);
			assertFalse(foundShowPopulatedFiles);
			assertTrue(foundStream);
			assertTrue(foundParentStream);
			assertTrue(foundReverseMapping);

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
			PopulateFilesOptions opts = new PopulateFilesOptions();
			opts.setDescription("test populate cmd");
			opts.setForceBranchDeletedFiles(true);
			opts.setMaxFiles(1000);
			opts.setNoUpdate(false);
			opts.setShowPopulatedFiles(true);
			opts.setBranch("test branch");
			opts.setStream("test stream");
			opts.setParentStream("test parent stream");
			opts.setBidirectional(true);
			opts.setReverseMapping(false);

			assertEquals("test populate cmd", opts.getDescription());
			assertEquals(true, opts.isForceBranchDeletedFiles());
			assertEquals(1000, opts.getMaxFiles());
			assertEquals(false, opts.isNoUpdate());
			assertEquals(true, opts.isShowPopulatedFiles());
			assertEquals("test branch", opts.getBranch());
			assertEquals("test stream", opts.getStream());
			assertEquals("test parent stream", opts.getParentStream());
			assertEquals(true, opts.isBidirectional());
			assertEquals(false, opts.isReverseMapping());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

}
