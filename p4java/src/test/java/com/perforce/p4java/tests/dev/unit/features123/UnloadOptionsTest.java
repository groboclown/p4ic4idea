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
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test unload options
 */
@Jobs({ "job056531" })
@TestId("Dev123_UnloadOptionsTest")
public class UnloadOptionsTest extends P4JavaTestCase {

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
	 * Test unload options constructors
	 */
    @Test
    public void testConstructors() {
        try {
            UnloadOptions opts = new UnloadOptions();
            assertNotNull(opts.processOptions(null));
            assertEquals(0, opts.processOptions(null).size());

            opts = new UnloadOptions("-f", "-z", "-ctestclient", "-ltestlabel");
            assertNotNull(opts.getOptions());
            String[] optsStrs = opts.getOptions().toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(optsStrs.length, 4);
            assertEquals("-f", optsStrs[0]);
            assertEquals("-z", optsStrs[1]);
            assertEquals("-ctestclient", optsStrs[2]);
            assertEquals("-ltestlabel", optsStrs[3]);

            opts = new UnloadOptions("-f", "-z", "-ac", "-d2012/05/22:11:25:10", "-utestuser");
            assertNotNull(opts.getOptions());
            optsStrs = opts.getOptions().toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(optsStrs.length, 5);
            assertEquals("-f", optsStrs[0]);
            assertEquals("-z", optsStrs[1]);
            assertEquals("-ac", optsStrs[2]);
            assertEquals("-d2012/05/22:11:25:10", optsStrs[3]);
            assertEquals("-utestuser", optsStrs[4]);

            opts = new UnloadOptions(true, false, "testclient", "testlabel");
            assertTrue(opts.isForce());
            assertFalse(opts.isCompress());
            assertEquals("testclient", opts.getClient());
            assertEquals("testlabel", opts.getLabel());

            opts = new UnloadOptions(false, true, "l", "2012/05/22:11:25:10", "testuser");
            assertFalse(opts.isForce());
            assertTrue(opts.isCompress());
            assertEquals("l", opts.getAll());
            assertEquals("2012/05/22:11:25:10", opts.getDate());
            assertEquals("testuser", opts.getUser());
        
        } catch (OptionsException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

	/**
	 * Test unload options to strings
	 */
   @Test
    public void testToStrings() {
        try {
        	UnloadOptions opts = new UnloadOptions(true, false, "testclient", "testlabel");

            assertNotNull(opts.processOptions(null));
            String[] optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(3, optsStrs.length);

            // Order is not guaranteed here, so we have to
            // search for the expected strings at each position

            boolean foundForce = false;
            boolean foundCompress = false;
            boolean foundAll = false;
            boolean foundClient = false;
            boolean foundLabel = false;
            boolean foundDate = false;
            boolean foundUser = false;

            for (String optStr : optsStrs) {
                if (optStr.equals("-f"))
                	foundForce = true;
                if (optStr.equals("-z"))
                	foundCompress = true;
                if (optStr.equals("-ctestclient"))
                	foundClient = true;
                if (optStr.equals("-ltestlabel"))
                    foundLabel = true;
            }

            assertTrue(foundForce);
            assertFalse(foundCompress);
            assertTrue(foundClient);
            assertTrue(foundLabel);

        	opts = new UnloadOptions(true, true, "", "2012/05/22:11:25:10", "testuser");

            assertNotNull(opts.processOptions(null));
            optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            assertEquals(4, optsStrs.length);

            // Order is not guaranteed here, so we have to
            // search for the expected strings at each position

            foundForce = false;
            foundCompress = false;
            foundAll = false;
            foundClient = false;
            foundLabel = false;
            foundDate = false;
            foundUser = false;

            for (String optStr : optsStrs) {
                if (optStr.equals("-f"))
                	foundForce = true;
                if (optStr.equals("-z"))
                	foundCompress = true;
                if (optStr.equals("-a"))
                	foundAll = true;
                if (optStr.equals("-d2012/05/22:11:25:10"))
                	foundDate = true;
                if (optStr.equals("-utestuser"))
                	foundUser = true;
            }

            assertTrue(foundForce);
            assertTrue(foundCompress);
            assertFalse(foundAll);
            assertTrue(foundDate);
            assertTrue(foundUser);

            opts = new UnloadOptions(true, true, "l", "2012/05/22:11:25:10", "testuser");
            assertNotNull(opts.processOptions(null));
            optsStrs = opts.processOptions(null)
                    .toArray(new String[0]);
            assertNotNull(optsStrs);
            foundAll = false;
            for (String optStr : optsStrs) {
                if (optStr.equals("-al"))
                	foundAll = true;
            }
            assertTrue(foundAll);
            
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
        	UnloadOptions opts = new UnloadOptions();
            opts.setForce(false);
            opts.setCompress(true);
            opts.setClient("testclient");
            opts.setLabel("testlabel");

            assertEquals(false, opts.isForce());
            assertEquals(true, opts.isCompress());
            assertEquals("testclient", opts.getClient());
            assertEquals("testlabel", opts.getLabel());

        	opts = new UnloadOptions();
            opts.setForce(false);
            opts.setCompress(true);
            opts.setAll("c");
            opts.setDate("2012/05/22:11:25:10");
            opts.setUser("testuser");

            assertEquals(false, opts.isForce());
            assertEquals(true, opts.isCompress());
            assertEquals("c", opts.getAll());
            assertEquals("2012/05/22:11:25:10", opts.getDate());
            assertEquals("testuser", opts.getUser());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }

}
