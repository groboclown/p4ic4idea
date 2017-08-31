package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2015.1 features.
 */
@RunWith(JUnitPlatform.class)
@SelectPackages({
		"com.perforce.p4java.tests.dev.unit.features151",
		"com.perforce.p4java.tests.dev.unit.bug.r151"
})
public class All151FeaturesSuite {
}
