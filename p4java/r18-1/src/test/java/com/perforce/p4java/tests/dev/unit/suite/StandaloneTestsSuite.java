package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.runner.SelectPackages;
import org.junit.runner.RunWith;



/**
 * Groups and runs standalone tests, i.e. those not requiring a
 * server connection. Note that many of these tests are also
 * referenced in the "normal" test suites and may run several times
 * for each continuous integration test run; this should not be a problem,
 * but if it is, remove the tests from this suite rather than the others.
 */

@RunWith(JUnitPlatform.class)
@SelectPackages({
		"com.perforce.p4java.tests.dev.unit.feature.filespec",
		"com.perforce.p4java.tests.dev.unit.feature.viewmap"
})
public class StandaloneTestsSuite {

}
