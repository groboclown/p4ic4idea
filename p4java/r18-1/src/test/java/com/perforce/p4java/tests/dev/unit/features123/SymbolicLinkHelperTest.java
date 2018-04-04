/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test symbolic link helper (JDK 7 or above)
 */
public class SymbolicLinkHelperTest extends P4JavaTestCase {

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
	 * Test symbolic link
	 */
	@Test
	public void tesSymbolicLink() {
		// Check if symlink capable
		if (SymbolicLinkHelper.isSymbolicLinkCapable()) {

			String target = "/usr/bin";
			String link = "/tmp/user-bin-" + getRandomInt();

			// Create symbolic link
			Object path = SymbolicLinkHelper.createSymbolicLink(link, target);

			boolean isSymlink = SymbolicLinkHelper.isSymbolicLink(path
					.toString());
			assertTrue(isSymlink);
			
			File file = new File(path.toString());
			if (file.exists()) {
				file.delete();
			}
		}
	}

}
