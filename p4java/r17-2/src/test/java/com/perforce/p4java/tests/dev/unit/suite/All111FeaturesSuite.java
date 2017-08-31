package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2011.1 feature development tests and scaffold.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages({
		"com.perforce.p4java.tests.dev.unit.features111",
		"com.perforce.p4java.tests.dev.unit.helper"
})
public class All111FeaturesSuite {
}
