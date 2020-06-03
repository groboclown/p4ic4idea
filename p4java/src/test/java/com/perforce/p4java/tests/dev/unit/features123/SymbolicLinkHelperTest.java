/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import com.perforce.p4java.tests.dev.UnitTestDevServerManager;
import com.perforce.p4java.tests.ignoreRule.ConditionallyIgnoreClassRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test symbolic link helper (JDK 7 or above)
 */
public class SymbolicLinkHelperTest extends P4JavaTestCase {

	@ClassRule
	public static ConditionallyIgnoreClassRule ignoreWindows = ConditionallyIgnoreClassRule.ifWindows(
			"Creates paths that can't be used on Windows");

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
		// p4ic4idea: use local server
		UnitTestDevServerManager.INSTANCE.startTestClass("unicode");
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
		// p4ic4idea: use local server
		UnitTestDevServerManager.INSTANCE.endTestClass("unicode");
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
			String path = SymbolicLinkHelper.createSymbolicLink(link, target);
			assertNotNull(path);

			boolean isSymlink = SymbolicLinkHelper.isSymbolicLink(path);
			assertTrue(isSymlink);
			
			File file = new File(path);
			if (file.exists()) {
				assertTrue(file.delete());
			}
		}
	}

}
