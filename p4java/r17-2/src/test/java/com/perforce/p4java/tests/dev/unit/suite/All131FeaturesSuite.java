package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2013.1 features.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages({
		"com.perforce.p4java.tests.dev.unit.features131",
		"com.perforce.p4java.tests.dev.unit.bug.r131"
})
public class All131FeaturesSuite {
}
