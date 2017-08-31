package com.perforce.p4java.tests.dev.unit.suite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for tests to be ignored in a run during normal continuous
 * integration cycle fired off by cruise control.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses( {
	All123FeaturesSuite.class,
	Release123BugsSuite.class,
	Release152BugsSuite.class
})
public class DisabledTestsSuite {

}
