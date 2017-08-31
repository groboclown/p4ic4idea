package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Test suite for all 2014.1 features.
 */
@RunWith(JUnitPlatform.class)
@SelectPackages({"com.perforce.p4java.tests.dev.unit.features141"})
public class All141FeaturesSuite {
}
