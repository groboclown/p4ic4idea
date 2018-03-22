/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

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
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test reload options
 */
@Jobs({ "job056531" })
@TestId("Dev123_ReloadOptionsTest")
public class ReloadOptionsTest extends P4JavaTestCase {

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
	 * Test reload options constructors
	 */
    @Test
    public void testConstructors() {
        try {
            ReloadOptions opts = new ReloadOptions();
            assertNotNull(opts.processOptions(null));
            assertEquals(0, opts.processOptions(null).size());

            opts = new ReloadOptions("-f", "-ctestclient", "-ltestlabel");
            assertNotNull(opts.getOptions());
            String[] optsStrs = opts.getOptions().toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(optsStrs.length, 3);
            assertEquals("-f", optsStrs[0]);
            assertEquals("-ctestclient", optsStrs[1]);
            assertEquals("-ltestlabel", optsStrs[2]);

            opts = new ReloadOptions(true, "testclient", "testlabel");
            assertTrue(opts.isForce());
            assertEquals("testclient", opts.getClient());
            assertEquals("testlabel", opts.getLabel());
        } catch (OptionsException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

	/**
	 * Test reload options to strings
	 */
   @Test
    public void testToStrings() {
        try {
        	ReloadOptions opts = new ReloadOptions(false, "testclient", "testlabel");

            assertNotNull(opts.processOptions(null));
            String[] optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(2, optsStrs.length);

            // Order is not guaranteed here, so we have to
            // search for the expected strings at each position

            boolean foundForce = false;
            boolean foundClient = false;
            boolean foundLabel = false;

            for (String optStr : optsStrs) {
                if (optStr.equals("-f"))
                	foundForce = true;
                if (optStr.equals("-ctestclient"))
                	foundClient = true;
                if (optStr.equals("-ltestlabel"))
                    foundLabel = true;
            }

            assertFalse(foundForce);
            assertTrue(foundClient);
            assertTrue(foundLabel);

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
        	ReloadOptions opts = new ReloadOptions();
            opts.setForce(true);
            opts.setClient("testclient");
            opts.setLabel("testlabel");

            assertEquals(true, opts.isForce());
            assertEquals("testclient", opts.getClient());
            assertEquals("testlabel", opts.getLabel());
        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

}
