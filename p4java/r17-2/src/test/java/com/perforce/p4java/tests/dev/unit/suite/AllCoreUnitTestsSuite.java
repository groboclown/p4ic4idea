package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all unit tests from core package.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages({"com.perforce.p4java.core"})
public class AllCoreUnitTestsSuite {

}
