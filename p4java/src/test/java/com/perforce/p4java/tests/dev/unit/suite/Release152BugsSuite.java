package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Runs all known bug tests for release 2015.2.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages("com.perforce.p4java.tests.dev.unit.bug.r152")
public class Release152BugsSuite {
}
