package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * @author Sean Shou
 * @since 12/10/2016
 */
abstract class Abstract101TestCase extends P4JavaTestCase {
  public Abstract101TestCase() {
    setServerUrlString("p4java://eng-p4java-vm.perforce.com:20101");
  }
}
