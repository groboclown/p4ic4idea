package com.perforce.p4java.tests.dev.unit.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for tests to be run in a normal continuous
 * integration cycle fired off by cruise control. Should also be
 * run before integrating up to JTeam main.<p>
 */

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    AdminFeaturesSuite.class,
    All101DevSuite.class,
    All101OptionsSuite.class,
    All111FeaturesSuite.class,
    All112FeaturesSuite.class,
    All121FeaturesSuite.class,
    All122FeaturesSuite.class,
    All131FeaturesSuite.class,
    All132FeaturesSuite.class,
    All141FeaturesSuite.class,
    All151FeaturesSuite.class,
    All152FeaturesSuite.class,
    AllEndToEndTestsSuite.class,
    ClientFeaturesSuite.class,
    CoreFeaturesSuite.class,
    Features101Suite.class,
    Release101BugsSuite.class,
    Release111BugsSuite.class,
    Release112BugsSuite.class,
    Release121BugsSuite.class,
    Release131BugsSuite.class,
    Release132BugsSuite.class,
    Release141BugsSuite.class,
    Release151BugsSuite.class,
    Release92BugsSuite.class,
    ServerFeaturesSuite.class,
    StandaloneTestsSuite.class,
    AllCommonUnitTestsSuite.class,
    AllCoreUnitTestsSuite.class,
    AllImplUnitTestsSuite.class,
    AllOptionUnitTestsSuite.class
    
})
public class ContinuousIntegrationSuite {

}
