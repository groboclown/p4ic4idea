package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Runs all known bug tests for release 2015.1.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages("com.perforce.p4java.tests.dev.unit.bug.r151")
public class Release151BugsSuite {
}
