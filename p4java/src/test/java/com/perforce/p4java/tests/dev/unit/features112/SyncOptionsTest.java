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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features121.GetStreamOptionsTest;

/**
 * Test the various SyncOptions values.
 */
@Jobs({ "job046091" })
@TestId("Dev112_SyncOptionsTest")
public class SyncOptionsTest extends P4JavaRshTestCase {

	@Test
	public void testConstructors() {
		try {
			SyncOptions opts = new SyncOptions();
			assertNotNull(opts.processOptions(null));
			assertEquals(0, opts.processOptions(null).size());

			opts = new SyncOptions("-f", "-n");
			assertNotNull(opts.getOptions());
			String[] optsStrs = opts.getOptions().toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(optsStrs.length, 2);
			assertEquals("-f", optsStrs[0]);
			assertEquals("-n", optsStrs[1]);

			opts = new SyncOptions(true, false, true, false, true);
			assertTrue(opts.isForceUpdate());
			assertFalse(opts.isNoUpdate());
			assertTrue(opts.isClientBypass());
			assertFalse(opts.isServerBypass());
			assertTrue(opts.isSafetyCheck());
		} catch (OptionsException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testToStrings() {
		try {
			SyncOptions opts = new SyncOptions(false, true, false, true, false);

			assertNotNull(opts.processOptions(null));
			String[] optsStrs = opts.processOptions(null)
					.toArray(new String[0]);
			assertNotNull(optsStrs);
			assertEquals(2, optsStrs.length);

			// Order is not guaranteed here, so we have to
			// search for the expected strings at each position

			boolean foundForceUpdate = false;
			boolean foundNoUpdate = false;
			boolean foundClientBypass = false;
			boolean foundServerBypass = false;
			boolean foundSafetyCheck = false;

			for (String optStr : optsStrs) {
				if (optStr.equals("-f"))
					foundForceUpdate = true;
				if (optStr.equals("-n"))
					foundNoUpdate = true;
				if (optStr.equals("-k"))
					foundClientBypass = true;
				if (optStr.equals("-p"))
					foundServerBypass = true;
				if (optStr.equals("-s"))
					foundSafetyCheck = true;
			}

			assertFalse(foundForceUpdate);
			assertTrue(foundNoUpdate);
			assertFalse(foundClientBypass);
			assertTrue(foundServerBypass);
			assertFalse(foundSafetyCheck);

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
			SyncOptions opts = new SyncOptions();
			opts.setForceUpdate(true);
			opts.setNoUpdate(true);
			opts.setClientBypass(false);
			opts.setServerBypass(true);
			opts.setSafetyCheck(false);

			assertEquals(true, opts.isForceUpdate());
			assertEquals(true, opts.isNoUpdate());
			assertEquals(false, opts.isClientBypass());
			assertEquals(true, opts.isServerBypass());
			assertEquals(false, opts.isSafetyCheck());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
