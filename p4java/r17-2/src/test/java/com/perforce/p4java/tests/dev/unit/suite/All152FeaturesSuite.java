package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2015.2 features.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages({"com.perforce.p4java.tests.dev.unit.features152"})
public class All152FeaturesSuite {
}
