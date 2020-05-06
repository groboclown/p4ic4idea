package com.perforce.p4java.tests;

import com.perforce.p4java.exception.P4JavaException;

@FunctionalInterface
public interface UnitTestGiven {
  void given() throws P4JavaException;
}
