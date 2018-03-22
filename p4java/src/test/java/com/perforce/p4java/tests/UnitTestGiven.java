package com.perforce.p4java.tests;

import com.perforce.p4java.exception.P4JavaException;

/**
 * @author Sean Shou
 * @since 20/09/2016
 */
@FunctionalInterface
public interface UnitTestGiven {
  void given() throws P4JavaException;
}
