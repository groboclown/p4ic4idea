package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all (or at least the vast majority) of
 * 2010.1 options server tests. May include scaffold tests
 * that should be pruned in the longer term.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages("com.perforce.p4java.tests.dev.unit.dev101.options")
public class All101OptionsSuite {

}
