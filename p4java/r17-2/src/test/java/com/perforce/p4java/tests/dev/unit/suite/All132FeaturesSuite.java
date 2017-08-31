package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2013.2 features.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages({"com.perforce.p4java.tests.dev.unit.features132"})
public class All132FeaturesSuite {
}
