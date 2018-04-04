package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Admin features only. May require you to have an admin or super user
 * installed.
 */
@RunWith(JUnitPlatform.class)
@SelectPackages("com.perforce.p4java.tests.dev.unit.feature.admin")
public class AdminFeaturesSuite {

}
