package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2010.1 development tests and scaffold.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages("com.perforce.p4java.tests.dev.unit.dev101.options")
public class All101DevSuite {

}
