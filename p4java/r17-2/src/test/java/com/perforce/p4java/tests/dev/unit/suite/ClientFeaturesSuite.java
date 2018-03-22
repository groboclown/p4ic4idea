package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Defines client feature tests available to be run for a normal test run.
 * Note: listing order below of suite classes is not significant.
 */
@RunWith(JUnitPlatform.class)
@SelectPackages({"com.perforce.p4java.tests.dev.unit.feature.client"})
public class ClientFeaturesSuite {

}
